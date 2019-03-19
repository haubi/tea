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

import java.io.File;
import java.util.Properties;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.library.build.config.BuildDirectories;
import org.eclipse.tea.library.build.lcdsl.tasks.p2.SimpleProductBuild.SimpleProductBuildDescription;
import org.eclipse.tea.library.build.model.FeatureBuild;
import org.eclipse.tea.library.build.model.WorkspaceBuild;
import org.eclipse.tea.library.build.util.FileUtils;

import com.google.common.base.Splitter;

/**
 * Defines all products defined by product.properties files in feature projects
 * in the workspace.
 */
@Creatable
public class DynamicProductBuildRegistry extends ProductBuildRegistry {

	@Inject
	public DynamicProductBuildRegistry(WorkspaceBuild wb) {
		// dynamic products
		addDynamicProducts(wb);
	}

	private void addDynamicProducts(WorkspaceBuild wb) {
		for (FeatureBuild fb : wb.getSourceFeatures()) {
			File root = fb.getData().getBundleDir();
			File props = new File(root, "product.properties");
			if (!props.exists()) {
				// compat for old naming scheme.
				props = new File(root, "wpob.properties");
			}
			if (props.exists()) {
				Properties properties = FileUtils.readProperties(props);
				if (properties.containsKey("updateSites")) {
					// it is a dynamic feature
					add(new SimpleProductBuild(new SimpleProductBuildDescription(fb, properties)), Splitter.on(',')
							.trimResults().omitEmptyStrings().splitToList(properties.getProperty("updateSites")));
				}
			}
		}
	}

	public static TaskChain getExportProductsForSiteChain(String siteName, boolean zipSites) {
		return new TaskChain() {

			@TaskChainContextInit
			public void init(TaskExecutionContext c, BuildDirectories dirs, DynamicProductBuildRegistry reg) {
				reg.addAllUpdateSiteTasks(c, dirs, siteName, zipSites);
				reg.addAllProductTasks(c, siteName);
			}

			@Override
			public String toString() {
				if (siteName == null) {
					return "Export all products";
				} else {
					return "Export products for " + siteName;
				}
			}
		};
	}
}
