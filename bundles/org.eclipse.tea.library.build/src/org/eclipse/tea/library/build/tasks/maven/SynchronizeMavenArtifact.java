/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.library.build.tasks.maven;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.DefaultServiceLocator.ErrorHandler;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
import org.eclipse.core.internal.variables.StringVariableManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.config.BuildDirectories;
import org.eclipse.tea.library.build.config.TeaBuildConfig;
import org.eclipse.tea.library.build.model.MavenExternalJarBuild;
import org.eclipse.tea.library.build.model.PluginBuild;
import org.eclipse.tea.library.build.model.WorkspaceBuild;
import org.eclipse.tea.library.build.util.FileUtils;
import org.eclipse.tea.library.build.util.StringHelper;

@SuppressWarnings("restriction")
public class SynchronizeMavenArtifact {

	private final static RepositoryPolicy DISABLED_POLICY = new RepositoryPolicy(false, null, null);
	private final static RepositoryPolicy RELEASE_POLICY = new RepositoryPolicy(true,
			RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
	private final static RepositoryPolicy SNAPSHOT_POLICY = new RepositoryPolicy(true,
			RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_WARN);
	private final static String MAVEN_DIRNAME = "maven";
	private final static String CLASSIFIER_SOURCES = "sources";
	private String lastExceptionName;
	private MavenConfig properties;

	@Override
	public String toString() {
		return "Synchronize Maven";
	}

	private void info(TaskingLog log, String msg) {
		log.info(toString() + ": " + msg);
	}

	@Execute
	public void run(TaskingLog log, TaskProgressTracker tracker, TeaBuildConfig cfg, WorkspaceBuild wb)
			throws Exception {
		properties = getMavenConfig(log, cfg);
		if (properties == null) {
			return;
		}
		ResourcesPlugin.getWorkspace().run(m -> runOperation(log, tracker, cfg, wb), null);
	}

	private void runOperation(TaskingLog log, TaskProgressTracker tracker, TeaBuildConfig cfg, WorkspaceBuild wb)
			throws CoreException {

		IndexManager indexManager = JavaModelManager.getIndexManager();
		lastExceptionName = null;

		boolean needIndexerDisable = false;
		try {

			ServiceLocator locator = createServiceLocator(log);
			RepositorySystem system = locator.getService(RepositorySystem.class);
			RepositorySystemSession session = createSession(log, system);
			List<RemoteRepository> remotes = createRemoteRepositories();

			Set<BatchableMavenManipulator> mavenManips = new HashSet<>();
			for (PluginBuild pb : wb.getSourcePlugIns()) {
				if (!pb.getMavenExternalJarDependencies().isEmpty() && !pb.getData().isBinary()) {
					mavenManips.add(new BatchableMavenManipulator(log, tracker, system, session, remotes, pb));
				}
			}

			for (BatchableMavenManipulator mavenManip : mavenManips) {
				mavenManip.identifyFilesToUpdate(); // throws
			}

			for (BatchableMavenManipulator mavenManip : mavenManips) {
				if (!mavenManip.getFilesToClean().isEmpty()) {
					needIndexerDisable = true;
					break;
				}
			}

			// process remaining old workspace change events
			ResourcesPlugin.getWorkspace().checkpoint(false);

			if (needIndexerDisable) {
				// We have to close jar files potentially in use by Eclipse,
				// to allow them for being replaced even on Windows, see
				// https://bugs.eclipse.org/406170
				tracker.setTaskName("closing workspace files that are updated");
				info(log, "closing workspace files that are updated");

				// First, prevent the Indexer from reopening them.
				indexManager.disable();

				// Second, tell indexer to release the file handles.
				mavenManips.forEach(
						m -> m.getFilesToClean().forEach(f -> indexManager.discardJobs(f.getFullPath().toString())));

				// The Indexer leaves closing ZipFile handles to finalization,
				// see https://bugs.eclipse.org/567661
				// Although discardJobs() does wait for the Indexer jobs to
				// terminate, the resources may take a little longer to get
				// ready for finalization.
				// But instead of sleeping, we do something else.

				// Close jar files providing Annotations, see
				// https://bugs.eclipse.org/565436
				AptConfig.setFactoryPath(null, AptConfig.getFactoryPath(null));

				// Third, close the file handles.
				System.gc();
				System.runFinalization();
			}

			// change file system content, no workspace change events yet
			mavenManips.forEach(BatchableMavenManipulator::synchronizeArtifacts);

			tracker.setTaskName("finalizing");
			info(log, "finalizing");

			// in case the indexer listens to workspace change events
			if (needIndexerDisable) {
				needIndexerDisable = false;
				indexManager.enable();
			}

			// create new workspace change events, may already process them
			for (BatchableMavenManipulator mavenManip : mavenManips) {
				mavenManip.refreshWorkspace(); // throws
			}

		} catch (OperationCanceledException e) {
			info(log, "cancelled");
			throw e;
		} catch (Exception e) {
			log.error("error synchronizing maven artifacts", e);
		} finally {
			if (needIndexerDisable) {
				// never leave disabled, even not on exception
				indexManager.enable();
			}
			// process remaining new workspace change events
			ResourcesPlugin.getWorkspace().checkpoint(false);
		}
	}

	private static void checkCanceled(TaskProgressTracker tracker) {
		if (tracker.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	public static MavenConfig getMavenConfig(TaskingLog log, TeaBuildConfig cfg) throws Exception {
		if (cfg.mavenConfigFilePath == null || StringHelper.isNullOrEmpty(cfg.mavenConfigFilePath)) {
			log.info("Skipping synchronization because no maven configuration has been set");
			return null;
		}
		String expanded = StringVariableManager.getDefault().performStringSubstitution(cfg.mavenConfigFilePath);
		File file = new File(expanded);
		if (!file.isAbsolute()) {
			file = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), expanded);
		}
		if (!file.exists()) {
			log.warn("Maven configuration file " + file + " is missing");
			return null;
		}
		return new MavenConfig(file);
	}

	private class BatchableMavenManipulator {
		private final TaskingLog log;
		private final TaskProgressTracker tracker;
		private final RepositorySystem system;
		private final RepositorySystemSession session;
		private final List<RemoteRepository> remotes;
		private final PluginBuild hostPlugin;
		private final IFolder targetFolder;

		private final Set<IFile> filesAlreadyUpToDate = new HashSet<>();
		private final Set<IFile> filesToClean = new HashSet<>();
		private final List<ArtifactRequest> artifactRequests = new ArrayList<>();
		private final Map<IFile, Artifact> artifactOfFilesToCreate = new HashMap<>();
		private final Set<IResource> artifactsToRefresh = new HashSet<>();

		private BatchableMavenManipulator(TaskingLog log, TaskProgressTracker tracker, RepositorySystem system,
				RepositorySystemSession session, List<RemoteRepository> remotes, PluginBuild hostPlugin)
				throws CoreException {
			this.log = log;
			this.tracker = tracker;
			this.system = system;
			this.session = session;
			this.remotes = remotes;
			this.hostPlugin = hostPlugin;
			IProject prj = hostPlugin.getData().getProject();
			this.targetFolder = prj.getFolder(MAVEN_DIRNAME);

			NullProgressMonitor monitor = new NullProgressMonitor();
			if (!targetFolder.exists()) {
				targetFolder.create(false, true, monitor);
				log.warn("creating " + targetFolder.getName() + "; make sure to add to the classpath of "
						+ hostPlugin.getPluginName());
			}
			// write .gitignore
			IFile gitignore = targetFolder.getFile(".gitignore");
			if (!gitignore.exists()) {
				gitignore.create(new ByteArrayInputStream("*.jar".getBytes(Charset.defaultCharset())), false, null);
			}
			this.filesAlreadyUpToDate.add(gitignore);
		}

		void identifyFilesToUpdate() throws CoreException {
			checkCanceled(tracker);
			int count = hostPlugin.getMavenExternalJarDependencies().size();
			tracker.setTaskName("checking artifacts (" + count + ") for " + hostPlugin.getPluginName());
			info(log,
					"check " + count + " artifacts for '" + hostPlugin.getPluginName() + "': "
							+ hostPlugin.getMavenExternalJarDependencies().stream()
									.map(artifact -> artifact.getCoordinates()).collect(Collectors.joining(", ")));
			for (MavenExternalJarBuild artifact : hostPlugin.getMavenExternalJarDependencies()) {
				Coordinate coord = new Coordinate(artifact.getCoordinates());

				// resolve binary bundle.
				checkCoordinateForUpdate(coord, coord.classifier);

				if (!CLASSIFIER_SOURCES.equals(coord.classifier)) {
					// resolve source bundle.
					checkCoordinateForUpdate(coord, CLASSIFIER_SOURCES);
				}
			}

			// Identify existing files that we don't know or have an update for.
			for (IResource file : targetFolder.members()) {
				if (file instanceof IFile && !filesAlreadyUpToDate.contains(file)) {
					filesToClean.add((IFile) file);
				}
			}
		}

		private void checkCoordinateForUpdate(Coordinate coord, String classifier) {
			Artifact mvn = new DefaultArtifact(coord.group, coord.artifact, classifier, coord.extension, coord.version);
			boolean needDownload = false;
			try {
				// try to look it up in the local repository only!
				ArtifactRequest localrq = new ArtifactRequest().setArtifact(mvn);
				ArtifactResult localResult = system.resolveArtifact(session, localrq);
				Artifact localArtifact = localResult.getArtifact();
				File localFile = localArtifact.getFile();
				if (localResult.isMissing() || !localResult.isResolved() || localArtifact.isSnapshot()
						|| localFile == null) {
					needDownload = true;
				} else {
					// Any existing workspace file might be up to date if
					// and only if we have locally resolved the artifact.
					// We always replace the workspace file when downloading.
					IFile targetFile = targetFolder.getFile(localFile.getName());
					if (needUpdateFileInProjectsMavenFolder(targetFile, localArtifact)) {
						artifactOfFilesToCreate.put(targetFile, localArtifact);
					} else {
						filesAlreadyUpToDate.add(targetFile);
					}
				}
			} catch (Exception e) {
				needDownload = true;
			}
			if (needDownload) {
				ArtifactRequest remoterq = new ArtifactRequest().setArtifact(mvn).setRepositories(remotes);
				artifactRequests.add(remoterq);
			}
		}

		Collection<IFile> getFilesToClean() {
			return filesToClean;
		}

		void synchronizeArtifacts() {
			checkCanceled(tracker);
			int local = artifactOfFilesToCreate.size();
			int remote = artifactRequests.size();
			int old = filesToClean.size();
			int total = local + remote;
			if (total > 0 || old > 0) {
				String totalFileCnt = total == 1 ? "1 file" : total + " files";
				tracker.setTaskName(
						"update " + totalFileCnt + " (download " + remote + ") in " + hostPlugin.getPluginName());
				info(log,
						"update " + total + " files (download " + remote + ") in '" + hostPlugin.getPluginName() + "'");
			}

			// cleanup unknown or outdated files
			for (IResource file : filesToClean) {
				artifactsToRefresh.add(file);
				info(log, "remove old or suspect file: " + file);
				// Delete raw File, let refreshLocal() fire workspace events.
				// Ignore delete errors here, we retry for files we update.
				file.getRawLocation().toFile().delete();
			}

			List<ArtifactResult> results = resolveArtifacts(log, system, session, artifactRequests);
			for (ArtifactResult result : results) {
				ArtifactRequest rq = result.getRequest();
				Artifact mvn = rq.getArtifact();
				String c = mvn.getClassifier();
				if (result.isMissing() || !result.isResolved() || !result.getExceptions().isEmpty()) {
					if ("sources".equals(c)) {
						log.warn("No sources available for " + mvn.getGroupId() + ":" + mvn.getArtifactId() + ":"
								+ mvn.getVersion());
					} else {
						String classifier = c == null || c.isEmpty() ? "" : (":" + c);
						log.error("cannot resolve " + mvn.getGroupId() + ":" + mvn.getArtifactId() + classifier + ":"
								+ mvn.getVersion());
					}
				} else {
					// copy file to maven directory
					Artifact artifact = result.getArtifact();
					File resolvedFile = artifact.getFile();
					IFile targetFile = targetFolder.getFile(resolvedFile.getName());
					artifactOfFilesToCreate.put(targetFile, artifact);
				}
			}
			for (Entry<IFile, Artifact> artifactOfFile : artifactOfFilesToCreate.entrySet()) {
				IFile targetResource = artifactOfFile.getKey();
				artifactsToRefresh.add(targetResource);
				// Update raw File, let refreshLocal() fire workspace events.
				File targetFile = targetResource.getRawLocation().toFile();
				File resolvedFile = artifactOfFile.getValue().getFile();
				updateFileInProjectsMavenFolder(log, targetFile, resolvedFile);
			}
		}

		void refreshWorkspace() throws CoreException {
			for (IResource r : artifactsToRefresh) {
				r.refreshLocal(IResource.DEPTH_ZERO, null); // throws
			}
		}
	}

	/**
	 * Resolves multiple {@link ArtifactRequest}. Resolving means that it looks
	 * up the bundle on the local repository and all servers. After successful
	 * resolution, the bundles is located in the local repository. After that,
	 * this method copies the according file to the target location.
	 *
	 * @param controller
	 *            used for logging
	 * @param system
	 *            the {@link RepositorySystem} providing the resolution
	 *            algorithm
	 * @param session
	 *            the {@link RepositorySystemSession} to use
	 * @param requests
	 *            A Collection of {@link ArtifactRequest} that defines what to
	 *            resolve
	 * @returns list of {@link ArtifactResult}
	 */
	private static List<ArtifactResult> resolveArtifacts(TaskingLog log, RepositorySystem system,
			RepositorySystemSession session, Collection<ArtifactRequest> requests) {
		List<ArtifactResult> results;
		try {
			results = system.resolveArtifacts(session, requests);
		} catch (ArtifactResolutionException e) {
			results = e.getResults();
		}
		return results;
	}

	private static boolean needUpdateFileInProjectsMavenFolder(IFile targetResource, Artifact artifact) {
		File targetFile = targetResource.getRawLocation().toFile();
		File resolvedFile = artifact.getFile();
		// no need to update if files are equal:
		return !FileUtils.equals(targetFile, resolvedFile);
	}

	private void updateFileInProjectsMavenFolder(TaskingLog log, File targetFile, File resolvedFile) {
		// update = delete existing + copy or link new file:
		if (targetFile.exists()) {
			// we have ignored failure of previous delete attempt
			if (!targetFile.delete()) {
				log.error("cannot update " + targetFile + ". Please make sure file is not locked");
				return;
			}
		}

		try {
			java.nio.file.Files.createSymbolicLink(targetFile.toPath(), resolvedFile.toPath());
		} catch (IOException e) {
			String exName = e.getClass().getName();
			// don't spam missing rights for symlink creation
			if (!Objects.equals(exName, lastExceptionName)) {
				lastExceptionName = exName;
				// Windows 10:
				// "$file: Dem Client fehlt ein erforderliches Recht.\r\n"
				String msg = e.getMessage();
				if (msg != null) {
					msg = msg.replace("\n", "\\n");
					msg = msg.replace("\r", "\\r");
				}
				log.warn("cannot create symlink for: " + targetFile + " (" + exName + " " + msg + ")");
			}
			try {
				FileUtils.copyFile(resolvedFile, targetFile);
			} catch (IOException copyException) {
				log.error("Could not copy file.", copyException);
				return;
			}
		}
		return;
	}

	private List<RemoteRepository> createRemoteRepositories() {
		List<RemoteRepository> repos = new ArrayList<>();
		for (Map.Entry<String, String> repo : properties.getMavenRepos().entrySet()) {
			RemoteRepository.Builder builder = new RemoteRepository.Builder("nexus_" + repo.getKey(), "default",
					repo.getValue());
			if (repo.getKey().startsWith("snapshot")) {
				builder.setReleasePolicy(DISABLED_POLICY);
				builder.setSnapshotPolicy(SNAPSHOT_POLICY);
			} else if (repo.getKey().startsWith("release")) {
				builder.setSnapshotPolicy(DISABLED_POLICY);
				builder.setReleasePolicy(RELEASE_POLICY);
			}
			repos.add(builder.build());
		}
		return repos;
	}

	private DefaultRepositorySystemSession createSession(TaskingLog log, RepositorySystem system) {
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		LocalRepository repo = new LocalRepository(BuildDirectories.get().getMavenDirectory());
		session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, repo));

		if (properties.isVerboseMavenOutput()) {
			session.setTransferListener(new ConsoleTransferListener(log.debug()));
			session.setRepositoryListener(new ConsoleRepositoryListener(log.debug()));
		}

		return session;
	}

	private static ServiceLocator createServiceLocator(TaskingLog log) {
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, WagonTransporterFactory.class);
		locator.addService(WagonProvider.class, InternalWagonProvider.class);
		locator.setErrorHandler(new ErrorHandler() {

			@Override
			public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
				log.error("cannot create service " + type.getName());
				exception.printStackTrace(log.error());
			}
		});
		return locator;
	}

	private static class InternalWagonProvider implements WagonProvider {

		@Override
		public Wagon lookup(String roleHint) throws Exception {
			switch (roleHint) {
			case "file":
				return new FileWagon();
			case "http":
				return newOfFirstWagonAvailable(
						// since Eclipse 2023-12
						"org.apache.maven.wagon.providers.http.HttpWagon",
						// up to Eclipse 2023-09
						"io.takari.aether.wagon.OkHttpWagon");
			case "https":
				return newOfFirstWagonAvailable(
						// since Eclipse 2023-12
						"org.apache.maven.wagon.providers.http.HttpWagon",
						// up to Eclipse 2023-09
						"io.takari.aether.wagon.OkHttpsWagon");
			default:
				return null;
			}
		}

		@SuppressWarnings("unchecked")
		private static Wagon newOfFirstWagonAvailable(String... classnames)
				throws ClassNotFoundException, InstantiationException, IllegalAccessException {
			String classname = null;
			Constructor<? extends Wagon> constructor = null;
			for (String name : classnames) {
				try {
					Class<?> candidate = Class.forName(name);
					if (Wagon.class.isAssignableFrom(candidate)) {
						classname = name;
						constructor = ((Class<? extends Wagon>) candidate).getConstructor();
						break;
					}
				} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
				}
			}
			if (constructor != null) {
				try {
					return constructor.newInstance();
				} catch (InvocationTargetException e) {
					throw new RuntimeException("constructing " + classname, e.getTargetException());
				}
			}
			ClassNotFoundException e = new ClassNotFoundException("one of " + String.join(", ", classnames));
			e.printStackTrace();
			throw e;
		}

		@Override
		public void release(Wagon wagon) {
			if (wagon instanceof StreamWagon) {
				try {
					((StreamWagon) wagon).closeConnection();
				} catch (ConnectionException e) {
					throw new RuntimeException("Cannot close connection", e);
				}
			}
		}

	}

	/**
	 * Partially taken from {@link DefaultArtifact}'s constructor - keep in
	 * sync!
	 */
	private static class Coordinate {

		final String group;
		final String artifact;
		final String extension;
		final String classifier;
		final String version;

		public Coordinate(String coord) {
			Matcher matcher = PluginBuild.MAVEN_COORDINATE_PATTERN.matcher(coord);
			if (!matcher.matches()) {
				throw new IllegalStateException("Illegal maven coordinates: " + coord);
			}

			group = matcher.group(1);
			artifact = matcher.group(2);
			extension = get(matcher.group(4), "jar");
			classifier = get(matcher.group(6), "");
			version = matcher.group(7);
		}

		private static String get(String value, String defaultValue) {
			return (value == null || value.length() <= 0) ? defaultValue : value;
		}
	}

	public static class MavenConfig {

		/**
		 * template for maven repo url: multiple properties that start with this
		 * string are allowed
		 */
		private static final String MAVEN_REPO_URL = "maven_repo_url_";

		/**
		 * template for maven repo type: multiple properties that start with
		 * this string are allowed
		 */
		private static final String MAVEN_REPO_TYPE = "maven_repo_type_";

		protected final Properties props;

		/**
		 * Reads the properties from the specified file.
		 *
		 * @param file
		 *            property file
		 */
		public MavenConfig(File file) throws IOException {
			FileInputStream fis = new FileInputStream(file);
			try {
				props = new Properties();
				props.load(fis);
			} finally {
				fis.close();
			}
		}

		/**
		 * @return the raw properties in the file.
		 */
		public Properties getProps() {
			return props;
		}

		/**
		 * @return the types and URLs of the main Maven repositories, used to
		 *         sync all artifacts.
		 */
		public Map<String, String> getMavenRepos() {
			long repoNum = 0;
			Map<String, String> result = new TreeMap<>();
			for (String key : props.stringPropertyNames()) {
				if (key != null && key.startsWith(MAVEN_REPO_URL)) {
					// each repo must have a type. the property that defines the
					// type must have the same
					// "ending" as the one defining the URL. Thus extract the
					// ending from the property
					// key and fetch the type with the according calculated key.
					String type = props.getProperty(MAVEN_REPO_TYPE + key.substring(MAVEN_REPO_URL.length()));
					result.put(type + (repoNum++), props.getProperty(key));
				}
			}
			return result;
		}

		/**
		 * Determines whether verbose maven output should be written to the
		 * console
		 */
		public boolean isVerboseMavenOutput() {
			String x = props.getProperty("maven_verbose");
			return Boolean.parseBoolean(x);
		}
	}
}