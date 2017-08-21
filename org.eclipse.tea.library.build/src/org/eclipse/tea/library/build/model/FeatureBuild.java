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
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.jar.ZipExec;
import org.eclipse.tea.library.build.jar.ZipExecFactory;
import org.eclipse.tea.library.build.jar.ZipExecPart;
import org.eclipse.tea.library.build.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides information about building a RCP feature.
 */
public class FeatureBuild extends BundleBuild<FeatureData> {

	protected final WorkspaceBuild workspace;

	protected FeatureBuild(FeatureData data, WorkspaceBuild workspace) {
		super(data);
		this.workspace = workspace;
	}

	/**
	 * Returns the name of this feature.
	 */
	public final String getFeatureName() {
		return data.getBundleName();
	}

	/**
	 * Returns the set of local plugins, which defines the content of this
	 * feature.
	 */
	public final Set<PluginBuild> getIncludedPlugins() {
		Set<PluginBuild> result = new TreeSet<>();
		for (PluginInfo info : data.getPluginInfos()) {
			PluginBuild plugin = workspace.getSourcePlugin(info.id);
			if (plugin != null) {
				result.add(plugin);
			} else {
				throw new IllegalStateException("cannot find plugin " + info.id);
			}
		}
		return result;
	}

	@Override
	public String getJarFileName(String buildVersion) {
		return getFeatureName() + '_' + buildVersion + ".jar";
	}

	/**
	 * Adds a single feature=version property to the given file, creating it if
	 * it does not exist.
	 *
	 * @param propFile
	 *            The filename of the property file.
	 * @param feature
	 *            the feature to add
	 */
	public static void generateProperty(File propFile, JarManager jarManager, FeatureBuild feature) throws Exception {
		Properties props = new Properties();

		final String buildVersion = feature.getData().getBundleVersion();
		props.setProperty(feature.getFeatureName(), buildVersion);

		updateProperties(propFile, props);
	}

	/**
	 * Updates a PDE build property file.
	 *
	 * @param propFile
	 *            The filename of the property file.
	 * @param props
	 *            Contains all properties which have to be added to the maybe
	 *            existing file.
	 */
	public static void updateProperties(File propFile, Properties props) throws Exception {
		Properties propsExists = new Properties();
		if (propFile.exists()) {
			propsExists.putAll(FileUtils.readProperties(propFile));
		}
		propsExists.putAll(props);
		FileUtils.writeProperties(propsExists, propFile);
	}

	public void backupFeatureXml() throws IOException {
		File f = data.getFeatureXmlFile();
		if (f.exists()) {
			FileUtils.moveFile(f, new File(f.getParentFile(), f.getName() + "._bak_"));
		}
	}

	public void restoreFeatureXml() throws IOException {
		File f = data.getFeatureXmlFile();
		File bak = new File(f.getParentFile(), f.getName() + "._bak_");
		if (bak.exists()) {
			FileUtils.delete(f);
			FileUtils.moveFile(bak, f);
			data.reloadFeatureXml();
		}
	}

	/**
	 * Creates the feature.xml dynamically depending on the included plug-ins.
	 *
	 * @param plugins
	 *            a set of all required plugins
	 */
	public void generateFeatureXml(JarManager jm, Iterable<PluginBuild> plugins, Iterable<PluginBuild> strictPlugins)
			throws Exception {
		final Document doc = data.generateFeatureHeader();
		final Element root = doc.getDocumentElement();

		for (PluginBuild plugin : plugins) {
			genSinglePluginElement(jm, doc, root, plugin, false);
		}

		for (PluginBuild plugin : strictPlugins) {
			genSinglePluginElement(jm, doc, root, plugin, true);
		}

		final File xmlFile = data.getFeatureXmlFile();
		FeatureData.writeXml(doc, xmlFile);
		data.reloadFeatureXml();
	}

	private void genSinglePluginElement(JarManager jm, Document doc, Element root, PluginBuild plugin,
			boolean strictVersion) throws Exception {
		String platformFilter = plugin.getData().getPlatformFilter();
		Element pluginElement = null;
		if (platformFilter != null) {
			PlatformTriple[] platforms = PlatformTriple.getAllPlatforms();
			boolean used = false;
			for (PlatformTriple platform : platforms) {
				boolean match = platform.matchFilter(platformFilter);
				if (match) {
					pluginElement = generatePluginElement(doc, plugin);
					pluginElement.setAttribute("os", platform.os);
					pluginElement.setAttribute("ws", platform.ws);
					pluginElement.setAttribute("arch", platform.arch);
					root.appendChild(pluginElement);
					used = true;
				}
			}
			if (!used) {
				System.out.println(
						"Warning: ignoring plugin " + plugin.getPluginName() + ". No supported platform found.");
			}
		} else {
			pluginElement = generatePluginElement(doc, plugin);
			root.appendChild(pluginElement);
		}

		if (strictVersion && pluginElement != null) {
			String version = jm.getBundleVersion(plugin.getData());
			pluginElement.setAttribute("version", version);
		}
	}

	/**
	 * generates a basic plug-in element.
	 *
	 * @param doc
	 *            the document needed for creation
	 * @param plugin
	 *            PluginBuild containing required data
	 * @return basic element 'plug-in'
	 */
	private Element generatePluginElement(Document doc, PluginBuild plugin) {
		Element pluginElement = doc.createElement("plugin");
		pluginElement.setAttribute("id", plugin.getPluginName());
		pluginElement.setAttribute("download-size", "0");
		pluginElement.setAttribute("install-size", "0");
		pluginElement.setAttribute("version", "0.0.0");

		// WARNING: this is seemingly not evaluate if version is unrestricted
		// (0.0.0). Rather use
		// the Eclipse-BundleShape header in MANIFEST.MF files, which always
		// works.
		pluginElement.setAttribute("unpack", Boolean.toString(plugin.needUnpack()));
		if (plugin.getData().getFragmentHost() != null) {
			pluginElement.setAttribute("fragment", "true");
		}
		return pluginElement;
	}

	/**
	 * Creates the JAR file for this feature.
	 */
	@Override
	public File execJarCommands(ZipExecFactory zip, File distDirectory, String buildVersion, JarManager jarManager)
			throws Exception {
		File jarFile = new File(distDirectory, getJarFileName(buildVersion));

		// remove the jar file
		FileUtils.delete(jarFile);

		// write feature.xml to temporary directory
		File tmpDir = new File(distDirectory, "jar_" + getFeatureName());
		FileUtils.mkdirs(tmpDir);
		File tmpFile = writeUpdatedFeatureXml(tmpDir, buildVersion, jarManager);

		// create JAR
		ZipExec exec = zip.createZipExec();
		exec.setZipFile(jarFile);
		exec.setJarMode(true);

		// add the feature.xml file
		ZipExecPart part = new ZipExecPart();
		part.sourceDirectory = tmpDir;
		part.relativePaths.add(tmpFile.getName());
		exec.addPart(part);

		// if it exists, add a feature.properties too.
		// getData().getFeatureXmlFile() is guaranteed non-null
		File featureProps = new File(getData().getFeatureXmlFile().getParentFile(), "feature.properties");
		if (featureProps.exists()) {
			ZipExecPart propPart = new ZipExecPart();
			propPart.sourceDirectory = featureProps.getParentFile();
			propPart.relativePaths.add(featureProps.getName());
			exec.addPart(propPart);
		}

		// create zip
		exec.createZip();

		// remove temporary elements
		FileUtils.delete(tmpFile);
		FileUtils.delete(tmpDir);

		return jarFile;
	}

	/**
	 * Writes an updated version of the feature XML file.
	 *
	 * @param targetDir
	 *            output directory
	 * @return the generated file
	 */
	public File writeUpdatedFeatureXml(File targetDir, String buildVersion, JarManager jarManager) throws Exception {
		Document document = data.readFeatureXml();
		Element rootElement = document.getDocumentElement();
		rootElement.setAttribute("version", buildVersion);

		final File targetFile = new File(targetDir, "feature.xml");
		FileUtils.writeXml(document, targetFile);
		return targetFile;
	}

}
