/*******************************************************************************
 *  Copyright (c) 2018 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.library.build.lcdsl.tasks.p2;

import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.library.build.model.PlatformTriple;
import org.eclipse.tea.library.build.tasks.jar.TaskRunFeaturePluginJarExport;
import org.eclipse.tea.library.build.tasks.p2.TaskGenPropertiesFiles;
import org.eclipse.tea.library.build.tasks.p2.TaskPublishProductUpdateSite;
import org.eclipse.tea.library.build.tasks.p2.TaskRunProductExport;

/**
 * Defines how to build a product and its update site. Every WAMAS 5 product
 * needs a separate sub-class.
 */
public abstract class AbstractProductBuild {

	private final String featureBundle;
	private final String productBundle;
	private final String productDefinition;
	private final boolean needProperties;

	/**
	 * Creates the product build.
	 *
	 * @param featureBundle
	 *            name of the feature bundle
	 * @param productBundle
	 *            name of the product bundle
	 * @param productDefinition
	 *            name of the product definition file
	 * @param needProperties
	 *            {@code true} if special properties should be updated (see
	 *            {@link TaskGenPropertiesFiles})
	 */
	protected AbstractProductBuild(String featureBundle, String productBundle, String productDefinition,
			boolean needProperties) {
		this.featureBundle = featureBundle;
		this.productBundle = productBundle;
		this.productDefinition = productDefinition;
		this.needProperties = needProperties;
	}

	public TaskGenFeatureFromLcDsl createTaskGenFeatureXml(boolean errorIfNotExist) {
		return new TaskGenFeatureFromLcDsl(featureBundle, errorIfNotExist);
	}

	protected TaskPublishProductUpdateSite createTaskPublishProductUpdateSite(String siteName) {
		return new TaskPublishProductUpdateSite(siteName, productBundle, productDefinition, true);
	}

	public void addUpdateSiteTasks(TaskExecutionContext c, String[] updateSites) {
		c.addTask(createTaskGenFeatureXml(true));
		if (needProperties) {
			c.addTask(new TaskGenPropertiesFiles(featureBundle));
		}
		c.addTask(new TaskRunFeaturePluginJarExport(featureBundle));
		for (String siteName : updateSites) {
			c.addTask(createTaskPublishProductUpdateSite(siteName));
		}
	}

	public void addProductTasks(TaskExecutionContext c, String updateSite) {
		addProductTasks(c, updateSite, true);
	}

	public TaskRunProductExport addProductTasks(TaskExecutionContext c, String updateSite, boolean zip) {
		final TaskRunProductExport task = createProductExportTask(updateSite, zip);
		c.addTask(task);
		return task;
	}

	public TaskRunProductExport createProductExportTask(String updateSite, boolean zip) {
		final TaskRunProductExport task = new TaskRunProductExport(updateSite, productBundle, productDefinition, zip);
		task.setPlatformsToBuild(getPlatformsToBuild());
		return task;
	}

	public PlatformTriple[] getPlatformsToBuild() {
		return new PlatformTriple[] { PlatformTriple.WIN64, PlatformTriple.LINUX64 };
	}

	/**
	 * Returns the short name of this product build definition.
	 */
	public String getName() {
		String name = getClass().getSimpleName();
		if (name.isEmpty()) {
			// we have an inner class
			name = getClass().getName();
		}
		return name;
	}

	public String getFeatureBundleName() {
		return featureBundle;
	}

	public String getProductBundleName() {
		return productBundle;
	}

	public String getOfficialName() {
		return getClass().getSimpleName();
	}

	public String getDescription() {
		return getOfficialName();
	}

}
