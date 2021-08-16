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
package org.eclipse.tea.library.build.model;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.jar.ZipExec;
import org.eclipse.tea.library.build.jar.ZipExecFactory;
import org.eclipse.tea.library.build.jar.ZipExecPart;
import org.eclipse.tea.library.build.util.FileUtils;

/**
 * Provides information about building a RCP plugin.
 */
public class PluginBuild extends BundleBuild<PluginData> implements Comparable<PluginBuild> {

	protected Set<PluginBuild> sourceDependencies;
	protected Set<MavenExternalJarBuild> mavenDependencies;
	protected Set<String> workspaceDependencies;
	protected final Set<PluginBuild> fragments = new TreeSet<>();

	public static final Pattern MAVEN_COORDINATE_PATTERN = Pattern
			.compile("([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?:([^: ]+)");

	public PluginBuild(PluginData data) {
		super(data);
	}

	/**
	 * Returns the name of this plugin.
	 */
	public final String getPluginName() {
		return data.getBundleName();
	}

	/**
	 * Returns the bundle directory; {@code null} for a JAR distribution.
	 */
	public final File getPluginDirectory() {
		return data.bundleDir;
	}

	/**
	 * Returns all existing and valid dependencies.
	 */
	public final Collection<PluginBuild> getSourceDependencies() {
		return sourceDependencies;
	}

	public final Collection<MavenExternalJarBuild> getMavenExternalJarDependencies() {
		return mavenDependencies;
	}

	/**
	 * Returns the names of all dependencies which could be found in the
	 * workspace.
	 */
	public final Collection<String> getWorkspaceDependencies() {
		return workspaceDependencies;
	}

	public final Collection<PluginBuild> getFragments() {
		return fragments;
	}

	/**
	 * Compares two plugins by its name.
	 */
	@Override
	public int compareTo(PluginBuild o) {
		return getPluginName().compareTo(o.getPluginName());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof PluginBuild)) {
			return false;
		}
		PluginBuild other = (PluginBuild) obj;
		return getPluginName().equals(other.getPluginName());
	}

	@Override
	public int hashCode() {
		return getPluginName().hashCode();
	}

	@Override
	public String toString() {
		return getPluginName();
	}

	protected void updateDependencies(WorkspaceBuild ws) {
		sourceDependencies = new TreeSet<>();
		workspaceDependencies = new TreeSet<>();
		mavenDependencies = new TreeSet<>();

		for (ParameterValue pv : data.getDependencies()) {
			addSourceDependency(ws, pv.getValue());
		}

		for (ParameterValue pv : data.getMavenDependencies()) {
			addMavenDependency(ws, pv.getValue());
		}

		// check fragments
		ParameterValue fragmentHost = data.getFragmentHost();
		if (fragmentHost != null) {
			PluginBuild host = addSourceDependency(ws, fragmentHost.getValue());
			if (host != null) {
				host.fragments.add(this);
			} else {
				ws.addHostLessFragment(this);
			}
		}
	}

	private void addMavenDependency(WorkspaceBuild ws, String value) {
		// dependency format: "groupId:name:version"
		Matcher matcher = MAVEN_COORDINATE_PATTERN.matcher(value);
		if (matcher.matches()) {
			mavenDependencies.add(new MavenExternalJarBuild(matcher.group(0)));
		}
	}

	private PluginBuild addSourceDependency(WorkspaceBuild ws, String name) {
		PluginBuild pb = ws.sourcePlugins.get(name);
		if (pb != null) {
			sourceDependencies.add(pb);
			workspaceDependencies.add(name);
		} else if (ws.isClosedOrIncomplete(name)) {
			workspaceDependencies.add(name);
		}
		return pb;
	}

	@Override
	public String getJarFileName(String buildVersion) {
		return getPluginName() + '_' + buildVersion + ".jar";
	}

	/**
	 * Find the binary for this plugin. Only valid if {@link #getData()}.
	 * {@link PluginData#isBinary() isBinary()}.
	 *
	 * @return the path to the underlying backing jar file for this binary
	 *         plugin. Never null.
	 * @throws Exception
	 *             in case the file cannot be found.
	 */
	public File getBinaryJarFile() throws Exception {
		if (!data.isBinary()) {
			throw new IllegalStateException(this + " is not binary!");
		}

		for (IResource r : data.getProject().members()) {
			String name = r.getName();
			if (name.startsWith(data.getBundleName()) && name.endsWith(".jar")) {
				return r.getLocation().toFile();
			}
		}
		throw new IllegalStateException(this + " has no binary file even though it is binary?!");
	}

	/**
	 * Creates the JAR file for this plugin.
	 *
	 * @return the path to the generated jar file.
	 */
	@Override
	public File execJarCommands(ZipExecFactory zip, File distDirectory, String buildVersion, JarManager jarManager)
			throws Exception {
		return execJarCommands(zip, distDirectory, buildVersion, true);
	}

	@Override
	public File execJarCommands(ZipExecFactory zip, File distDirectory, String buildVersion, JarManager jarManager,
			boolean withSources) throws Exception {
		return execJarCommands(zip, distDirectory, buildVersion, true, withSources);
	}

	/**
	 * Creates the JAR file for this plugin. If withBinInc is
	 * <code>false</code>, no binary includes will be added to the jar.
	 *
	 * @return the path to the generated jar file.
	 */
	public File execJarCommands(ZipExecFactory zip, File distDirectory, String buildVersion, boolean withBinInc)
			throws Exception {
		boolean withSource = false;
		return execJarCommands(zip, distDirectory, buildVersion, withBinInc, withSource);
	}

	public File execJarCommands(ZipExecFactory zip, File distDirectory, String buildVersion, boolean withBinInc,
			boolean withSource) throws Exception {

		final String oldBundleVersion = data.getBundleVersion();
		final File manifest = data.getManifestFile();
		if (oldBundleVersion == null || manifest == null) {
			// simply create the JAR
			return doExecJarCommands(zip, distDirectory, buildVersion, withBinInc, withSource);
		}

		// backup the manifest file
		File rootDir = manifest.getParentFile().getParentFile();
		final File backup = new File(rootDir, "__manifest__.bak");
		FileUtils.copyFile(manifest, backup);
		final long timeStamp = manifest.lastModified();

		String[] classPath = data.getClassPath();

		try {
			// create the new manifest
			data.setBundleVersion(buildVersion);

			// TODO: what to do about this? don't want to have it always?
			String[] enhancedCP = new String[classPath.length + (classPath.length == 0 ? 2 : 1)];
			enhancedCP[0] = "external:$com.wamas.fastpatch.root$/" + data.getBundleName();
			if (classPath.length == 0) {
				enhancedCP[1] = ".";
			}
			System.arraycopy(classPath, 0, enhancedCP, 1, classPath.length);
			data.setClassPath(enhancedCP);

			data.writeManifest();
			if (!manifest.setLastModified(timeStamp)) {
				zip.log.debug("cannot set last modified time: " + manifest);
			}

			// create the JAR
			return doExecJarCommands(zip, distDirectory, buildVersion, withBinInc, withSource);
		} finally {
			// restore the old manifest
			FileUtils.copyFile(backup, manifest);
			FileUtils.delete(backup);
			data.setBundleVersion(oldBundleVersion);
			data.setClassPath(classPath);
			data.refreshProject();
		}
	}

	public boolean isPreserveBinaryStructure() {
		return Boolean.parseBoolean(data.getSimpleManifestValue("Preserve-Binary-Structure"));
	}

	private File doExecJarCommands(ZipExecFactory zip, File distDirectory, String buildVersion, boolean withBinInc,
			boolean withSource) throws Exception {
		final File jarFile = new File(distDirectory, getJarFileName(buildVersion));

		// remove the jar file
		FileUtils.delete(jarFile);

		// update the manifest for binary deployment
		data.updateManifestForBinaryDeployment();

		// create the ZIP executor
		final ZipExec exec = zip.createZipExec();
		exec.setZipFile(jarFile);
		exec.setJarMode(true);

		// run ZIP on 'bin' directories
		Map<String, List<String>> binaryFolders = data.getBinaryFolders();
		String[] binInc = data.getBinaryIncludes();

		for (String inc : binInc) {
			List<String> paths = binaryFolders.get(inc);
			if (paths != null && !paths.isEmpty()) {
				for (String path : paths) {
					File binDir = new File(data.getBundleDir(), path);
					if (binDir.isDirectory() && binDir.list().length > 0) {
						ZipExecPart part = new ZipExecPart();
						if (isPreserveBinaryStructure() && !withSource) {
							// FIXME acx needs a plugin suitable form
							// "Cannot nest
							// 'com.wamas.acx4.mfs_5.13.0.N/build-eclipse/main'
							// inside library"
							part.sourceDirectory = data.getBundleDir();
							part.relativePaths.add(path);
						} else {
							part.sourceDirectory = binDir;
							part.relativePaths.add(".");
						}
						part.excludeGit = true;

						exec.addPart(part);
					}
				}
			}
		}

		// run ZIP for binary includes
		if (withBinInc) {
			ZipExecPart incPart = new ZipExecPart();
			for (String bin : binInc) {
				// output folders are added above
				if (!binaryFolders.containsKey(bin)) {
					incPart.relativePaths.add(bin);
				}
			}
			if (!incPart.relativePaths.isEmpty()) {
				incPart.sourceDirectory = data.getBundleDir();
				incPart.excludeGit = true;
				exec.addPart(incPart);
			}
		}

		// run ZIP on source directories
		String[] srcFolders = data.getSourceFolders();
		if (withSource) {
			for (String path : srcFolders) {
				// typically "src" or "src-gen" or "src/main/java"
				ZipExecPart part = new ZipExecPart();
				File binDir = new File(data.getBundleDir(), path);
				if (binDir.isDirectory() && binDir.list().length > 0) {
					// keep the "src" folder
					part.sourceDirectory = data.getBundleDir();
					part.relativePaths.add(path);
					part.excludeGit = true;
					exec.addPart(part);
				}
			}
		}

		// create the jar file
		try {
			exec.createZip();
		} catch (Exception ex) {
			throw new RuntimeException("Unable to zip plug-in '" + data.getBundleName() + "'", ex);
		}

		return jarFile;
	}

	/**
	 * Calculates the state of the 'unpack' flag (used by features).
	 */
	public boolean needUnpack() {
		// check our special flag
		if (data.manifest.getNeedUnpack()) {
			return true;
		}

		// check the class path
		String[] classPath = data.getClassPath();
		if (classPath == null) {
			return false;
		}
		for (String element : classPath) {
			if (!element.equals(".")) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Creates a self-contained ZIP file for this plugins. The result can be
	 * treated as a 'product' for stand-alone Java applications.
	 *
	 * @param zipFactory
	 *            ZIP factory
	 * @param buildVersion
	 *            build version of the product
	 * @param output
	 *            ZIP file to build
	 */
	public void buildSelfContainedZip(ZipExecFactory zipFactory, String buildVersion, File output) throws Exception {
		File outputDirectory = output.getParentFile();
		File temp = new File(outputDirectory, "zip_" + String.valueOf(System.currentTimeMillis()));
		File libs = new File(temp, getPluginName());

		FileUtils.mkdirs(libs);

		try {
			copyLibsForSelfContainment(zipFactory, libs, buildVersion);

			FileUtils.delete(output);

			ZipExec exec = zipFactory.createZipExec();
			exec.setZipFile(output);

			ZipExecPart part = new ZipExecPart();
			part.sourceDirectory = temp;
			part.relativePaths.add(libs.getName());
			exec.addPart(part);
			exec.createZip();
		} finally {
			FileUtils.deleteDirectory(temp);
		}

	}

	private void copyLibsForSelfContainment(ZipExecFactory zipFactory, File destDir, String buildVersion)
			throws Exception {
		if (data.isBinary()) {
			disruptBinaryJar(zipFactory, destDir);
		} else {
			execJarCommands(zipFactory, destDir, buildVersion, false);
		}
		File pd = getPluginDirectory();

		if (pd != null && pd.exists() && pd.isDirectory()) {
			for (String inc : getData().getBinaryIncludes()) {
				if (".".equals(inc)) {
					continue;
				}

				File binFile = new File(pd, inc);
				if (binFile.exists() && binFile.isFile() && binFile.getName().toLowerCase().endsWith(".jar")) {
					FileUtils.copyFileToDirectory(binFile, destDir);
				}
			}
		}

		for (PluginBuild dep : getSourceDependencies()) {
			dep.copyLibsForSelfContainment(zipFactory, destDir, buildVersion);
		}
	}

	private void disruptBinaryJar(ZipExecFactory zipFactory, File destDir) throws Exception {
		// unpack the binary, remove META-INF, move all jars to destDir, re-pack
		// all remaining
		// files into new jar
		File binFile = getBinaryJarFile();
		File tmp = new File(destDir, binFile.getName() + "_" + System.currentTimeMillis());
		tmp.mkdirs();

		final ZipExec exec = zipFactory.createZipExec();
		exec.unzip(binFile, tmp);
		File meta = new File(tmp, "META-INF");
		if (meta.exists() && meta.isDirectory()) {
			FileUtils.deleteDirectory(meta);
		}

		moveJarsRecursive(tmp, destDir);

		File target = new File(destDir, getPluginName() + "_stripped.jar");

		// now create jar of the "remains"
		FileUtils.delete(target);

		// create the ZIP executor
		exec.setZipFile(target);
		exec.setJarMode(true);

		// run ZIP on the remains of the original jar
		ZipExecPart part = new ZipExecPart();
		part.sourceDirectory = tmp;
		part.relativePaths.add(".");
		part.excludeGit = true;
		exec.addPart(part);
		exec.createZip();

		FileUtils.deleteDirectory(tmp);
	}

	private void moveJarsRecursive(File tmp, File destDir) throws IOException {
		File[] files = tmp.listFiles();
		if (files == null || files.length == 0) {
			return;
		}

		for (File f : files) {
			if (f.isFile() && f.getName().endsWith(".jar")) {
				FileUtils.moveFile(f, new File(destDir, f.getName()));
			}
			if (f.isDirectory()) {
				moveJarsRecursive(f, destDir);
			}
		}
	}
}
