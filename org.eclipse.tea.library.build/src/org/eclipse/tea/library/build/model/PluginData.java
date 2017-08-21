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
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.tea.library.build.util.StringHelper;
import org.osgi.framework.Version;

/**
 * Stores useful information about a RCP plugin.
 */
public class PluginData extends BundleData {

	/**
	 * the Eclipse plugin project
	 */
	protected final IProject project;

	protected final ParameterValue[] dependencies;
	protected final ParameterValue[] mavenDependencies;
	protected final ParameterValue[] imports;
	protected ParameterValue[] exports;
	protected final String platformFilter;
	protected String[] classPath;
	protected final String[] requiredExecutionEnvironment;

	/**
	 * the fragment host, or {@code null} if no fragment host is defined
	 */
	protected final ParameterValue fragmentHost;

	protected final Properties buildProperties;
	protected final Map<String, List<String>> sourceFolders, binaryFolders;

	protected final String[] binaryIncludes;

	protected final String description;

	/**
	 * Creates the plugin data for a source distribution.
	 *
	 * @param project
	 *            Eclipse project
	 */
	public PluginData(IProject project) {
		this(project.getName(), project.getLocation().toFile(), true, null, project);
	}

	/**
	 * Creates the plugin data for a binary distribution.
	 *
	 * @param binaryDistribution
	 *            JAR file or directory of an extracted JAR
	 */
	public static PluginData createFromBinaryDistribution(File binaryDistribution) {
		if (binaryDistribution.isFile()) {
			String jarName = binaryDistribution.getName().toLowerCase();
			if (jarName.endsWith(".jar")) {
				return new PluginData(null, null, false, binaryDistribution, null);
			}
			throw new IllegalArgumentException("illegal JAR name: " + jarName);
		}
		if (binaryDistribution.isDirectory()) {
			return new PluginData(null, binaryDistribution, false, null, null);
		}
		throw new IllegalArgumentException(binaryDistribution.getPath());
	}

	protected PluginData(String projectName, File bundleDir, boolean hasSource, File jarFile, IProject project) {
		super(projectName, bundleDir, hasSource, jarFile);

		this.project = project;

		if (jarFile != null) {
			buildProperties = readBuildPropertiesFromJar(jarFile);
		} else {
			buildProperties = readBuildPropertiesFromDirectory(bundleDir);
		}

		final String[] binInc1;
		if (manifest != null) {
			dependencies = manifest.getDependencies();
			mavenDependencies = manifest.getMavenDependencies();
			imports = manifest.getImportPackages();
			exports = manifest.getExportPackages();
			classPath = manifest.getClassPath();
			requiredExecutionEnvironment = manifest.getRequiredExecutionEnvironment();
			fragmentHost = manifest.getFragmentHost();
			platformFilter = manifest.getPlatformFilter();
			description = manifest.getDescription();
			binInc1 = manifest.getBinaryInclude();
		} else {
			dependencies = null;
			mavenDependencies = null;
			imports = null;
			exports = null;
			classPath = null;
			requiredExecutionEnvironment = null;
			fragmentHost = null;
			platformFilter = null;
			description = null;
			binInc1 = EMPTY_STRINGS;
		}

		final String[] binInc2;
		if (buildProperties != null) {
			sourceFolders = splitMap(buildProperties, "source.");
			binaryFolders = splitMap(buildProperties, "output.");
			binInc2 = splitList(buildProperties, "bin.includes");
		} else {
			sourceFolders = Collections.emptyMap();
			binaryFolders = Collections.emptyMap();
			binInc2 = EMPTY_STRINGS;
		}

		binaryIncludes = mergeLists(binInc1, binInc2);
	}

	@Override
	public final IProject getProject() {
		return project;
	}

	public boolean isBinary() {
		// Note: DON'T cache this information here (so we don't need to
		// reconstruct PluginData to get current information)!
		return WorkspaceData.isBinaryProject(project);
	}

	public final ParameterValue[] getDependencies() {
		return dependencies;
	}

	public final ParameterValue[] getMavenDependencies() {
		return mavenDependencies;
	}

	public final ParameterValue[] getPackageImports() {
		if (imports == null) {
			return new ParameterValue[0];
		}
		return imports;
	}

	public final ParameterValue[] getPackageExports() {
		if (exports == null) {
			return new ParameterValue[0];
		}
		return exports;
	}

	public final void setPackageExports(ParameterValue[] v) {
		exports = v;
		manifest.setExportPackages(v);
	}

	public final String[] getClassPath() {
		return classPath;
	}

	public final void setClassPath(String[] cp) {
		manifest.setClassPath(cp); // don't refresh our model
	}

	public final String[] getRequiredExecutionEnvironment() {
		return requiredExecutionEnvironment;
	}

	public final void setRequiredExecutionEnvironment(String[] values) {
		manifest.setRequiredExecutionEnvironment(values);
	}

	@Override
	public final String getBundleVersion() {
		if (manifest == null) {
			return null;
		}
		return manifest.getBundleVersion();
	}

	@Override
	public final void setBundleVersion(String value) {
		manifest.setBundleVersion(value);
	}

	public final String getBundleVendor() {
		return manifest.getBundleVendor();
	}

	public final void setBundleVendor(String value) {
		manifest.setBundleVendor(value);
	}

	public final String getBuddyPolicy() {
		ParameterValue buddyPolicy = manifest.getBuddyPolicy();
		return buddyPolicy == null ? null : buddyPolicy.value;
	}

	public final ParameterValue[] getBuddyRegistrations() {
		return manifest.getBuddyRegistrations();
	}

	public final boolean isLazyActivationPolicy() {
		return StringHelper.compare("lazy", manifest.getActivationPolicy()) == 0;
	}

	public final void setLazyActivationPolicy() {
		manifest.setLazyActivationPolicy();
	}

	/**
	 * Returns the fragment host, or {@code null} if no fragment host is
	 * defined.
	 */
	public final ParameterValue getFragmentHost() {
		return fragmentHost;
	}

	public final String[] getSourceFolders() {
		return sourceFolders.values().stream().flatMap(List::stream).toArray(String[]::new);
	}

	public final Map<String, List<String>> getBinaryFolders() {
		return binaryFolders;
	}

	public final String[] getBinaryIncludes() {
		return binaryIncludes;
	}

	public final String getPlatformFilter() {
		return platformFilter;
	}

	public String getBundleActivator() {
		if (manifest == null) {
			return null;
		}

		return manifest.getActivator();
	}

	/**
	 * @param activator
	 *            the activator to use, may be <code>null</code> to removed the
	 *            header.
	 */
	public void setBundleActivator(String activator) {
		manifest.setActivator(activator);
	}

	public List<String> getBinaryClassPath() {
		if (jarFile != null) {
			return Collections.singletonList(jarFile.getAbsolutePath());
		}
		List<String> result = new ArrayList<>();
		for (String cp : classPath) {
			if (".".equals(cp)) {
				continue;
			}
			File f = new File(bundleDir, cp);
			result.add(f.getAbsolutePath());
		}
		return result;
	}

	void writeHtmlListelement(Writer w) throws IOException {
		w.write("<li>");
		w.write(getBundleName());
		if (dependencies != null) {
			w.write("<br>Dependencies:\n<ul>\n");
			for (ParameterValue dep : dependencies) {
				dep.writeHtmlListelement(w);
			}
			w.write("</ul>\n");
		}
		if (mavenDependencies != null) {
			w.write("<br>Maven JAR Dependencies:\n<ul>\n");
			for (ParameterValue dep : mavenDependencies) {
				dep.writeHtmlListelement(w);
			}
			w.write("</ul>\n");
		}
		w.write("</li>\n");
	}

	/**
	 * Checks whether this plugin exports a package with the given name. only
	 * call this if {@link BundleData#isMetadataOK()} returns true!
	 *
	 * @param id
	 *            the id of the package to look for
	 */
	public boolean exportsPackage(String id, VersionRange range) {
		if (exports == null) {
			return false;
		}

		for (ParameterValue val : exports) {
			if (!val.value.equals(id)) {
				continue;
			}

			String exportVersion = val.getStringParameter("version");
			Version v;
			if (exportVersion != null && !exportVersion.isEmpty()) {
				v = Version.parseVersion(exportVersion);
			} else {
				v = Version.emptyVersion;
			}
			if (range == null || range.isIncluded(v)) {
				return true;
			}
		}
		return false;
	}

	public final ParameterValue[] getImportPackages() {
		return imports;
	}

	public ParameterValue getManifestHeader(String name) {
		return manifest.getSingleAttribute(name);
	}

	public ParameterValue[] getManifestHeaderList(String name) {
		return manifest.getListAttribute(name);
	}

	public final String getDescription() {
		return description;
	}

	public boolean migrateUnpackInformation() {
		return manifest.migrateUnpackInformation();
	}

	private Map<String, String> getExternalizeClasspath() {
		Map<String, String> result = new TreeMap<>();
		for (ParameterValue v : getManifestHeaderList("Externalize-ClassPath")) {
			String from = v.value;
			String to = v.getStringParameter("map");

			result.put(from, to);
		}
		return result;
	}

	/**
	 * @deprecated use non-WAMAS specifically named version
	 *             ({@link #getExternalizeClasspath()}).
	 */
	@Deprecated
	private Map<String, String> getWamasExternalizeClasspath() {
		Map<String, String> result = new TreeMap<>();
		for (ParameterValue v : getManifestHeaderList("WAMAS-Externalize-ClassPath")) {
			String from = v.value;
			String to = v.getStringParameter("map");

			result.put(from, to);
		}
		return result;
	}

	String getSimpleManifestValue(String name) {
		ParameterValue pv = getManifestHeader(name);
		if (pv == null) {
			return null;
		}
		return pv.value;
	}

	/**
	 * @deprecated use non-WAMAS specifically named version
	 */
	@Deprecated
	String getWamasPrefixNativeCode() {
		return getSimpleManifestValue("WAMAS-Prefix-NativeCode");
	}

	String getPrefixNativeCode() {
		return getSimpleManifestValue("Prefix-NativeCode");
	}

	/**
	 * ATTENTION: this method may ONLY be called while building JAR files. The
	 * MANIFEST.MF file MUST be backed up and restored after the operation.
	 */
	public void updateManifestForBinaryDeployment() {
		if (getManifestFile() == null || manifest == null) {
			return;
		}

		// re-read the manifest, manipulate and write the changes.
		ManifestHolder temp = readManifestFromDirectory(bundleDir);
		Map<String, String> updates = getExternalizeClasspath();
		updates.putAll(getWamasExternalizeClasspath());

		if (temp == null) {
			return;
		}

		String[] cp = manifest.getClassPath();
		if (!updates.isEmpty() && cp.length > 0) {
			String[] target = new String[cp.length];
			System.arraycopy(cp, 0, target, 0, cp.length);

			for (int i = 0; i < cp.length; ++i) {
				String replacement = updates.get(cp[i]);
				if (replacement != null) {
					target[i] = replacement;
				}
			}

			temp.setClassPath(target);
		}

		String prefix = getWamasPrefixNativeCode();
		if (prefix == null) {
			prefix = getPrefixNativeCode();
		}
		ParameterValue[] nc = temp.getNativeCode();
		if (prefix != null && nc.length > 0) {
			for (int i = 0; i < nc.length; ++i) {
				nc[i].value = prefix + nc[i].value;
			}
		}

		try {
			temp.write(getManifestFile());
		} catch (Exception e) {
			throw new RuntimeException("cannot update manifest for binary deployment", e);
		}
	}
}
