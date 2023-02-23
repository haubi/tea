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
package org.eclipse.tea.library.build.config;

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.services.TaskingConfigurationExtension;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfig;
import org.eclipse.tea.core.ui.config.TaskingEclipsePreferenceStore;
import org.osgi.service.component.annotations.Component;

/**
 * Definition of directories used by various components of TEA builds. All
 * defined directories are relative to the workspace path.
 */
@TaskingConfig(description = "TEA Build Directories")
@Component
public class BuildDirectories implements TaskingConfigurationExtension {

	/** Directory to store arbitrary build output into (generated files, ...) */
	@TaskingConfigProperty(description = "Build Artifacts")
	public String buildDirOutput = "00_BUILD_OUTPUT";

	/** Directory to generate update sites */
	@TaskingConfigProperty(description = "Update Sites")
	public String buildDirSite = "01_BUILD_SITE";

	/** Directory to generate executable products */
	@TaskingConfigProperty(description = "Product Binaries")
	public String buildDirProduct = "02_BUILD_PRODUCT";

	@TaskingConfigProperty(description = "JUnit Results and Temporary Files")
	public String buildDirJunit = "03_JUNIT_RESULTS";

	/** Directory used as workspace root for processes launched */
	@TaskingConfigProperty(description = "Runtime Directories for launches")
	public String buildDirRuntime = "04_RUNTIME";

	/** Directory used as local maven repository cache */
	@TaskingConfigProperty(description = "Local Maven repository")
	public String buildDirMaven = "08_MAVEN";

	@TaskingConfigProperty(description = "General Purpose Cache directories")
	public String buildDirCache = "09_CACHE";

	public File getOutputDirectory() {
		return getAndCreateWorkspaceDir(buildDirOutput);
	}

	public File getLogDirectory() {
		return getAndCreateWorkspaceDir(buildDirOutput + "/log");
	}

	public File getSiteDirectory() {
		return getAndCreateWorkspaceDir(buildDirSite);
	}

	public File getRuntimeDirectory() {
		return getAndCreateWorkspaceDir(buildDirRuntime);
	}

	public File getJunitDirectory() {
		return getAndCreateWorkspaceDir(buildDirJunit);
	}

	public File getWorkspaceDirectory() {
		return ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
	}

	private File getAndCreateWorkspaceDir(String what) {
		File r = new File(getWorkspaceDirectory(), what);
		r.mkdirs();
		return r;
	}

	public File getProductDirectory() {
		return getAndCreateWorkspaceDir(buildDirProduct);
	}

	public File getMavenDirectory() {
		return getAndCreateWorkspaceDir(buildDirMaven);
	}

	public File getNewCacheDirectory(String cacheName) {
		return new File(getBaseCacheDirectory(), cacheName + "-" + Long.valueOf(System.currentTimeMillis()));
	}

	public File getBaseCacheDirectory() {
		return getAndCreateWorkspaceDir(buildDirCache);
	}

	/**
	 * Helper for legacy code still outside TEA that requires access to this
	 * information.
	 */
	public static BuildDirectories get() {
		IEclipseContext ctx = TaskingInjectionHelper.createConfiguredContext(new TaskingEclipsePreferenceStore());
		try {
			return ctx.get(BuildDirectories.class);
		} finally {
			ctx.dispose();
		}
	}

}
