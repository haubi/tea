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
package org.eclipse.tea.library.build.lcdsl.tasks.p2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.model.FeatureBuild;
import org.eclipse.tea.library.build.model.PluginBuild;
import org.eclipse.tea.library.build.model.WorkspaceBuild;
import org.eclipse.tea.library.build.model.WorkspaceData;

import com.wamas.ide.launching.generator.DependencyResolver;
import com.wamas.ide.launching.lcDsl.LaunchConfig;
import com.wamas.ide.launching.ui.LcDslHelper;

/**
 * This class is responsible for creating a feature.xml file.
 * <p>
 * The feature folder should contain a 'content.properties' file that references
 * one or more LcDsl launch configuration names in the 'dependencies' attribute
 * (comma separated). All plugins required by the referenced launch
 * configurations will be included in the feature.xml.
 */
public class TaskGenFeatureFromLcDsl {

	protected final String featureID;
	protected final boolean errorIfNotExist;

	public TaskGenFeatureFromLcDsl(String featureID, boolean errorIfNotExist) {
		this.featureID = featureID;
		this.errorIfNotExist = errorIfNotExist;
	}

	@Override
	public String toString() {
		return "Generate Feature XML (" + featureID + ')';
	}

	/**
	 * Determine the needed plug-ins from a list of launch config ini files and
	 * write down the xml file.
	 *
	 * @param log
	 *            the log destination
	 * @param wb
	 *            provides workspace information
	 * @throws Exception
	 *             if no project for the configured featureID could be found,
	 *             the wbop.properties file could not be found or contains the
	 *             wrong properties, the configured ini files could not be found
	 *             or an error occurred while generating the feature xml file.
	 */
	@Execute
	public void generateFeatureXml(TaskingLog log, WorkspaceBuild wb, JarManager jm) throws Exception {
		IProject wpobPropertiesProject = WorkspaceData.getProject(featureID);

		if (wpobPropertiesProject == null) {
			if (!errorIfNotExist) {
				return;
			}
			throw new IllegalStateException("cannot find " + featureID);
		}

		Set<PluginBuild> allPlugins = getPluginsForFeature(log, wpobPropertiesProject, wb, errorIfNotExist);

		// write feature XML
		FeatureBuild targetFeature = wb.getFeature(featureID);
		targetFeature.generateFeatureXml(jm, allPlugins, Collections.emptyList());

		// REFRESH the project so that others see the change
		wpobPropertiesProject.refreshLocal(IProject.DEPTH_INFINITE, null);
	}

	public static Set<PluginBuild> getPluginsForFeature(TaskingLog log, IProject featurePrj, WorkspaceBuild wb,
			boolean errorIfNotExists) throws IOException {
		File featureDir = featurePrj.getLocation().toFile();

		// load properties
		Properties props = new Properties();
		File propFile = new File(featureDir, "content.properties");
		if (!propFile.exists()) {
			throw new RuntimeException(propFile + " doesn't exist.");
		}
		FileInputStream inStream = new FileInputStream(propFile);
		props.load(inStream);
		inStream.close();

		// read properties
		final String dependencies = props.getProperty("dependencies");
		if (dependencies == null) {
			throw new RuntimeException("Please specify at least one ini file for feature \"" + featurePrj.getName()
					+ "\" in your property file.");
		}

		// find launch configurations and gather all dependencies
		Set<PluginBuild> allPlugins = new TreeSet<>();
		for (String lcName : dependencies.split(",")) {
			LaunchConfig lc = LcDslHelper.getInstance().findLaunchConfig(lcName);
			if (lc == null) {
				log.info("cannot find launch configuration " + lcName);
				return Collections.emptySet();
			}

			for (Map.Entry<BundleDescription, ?> entry : DependencyResolver.findDependencies(lc, true).entrySet()) {
				PluginBuild sourcePlugin = wb.getSourcePlugin(entry.getKey().getSymbolicName());
				if (sourcePlugin != null) {
					allPlugins.add(sourcePlugin);
				}
			}
		}

		return allPlugins;
	}

}