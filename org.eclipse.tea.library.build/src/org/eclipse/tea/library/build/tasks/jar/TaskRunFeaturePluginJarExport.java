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
package org.eclipse.tea.library.build.tasks.jar;

import java.io.File;
import java.util.Collection;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.config.BuildDirectories;
import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.jar.ZipConfig;
import org.eclipse.tea.library.build.model.FeatureBuild;
import org.eclipse.tea.library.build.model.PluginBuild;
import org.eclipse.tea.library.build.model.WorkspaceBuild;
import org.eclipse.tea.library.build.util.FileUtils;

/**
 * Creates JAR files for all plug-ins of the given feature and for the feature
 * itself
 */
public class TaskRunFeaturePluginJarExport extends TaskRunJarExport {

	private static final String FEATURE_DIRECTORY = "features";

	protected final String featureName;

	private final boolean composite;

	public TaskRunFeaturePluginJarExport(String featureName) {
		this(featureName, false);
	}

	public TaskRunFeaturePluginJarExport(String featureName, boolean composite) {
		super(composite);
		this.featureName = featureName;
		this.composite = composite;
	}

	@Override
	public String toString() {
		return super.toString() + " (" + featureName + ')';
	}

	@Override
	protected Collection<PluginBuild> getPlugins(WorkspaceBuild wb) {
		FeatureBuild feature = wb.getFeature(featureName);
		if (feature == null) {
			throw new IllegalStateException("Cannot find feature " + featureName);
		}
		return feature.getIncludedPlugins();
	}

	@Override
	@Execute
	public void run(TaskingLog log, WorkspaceBuild wb, JarManager jarManager, BuildDirectories dirs,
			TaskProgressTracker tracker, ZipConfig config) throws Exception {
		// create jars for all plug-ins
		super.run(log, wb, jarManager, dirs, tracker, config);

		// reset destination directory
		final File distFeatureDirectory = new File(dirs.getOutputDirectory(), FEATURE_DIRECTORY);
		if (!composite) {
			FileUtils.deleteDirectory(distFeatureDirectory);
		}
		FileUtils.mkdirs(distFeatureDirectory);

		// create jar for the feature itself
		log.info("Features directory: " + distFeatureDirectory);
		FeatureBuild feature = wb.getFeature(featureName);
		log.info("execJarCommand: " + feature.getFeatureName());
		execJarCached(jarManager, distFeatureDirectory, feature);
	}

	/**
	 * Returns the shared directory where the created jar files of the plug-ins
	 * are located.
	 *
	 * @return the path to the directory
	 */
	public static String getFeatureJarDirectory() {
		return FEATURE_DIRECTORY;
	}

}
