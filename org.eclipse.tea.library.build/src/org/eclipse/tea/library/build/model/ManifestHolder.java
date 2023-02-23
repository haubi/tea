/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.library.build.model;

import static org.eclipse.tea.library.build.model.BundleData.splitList;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tea.library.build.internal.Activator;

/**
 * Abstraction of the famous {@code MANIFEST.MF} file.
 * <p>
 * Supports manipulation of the manifest whilst keeping track of the order of
 * headers.
 */
final class ManifestHolder {

	private static final ParameterValue[] EMPTY_VALUES = {};

	private final Map<String, ManifestAttribute> attributes = new TreeMap<>();
	private final File referenceFile;

	private ManifestHolder(final Manifest manifest, final File referenceFile) {
		final Attributes attr = manifest.getMainAttributes();
		for (Object key : attr.keySet()) {
			final String name = key.toString();
			final String[] values = splitList(attr, name);
			attributes.put(name, new ManifestAttribute(name, values));
		}

		this.referenceFile = referenceFile;
	}

	static ManifestHolder fromManifest(Manifest manifest, File manifestFile) {
		if (manifest == null) {
			return null;
		}
		return new ManifestHolder(manifest, manifestFile);
	}

	/**
	 * Returns the value list to which the specified key is mapped, or
	 * {@code null} if this map contains no mapping for the key.
	 */
	private ParameterValue[] getValues(String key) {
		ManifestAttribute attr = attributes.get(key);
		if (attr == null) {
			return null;
		}
		return attr.values;
	}

	private ParameterValue[] safeList(String name) {
		ParameterValue[] result = getValues(name);
		if (result == null) {
			return EMPTY_VALUES;
		}
		return result;
	}

	ParameterValue[] getListAttribute(String name) {
		return safeList(name);
	}

	ParameterValue getSingleAttribute(String name) {
		return firstValue(name);
	}

	/**
	 * Returns the first parameter value to which the specified key is mapped,
	 * or {@code null} if this map contains no mapping for the key or there are
	 * no values.
	 */
	private ParameterValue firstValue(String key) {
		ParameterValue[] result = getValues(key);
		if (result == null || result.length < 1) {
			return null;
		}
		return result[0];
	}

	private String[] safeValueList(String name) {
		ParameterValue[] result = getValues(name);
		if (result == null) {
			return BundleData.EMPTY_STRINGS;
		}
		return ParameterValue.valuesFromList(result);
	}

	private String getSimple(String name) {
		ParameterValue pv = firstValue(name);
		if (pv == null) {
			return null;
		}
		return pv.getValue();
	}

	private boolean getBoolean(String name) {
		return Boolean.parseBoolean(getSimple(name));
	}

	void putSimple(String name, String value) {
		attributes.put(name, new ManifestAttribute(name, value));
	}

	private void putList(String name, String[] values) {
		attributes.put(name, new ManifestAttribute(name, values));
	}

	private void putList(String name, ParameterValue[] values) {
		attributes.put(name, new ManifestAttribute(name, values));
	}

	ParameterValue getSymbolicName() {
		return firstValue("Bundle-SymbolicName");
	}

	String getDescription() {
		return getSimple("Bundle-Name");
	}

	String getActivator() {
		return getSimple("Bundle-Activator");
	}

	void setActivator(String activator) {
		if (activator == null) {
			attributes.remove("Bundle-Activator");
		} else {
			putSimple("Bundle-Activator", activator);
		}
	}

	void addDependency(String pluginName, boolean optional) {
		ManifestAttribute reqBundleAttr = attributes.get("Require-Bundle");
		ParameterValue[] dependencies = reqBundleAttr.values;
		ParameterValue[] newDependecies = new ParameterValue[dependencies.length + 1];
		System.arraycopy(dependencies, 0, newDependecies, 0, dependencies.length);

		String param = optional ? ";resolution:=optional" : "";
		newDependecies[newDependecies.length - 1] = new ParameterValue(pluginName + param);

		ManifestAttribute manifestAttr = new ManifestAttribute("Require-Bundle", newDependecies);
		attributes.put("Require-Bundle", manifestAttr);
	}

	ParameterValue[] getDependencies() {
		return safeList("Require-Bundle");
	}

	ParameterValue[] getMavenDependencies() {
		if (attributes.containsKey("WAMAS-Build-Maven")) {
			// DEPRECATED
			return safeList("WAMAS-Build-Maven");
		}
		return safeList("Build-Maven");
	}

	ParameterValue[] getImportPackages() {
		return safeList("Import-Package");
	}

	ParameterValue[] getExportPackages() {
		return safeList("Export-Package");
	}

	void setExportPackages(ParameterValue[] value) {
		if (value == null || value.length == 0) {
			attributes.remove("Export-Package");
		} else {
			putList("Export-Package", value);
		}
	}

	String getPlatformFilter() {
		return getSimple("Eclipse-PlatformFilter");
	}

	void setPlatformFilter(String value) {
		putSimple("Eclipse-PlatformFilter", value);
	}

	String[] getClassPath() {
		return safeValueList("Bundle-ClassPath");
	}

	void setClassPath(String[] values) {
		putList("Bundle-ClassPath", values);
	}

	ParameterValue[] getNativeCode() {
		return safeList("Bundle-NativeCode");
	}

	String[] getRequiredExecutionEnvironment() {
		return safeValueList("Bundle-RequiredExecutionEnvironment");
	}

	void setRequiredExecutionEnvironment(String[] values) {
		putList("Bundle-RequiredExecutionEnvironment", values);
	}

	void setAutomaticModuleName(String value) {
		putSimple("Automatic-Module-Name", value);
	}

	String getAutomaticModuleName() {
		return getSimple("Automatic-Module-Name");
	}

	String getBundleVersion() {
		return getSimple("Bundle-Version");
	}

	void setBundleVersion(String value) {
		putSimple("Bundle-Version", value);
	}

	String getBundleVendor() {
		return getSimple("Bundle-Vendor");
	}

	void setBundleVendor(String value) {
		putSimple("Bundle-Vendor", value);
	}

	String getActivationPolicy() {
		return getSimple("Bundle-ActivationPolicy");
	}

	void setLazyActivationPolicy() {
		putSimple("Bundle-ActivationPolicy", "lazy");
	}

	/**
	 * Returns the fragment host, or {@code null} if no fragment host is
	 * defined.
	 */
	ParameterValue getFragmentHost() {
		return firstValue("Fragment-Host");
	}

	/**
	 * Returns {@code true} if the 'unpack' flag must be set (used by features).
	 */
	boolean getNeedUnpack() {
		ParameterValue value = firstValue("Eclipse-BundleShape");
		if (value != null && value.getValue() != null && value.getValue().equals("dir")) {
			return true;
		}

		return false;
	}

	String[] getBinaryInclude() {
		return safeValueList("Binary-Include");
	}

	private static final String MANIFEST_VERSION = "Manifest-Version";

	void write(File mfFile) throws IOException {
		// calculate the order of the manifest entries
		if (referenceFile != null) {
			calculateOrder();
		}

		// always provide a manifest version
		ManifestAttribute mfVer = attributes.get(MANIFEST_VERSION);
		if (mfVer == null) {
			mfVer = new ManifestAttribute(MANIFEST_VERSION, "1.0");
			attributes.put(MANIFEST_VERSION, mfVer);
		}
		mfVer.order = Integer.MIN_VALUE;

		// sort the manifest entries
		List<ManifestAttribute> sorted = new ArrayList<>(attributes.values());
		Collections.sort(sorted, new Comparator<ManifestAttribute>() {

			@Override
			public int compare(ManifestAttribute a1, ManifestAttribute a2) {
				if (a1.order < a2.order) {
					return -1;
				}
				if (a1.order > a2.order) {
					return 1;
				}
				return a1.name.compareTo(a2.name);
			}
		});

		// write the manifest
		Writer ps = new FileWriter(mfFile);
		try {
			for (ManifestAttribute mf : sorted) {
				writeAttribute(mf.name, mf.values, ps);
			}
		} finally {
			ps.close();
		}
	}

	private static void writeAttribute(String name, ParameterValue[] value, Writer ps) throws IOException {
		ps.write(name);
		ps.write(": ");
		boolean firstLine = true;
		for (ParameterValue pv : value) {
			if (firstLine) {
				firstLine = false;
			} else {
				ps.write(",\n ");
			}
			pv.write(ps);
		}
		ps.write('\n');
	}

	private void calculateOrder() {
		List<String> lines = new ArrayList<>();
		try {
			LineNumberReader r = new LineNumberReader(new FileReader(referenceFile));
			try {
				String line;
				while ((line = r.readLine()) != null) {
					lines.add(line);
				}
			} finally {
				r.close();
			}
		} catch (Exception e) {
			Activator.log(IStatus.ERROR, "cannot read " + referenceFile, e);
			return;
		}

		int index = 0;
		for (String line : lines) {
			for (ManifestAttribute mf : attributes.values()) {
				if (line.startsWith(mf.name + ':')) {
					mf.order = index;
					break;
				}
			}
			++index;
		}
	}

	public boolean migrateUnpackInformation() {
		if (getBoolean("Unpack")) {
			putSimple("Eclipse-BundleShape", "dir");
			attributes.remove("Unpack");
			return true;
		}
		return false;
	}

	public ParameterValue getBuddyPolicy() {
		return firstValue("Eclipse-BuddyPolicy");
	}

	public ParameterValue[] getBuddyRegistrations() {
		return safeList("Eclipse-RegisterBuddy");
	}

}
