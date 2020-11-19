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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

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
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
import org.eclipse.core.internal.variables.StringVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.config.BuildDirectories;
import org.eclipse.tea.library.build.config.TeaBuildConfig;
import org.eclipse.tea.library.build.model.MavenExternalJarBuild;
import org.eclipse.tea.library.build.model.PluginBuild;
import org.eclipse.tea.library.build.model.WorkspaceBuild;
import org.eclipse.tea.library.build.util.FileUtils;
import org.eclipse.tea.library.build.util.StringHelper;

import com.google.common.base.Charsets;

import io.takari.aether.wagon.OkHttpWagon;
import io.takari.aether.wagon.OkHttpsWagon;

@SuppressWarnings("restriction")
public class SynchronizeMavenArtifact {

	private final static RepositoryPolicy DISABLED_POLICY = new RepositoryPolicy(false, null, null);
	private final static RepositoryPolicy RELEASE_POLICY = new RepositoryPolicy(true,
			RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
	private final static RepositoryPolicy SNAPSHOT_POLICY = new RepositoryPolicy(true,
			RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_WARN);
	private MavenConfig properties;

	@Override
	public String toString() {
		return "Synchronize Maven";
	}

	@Execute
	public void run(TaskingLog log, TeaBuildConfig cfg, WorkspaceBuild wb) throws Exception {
		properties = getMavenConfig(log, cfg);
		if (properties == null) {
			return;
		}

		// Close jar files potentially in use by the Indexer:
		// The Indexer actually leaves closing real files to finalization,
		// see https://bugs.eclipse.org/567661
		// and https://bugs.eclipse.org/406170
		// Although discardJobs() does wait for the Indexer jobs to
		// terminate, the resources may take a little longer to get ready
		// for finalization. But instead of sleeping, we do something else.
		IndexManager indexManager = JavaModelManager.getIndexManager();
		indexManager.disable();

		try {
			indexManager.discardJobs(null);

			// close jar files providing Annotations, see
			// https://bugs.eclipse.org/565436
			AptConfig.setFactoryPath(null, AptConfig.getFactoryPath(null));

			ServiceLocator locator = createServiceLocator(log);
			RepositorySystem system = locator.getService(RepositorySystem.class);
			RepositorySystemSession session = createSession(log, system);
			List<RemoteRepository> remotes = createRemoteRepositories();

			Collection<PluginBuild> pbs = wb.getSourcePlugIns();

			// Try to close the Indexer's file handles now.
			System.gc();
			System.runFinalization();

			for (PluginBuild pb : pbs) {
				if (!pb.getMavenExternalJarDependencies().isEmpty() && !pb.getData().isBinary()) {
					runSingle(log, pb, system, session, remotes);
				}
			}
		} finally {
			indexManager.enable();
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

	private void runSingle(TaskingLog log, PluginBuild hostPlugin, RepositorySystem system,
			RepositorySystemSession session, List<RemoteRepository> remotes) throws Exception {
		File target = new File(hostPlugin.getPluginDirectory(), "maven");

		if (!target.exists()) {
			FileUtils.mkdirs(target);
			log.warn("creating " + target + "; make sure to add to the classpath of " + hostPlugin.getPluginName());
		}

		Set<File> valid = new TreeSet<>(Comparator.comparing(File::getName));
		for (MavenExternalJarBuild artifact : hostPlugin.getMavenExternalJarDependencies()) {
			log.info("synchronize nexus coordinate " + artifact.getCoordinates() + " into "
					+ hostPlugin.getPluginName());

			Coordinate coord = new Coordinate(artifact.getCoordinates());

			// try to look it up in the local repository only!
			Artifact mvn = new DefaultArtifact(coord.group, coord.artifact, coord.classifier, coord.extension,
					coord.version);
			ArtifactRequest localrq = new ArtifactRequest().setArtifact(mvn);
			boolean remote = false;
			try {
				ArtifactResult local = system.resolveArtifact(session, localrq);
				if (local.isMissing() || !local.isResolved() || local.getArtifact().isSnapshot()) {
					remote = true;
				}
			} catch (Exception e) {
				remote = true;
			}

			// resolve binary bundle.
			{
				ArtifactRequest remoterq = new ArtifactRequest().setArtifact(mvn)
						.setRepositories(remote ? remotes : null);
				resolveArtifact(log, target, system, session, remoterq, valid);
			}

			// resolve source bundle.
			try {
				Artifact srcmvn = new DefaultArtifact(coord.group, coord.artifact, "sources", coord.extension,
						coord.version);
				ArtifactRequest srcrq = new ArtifactRequest().setArtifact(srcmvn)
						.setRepositories(remote ? remotes : null);

				resolveArtifact(log, target, system, session, srcrq, valid);
			} catch (Exception e) {
				log.warn("No sources available for " + artifact.getCoordinates());
			}
		}

		// cleanup old files
		for (File file : target.listFiles()) {
			if (file.getName().equals(".gitignore")) {
				continue;
			}

			boolean isValid = false;
			for (File validFile : valid) {
				if (file.equals(validFile)) {
					isValid = true;
				}
			}

			if (!isValid) {
				log.info("removing old maven artifact: " + file);
				FileUtils.delete(file);
			}
		}

		// write .gitignore
		File gitignore = new File(target, ".gitignore");
		if (!gitignore.exists()) {
			FileUtils.writeFileFromString(gitignore, Charsets.UTF_8, "*.jar");
		}

		// refresh project
		IProject prj = hostPlugin.getData().getProject();
		prj.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	}

	/**
	 * Resolves an {@link ArtifactRequest}. Resolving means that it looks up the
	 * bundle on the local repository and all servers. After successful
	 * resolution, the bundles is located in the local repository. After that,
	 * this method copies the according file to the target location.
	 *
	 * @param controller
	 *            used for logging
	 * @param target
	 *            the target directory to put the bundle into
	 * @param system
	 *            the {@link RepositorySystem} providing the resolution
	 *            algorithm
	 * @param session
	 *            the {@link RepositorySystemSession} to use
	 * @param rq
	 *            the {@link ArtifactRequest} that defines what to resolve
	 * @param resolved
	 *            all resolved artifacts will be added to this list, even if no
	 *            file has been changed on disk.
	 */
	private void resolveArtifact(TaskingLog log, File target, RepositorySystem system, RepositorySystemSession session,
			ArtifactRequest rq, Set<File> resolved) {
		Artifact mvn = rq.getArtifact();

		try {
			ArtifactResult result = system.resolveArtifact(session, rq);
			if (result.isMissing() || !result.isResolved()) {
				log.warn("cannot resolve " + mvn.getGroupId() + ":" + mvn.getArtifactId() + ":" + mvn.getVersion() + ":"
						+ mvn.getClassifier());
				if (!result.getExceptions().isEmpty()) {
					for (Exception e : result.getExceptions()) {
						e.printStackTrace(log.error());
					}
				}
				return;
			}
			if (properties.isVerboseMavenOutput()) {
				if (!result.getExceptions().isEmpty()) {
					for (Exception e : result.getExceptions()) {
						e.printStackTrace(log.debug());
					}
				}
			}
			// download file to maven directory
			File file = result.getArtifact().getFile();
			addFileToProjectsMavenFolder(log, target, resolved, result, file);
		} catch (Exception e) {
			log.error("cannot resolve " + mvn.getGroupId() + ":" + mvn.getArtifactId() + ":" + mvn.getVersion());
			throw new RuntimeException("failed to synchronize " + rq.getArtifact().getArtifactId(), e);
		}
	}

	private static void addFileToProjectsMavenFolder(TaskingLog log, File target, Set<File> resolved,
			ArtifactResult result, File file) throws IOException {
		File targetFile = new File(target, file.getName());
		resolved.add(targetFile);

		if (targetFile.exists() && !result.getArtifact().isSnapshot()) {
			// it is a released file. try to update but don't fail if (for
			// example) windows locks
			// the file...
			if (!targetFile.delete()) {
				if (targetFile.length() != file.length()) {
					// would need update but can't
					log.error("cannot update " + targetFile + " to new version, please make sure file is not locked");
				}

				return; // don't update
			}
		}

		// force refresh of file/link in any case.
		FileUtils.delete(targetFile);

		try {
			targetFile = java.nio.file.Files.createSymbolicLink(targetFile.toPath(), file.toPath()).toFile();
			log.info("symlink created: " + targetFile);
		} catch (IOException e) {
			FileUtils.copyFileToDirectory(file, target);
		}
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
				return new OkHttpWagon();
			case "https":
				return new OkHttpsWagon();
			default:
				return null;
			}
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