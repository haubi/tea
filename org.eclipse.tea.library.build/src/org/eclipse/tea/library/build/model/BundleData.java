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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tea.library.build.internal.Activator;
import org.osgi.framework.Version;

/**
 * Stores useful information about an OSGi bundle.
 */
public abstract class BundleData {

	/**
	 * the name of the bundle
	 */
	private final String bundleName;

	/**
	 * the bundle directory; {@code null} for a JAR distribution
	 */
	protected final File bundleDir;

	/**
	 * {@code true} for a source distribution; {@code false} for a binary
	 * distribution
	 */
	protected final boolean hasSource;

	/**
	 * the JAR file; {@code null} for a source distribution or if the JAR is
	 * extracted
	 */
	protected final File jarFile;

	/**
	 * content of the manifest file; optional
	 */
	protected final ManifestHolder manifest;

	static final String[] EMPTY_STRINGS = {};

	/**
	 * Creates the bundle data.
	 *
	 * @param projectName
	 *            external project name; only in use if we couldn't read the
	 *            manifest
	 * @param bundleDir
	 *            the bundle directory; {@code null} for a JAR distribution
	 * @param hasSource
	 *            {@code true} for a source distribution; {@code false} for a
	 *            binary distribution
	 * @param jarFile
	 *            the JAR file; {@code null} for a source distribution or if the
	 *            JAR is extracted
	 */
	protected BundleData(String projectName, File bundleDir, boolean hasSource, File jarFile) {
		this.bundleDir = bundleDir;
		this.hasSource = hasSource;
		this.jarFile = jarFile;

		// read manifest
		if (jarFile != null) {
			manifest = readManifestFromJar(jarFile);
		} else {
			if (bundleDir.isDirectory()) {
				manifest = readManifestFromDirectory(bundleDir);
			} else {
				// maybe the project is not installed
				manifest = null;
			}
		}

		// get the bundle name
		if (manifest != null) {
			ParameterValue symName = manifest.getSymbolicName();
			if (symName != null) {
				bundleName = symName.getValue();
				if (projectName != null && !bundleName.equals(projectName)) {
					Activator.log(IStatus.WARNING,
							"Missmatch of names: projectName=" + projectName + "  bundleName=" + bundleName, null);
				}
			} else {
				bundleName = projectName;
			}
		} else {
			bundleName = projectName;
		}
	}

	/**
	 * Returns {@code true} if full access to the meta-data is possible.
	 */
	public boolean isMetadataOK() {
		return manifest != null;
	}

	/**
	 * Returns the name of the bundle.
	 */
	public String getBundleName() {
		return bundleName;
	}

	/**
	 * Returns the bundle directory; {@code null} for a JAR distribution.
	 */
	public final File getBundleDir() {
		return bundleDir;
	}

	/**
	 * Returns {@code true} for a source distribution; {@code false} for a
	 * binary distribution.
	 */
	public final boolean hasSource() {
		return hasSource;
	}

	/**
	 * Returns the JAR file; {@code null} for a source distribution or if the
	 * JAR is extracted.
	 */
	public final File getJarFile() {
		return jarFile;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(\"" + getBundleName() + "\")";
	}

	/**
	 * Re-writes the manifest file; only possible for source distributions.
	 *
	 * @return {@code true} if we could write the manifest file
	 */
	public final boolean writeManifest() {
		final File manifestFile = getManifestFile();
		if (manifestFile == null) {
			return false;
		}
		try {
			manifest.write(manifestFile);
			return true;
		} catch (Exception ex) {
			throw new IllegalStateException("cannot write " + manifestFile, ex);
		}
	}

	/**
	 * Returns the path to the manifest file.
	 *
	 * @return path to the manifest file; {@code null} if not supported
	 */
	public final File getManifestFile() {
		if (manifest == null || !hasSource || bundleDir == null) {
			return null;
		}
		return new File(bundleDir, "META-INF/MANIFEST.MF");
	}

	/**
	 * Returns the version string of this bundle.
	 */
	public abstract String getBundleVersion();

	/**
	 * Returns the version of the bundle as {@link Version}
	 */
	public final Version getVersion() {
		return Version.parseVersion(getBundleVersion());
	}

	/**
	 * Sets the version string of this bundle.
	 */
	public abstract void setBundleVersion(String value);

	public static void copyManifestFromDirectory(File srcBundleDir, File destFile,
			Map<String, String> additionalAttributes) throws IOException {
		ManifestHolder mfh = readManifestFromDirectory(srcBundleDir);
		additionalAttributes.forEach((name, value) -> mfh.putSimple(name, value));
		mfh.write(destFile);
	}

	protected static ManifestHolder readManifestFromDirectory(File bundleDir) {
		File manifestFile = new File(bundleDir, "META-INF/MANIFEST.MF");
		if (!manifestFile.isFile()) {
			return null;
		}
		try {
			FileInputStream fis = new FileInputStream(manifestFile);
			try {
				Manifest mf = new Manifest(fis);
				return ManifestHolder.fromManifest(mf, manifestFile);
			} finally {
				fis.close();
			}
		} catch (Exception ex) {
			throw new IllegalStateException("cannot read " + manifestFile, ex);
		}
	}

	protected static ManifestHolder readManifestFromJar(File jarFile) {
		try {
			JarFile jar = new JarFile(jarFile);
			try {
				Manifest mf = jar.getManifest();
				return ManifestHolder.fromManifest(mf, null);
			} finally {
				jar.close();
			}
		} catch (Exception ex) {
			throw new IllegalStateException("cannot read " + jarFile, ex);
		}
	}

	protected static Properties readBuildPropertiesFromDirectory(File bundleDir) {
		File propFile = new File(bundleDir, "build.properties");
		if (!propFile.isFile()) {
			return null;
		}
		try {
			FileInputStream fis = new FileInputStream(propFile);
			try {
				Properties result = new Properties();
				result.load(fis);
				return result;
			} finally {
				fis.close();
			}
		} catch (Exception ex) {
			throw new IllegalStateException("cannot read " + propFile, ex);
		}
	}

	protected static Properties readBuildPropertiesFromJar(File jarFile) {
		try {
			JarFile jar = new JarFile(jarFile);
			try {
				ZipEntry ze = jar.getEntry("build.properties");
				if (ze == null) {
					return null;
				}
				InputStream is = jar.getInputStream(ze);
				try {
					Properties result = new Properties();
					result.load(is);
					is.close();
					return result;
				} finally {
					is.close();
				}
			} finally {
				jar.close();
			}
		} catch (Exception ex) {
			throw new IllegalStateException("cannot read " + jarFile, ex);
		}
	}

	protected static String[] splitList(Attributes attributes, String name) {
		String value = attributes.getValue(name);
		return splitList(value);
	}

	protected static String[] splitList(Properties props, String name) {
		String value = props.getProperty(name);
		return splitList(value);
	}

	private static String[] splitList(String value) {
		if (value == null) {
			return EMPTY_STRINGS;
		}

		// we cannot call value.split(",") because of substrings like "[5.1.0,
		// 5.2.0)"
		List<String> parts = new ArrayList<>();
		final int len = value.length();
		char[] chars = value.toCharArray();
		int i1 = 0, i2 = 1;
		while (i2 < len) {
			char c = chars[i2];
			if (c == ',') {
				addPart(parts, value.substring(i1, i2).trim());
				i1 = i2 + 1;
				i2 = i1 + 1;
			} else if (c == '"') {
				++i2;
				while (i2 < len) {
					c = chars[i2];
					++i2;
					if (c == '"') {
						break;
					}
				}
			} else {
				++i2;
			}
		}
		if (i1 < len) {
			addPart(parts, value.substring(i1).trim());
		}
		return parts.toArray(new String[parts.size()]);
	}

	private static void addPart(List<String> parts, String v) {
		if (v.isEmpty() || v.equals(",")) {
			return;
		}
		parts.add(v);
	}

	protected static String[] mergeLists(String[] a, String[] b) {
		if (a.length < 1) {
			return b;
		}
		if (b.length < 1) {
			return a;
		}
		String[] result = new String[a.length + b.length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	protected static Map<String, List<String>> splitMap(Properties props, String prefix) {
		Map<String, List<String>> result = new TreeMap<>();
		for (Entry<Object, Object> entry : props.entrySet()) {
			String fullKey = entry.getKey().toString();
			if (fullKey.startsWith(prefix)) {
				String itemName = fullKey.substring(prefix.length());
				result.put(itemName, Arrays.asList(splitList(entry.getValue().toString())));
			}
		}
		return result;
	}

	public abstract IProject getProject();

	public final void refreshProject() {
		IProject project = getProject();
		if (project != null && project.exists() && project.isOpen()) {
			try {
				project.refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	public void addDependency(String pluginName, boolean optional) {
		manifest.addDependency(pluginName, optional);
	}

	public ParameterValue getManifestHeader(String name) {
		if (manifest == null) {
			return null;
		}
		return manifest.getSingleAttribute(name);
	}

	public ParameterValue[] getManifestHeaderList(String name) {
		return manifest.getListAttribute(name);
	}

	public String getSimpleManifestValue(String name) {
		ParameterValue pv = getManifestHeader(name);
		if (pv == null) {
			return null;
		}
		return pv.getValue();
	}

	public boolean isJarBundleShape() {
		String x = getSimpleManifestValue("Eclipse-BundleShape");
		return !"dir".equals(x); // "jar" or null
		// no support for PDEs old "unpack" guessing
	}
}
