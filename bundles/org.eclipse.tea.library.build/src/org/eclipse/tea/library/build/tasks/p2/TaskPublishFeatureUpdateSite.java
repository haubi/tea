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
package org.eclipse.tea.library.build.tasks.p2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.internal.p2.updatesite.CategoryPublisherApplication;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAndBundlesPublisherApplication;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.config.BuildDirectories;
import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.model.PlatformTriple;
import org.eclipse.tea.library.build.model.WorkspaceBuild;
import org.eclipse.tea.library.build.model.WorkspaceData;
import org.eclipse.tea.library.build.p2.UpdateSite;
import org.eclipse.tea.library.build.p2.UpdateSiteCategory;
import org.eclipse.tea.library.build.tasks.jar.TaskRunFeaturePluginJarExport;
import org.eclipse.tea.library.build.util.FileUtils;

/**
 * Generates a update site from given feature.
 */
@SuppressWarnings("restriction")
public class TaskPublishFeatureUpdateSite {

	private final String siteName;
	private final File distDirectory;
	private final List<String> includedFeatures;
	private File siteZip;

	public TaskPublishFeatureUpdateSite(String featureID) {
		this(featureID, Collections.singletonList(featureID));
	}

	public TaskPublishFeatureUpdateSite(String siteName, List<String> includedFeatures) {
		this(siteName, new File(BuildDirectories.get().getSiteDirectory(), siteName), includedFeatures);
	}

	public TaskPublishFeatureUpdateSite(String featureID, File distDir) {
		this(featureID, distDir, Collections.singletonList(featureID));
	}

	public TaskPublishFeatureUpdateSite(String siteName, File distDir, List<String> includedFeatures) {
		this.siteName = siteName;
		this.distDirectory = distDir;
		this.includedFeatures = includedFeatures;
	}

	@Override
	public String toString() {
		return "Publish Update Site (" + siteName + ')';
	}

	@Execute
	public void run(TaskingLog log, WorkspaceBuild wb, JarManager jarManager, BuildDirectories dirs) throws Exception {
		log.info("generate update site for platform:" + PlatformTriple.getAllTargetsCommandLineStyle());

		// these directories are prepared by the TaskRunFeaturePluginJarExport
		final File featureDir = new File(BuildDirectories.get().getOutputDirectory(),
				TaskRunFeaturePluginJarExport.getFeatureJarDirectory());
		final File pluginDir = new File(BuildDirectories.get().getOutputDirectory(),
				TaskRunFeaturePluginJarExport.getPluginJarDirectory());

		// create destination directory
		FileUtils.mkdirs(distDirectory);

		log.info("update site destination directory: " + distDirectory);

		if (!IApplication.EXIT_OK.equals(runUpdateSitePublisher(distDirectory, featureDir, pluginDir))) {
			throw new RuntimeException("exit status of application not ok!");
		}

		// publish category if required.
		createCategory(log, jarManager, wb, distDirectory);

		// create zip file for further use
		createSiteZip(log, jarManager, wb, dirs);
	}

	/**
	 * @return the path to the update site zip file that was created. only valid
	 *         after the task has run successfully.
	 */
	public File getSiteZip() {
		return siteZip;
	}

	public static Object runUpdateSitePublisher(File targetDirectory, File featureDir, File pluginDir)
			throws Exception {
		// arguments for FeaturesAndBundlesPublisherApplication
		Collection<String> cmdArgs = new ArrayList<>();
		cmdArgs.add("-metadataRepository");
		cmdArgs.add("file:" + targetDirectory.getAbsolutePath());
		cmdArgs.add("-artifactRepository");
		cmdArgs.add("file:" + targetDirectory.getAbsolutePath());
		cmdArgs.add("-bundles");
		cmdArgs.add(pluginDir.getAbsolutePath());
		cmdArgs.add("-features");
		cmdArgs.add(featureDir.getAbsolutePath());
		cmdArgs.add("-configs");
		cmdArgs.add(PlatformTriple.getAllTargetsCommandLineStyle());
		cmdArgs.add("-compress");
		cmdArgs.add("-publishArtifacts");

		FeaturesAndBundlesPublisherApplication updateSiteGenerator = new FeaturesAndBundlesPublisherApplication();
		return updateSiteGenerator.run(cmdArgs.toArray(new String[cmdArgs.size()]));
	}

	public static Object runCategoryPublisher(File targetDirectory, File categoryFile) throws Exception {
		// arguments for the CategoryPublisherApplication
		Collection<String> cmdArgs = new ArrayList<>();
		cmdArgs.add("-metadataRepository");
		cmdArgs.add("file:" + targetDirectory.getAbsolutePath());
		cmdArgs.add("-categoryDefinition");
		cmdArgs.add("file:" + categoryFile);
		cmdArgs.add("-compress");

		CategoryPublisherApplication categoryPublisher = new CategoryPublisherApplication();
		return categoryPublisher.run(cmdArgs.toArray(new String[cmdArgs.size()]));
	}

	private void createCategory(TaskingLog log, JarManager jarManager, WorkspaceBuild wb, File distDirectory) {
		Map<String, String> featureToCategory = new TreeMap<>();
		for (String feature : includedFeatures) {
			IProject featureProject = WorkspaceData.getProject(feature);
			if (featureProject == null) {
				throw new IllegalStateException("cannot find " + feature);
			}

			// get dir of content.properties file
			File featureDir = featureProject.getLocation().toFile();

			String categoryName = "Default";

			// load properties and get Category name
			Properties props = new Properties();
			File propFile = new File(featureDir, "content.properties");
			if (propFile.exists()) {
				try {
					FileInputStream inStream = new FileInputStream(propFile);
					props.load(inStream);
					inStream.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				categoryName = props.getProperty("category", "Default");
			}

			featureToCategory.put(feature, categoryName);
		}

		File dirName = BuildDirectories.get().getOutputDirectory();
		File categoryFile = new File(dirName, "category.xml");

		try {
			// generate XML file containing the category information
			UpdateSiteCategory.generateCategoryXml(categoryFile, featureToCategory, wb, jarManager);

			log.info("read category information from " + categoryFile.getAbsolutePath());

			if (!IApplication.EXIT_OK.equals(runCategoryPublisher(distDirectory, categoryFile))) {
				throw new RuntimeException("exit status of category publisher not ok!");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// remove category.xml
		FileUtils.delete(categoryFile);
	}

	private IStatus createSiteZip(TaskingLog log, JarManager jarManager, WorkspaceBuild wb, BuildDirectories dirs) {
		try {
			File dir = new File(dirs.getSiteDirectory(), siteName);

			String buildVersion = jarManager.getBuildVersion();
			siteZip = new File(dirs.getSiteDirectory(), siteName + "-" + buildVersion + ".zip");

			UpdateSite.createUpdateSiteZip(dir, siteZip, jarManager.getZipExecFactory(), log);
		} catch (Exception e) {
			return new Status(Status.ERROR, getClass().getName(), "failed to zip update site", e);
		}
		return Status.OK_STATUS;
	}
}
