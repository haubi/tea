/*******************************************************************************
 *  Copyright (c) 2018 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.library.build.tasks.p2;

import java.io.File;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.library.build.config.BuildDirectories;
import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.model.FeatureBuild;
import org.eclipse.tea.library.build.model.WorkspaceBuild;

/**
 * Generates properties required by augustin to download files.
 */
public class TaskGenPropertiesFiles {

	protected final String featureID;

	public TaskGenPropertiesFiles(String featureID) {
		this.featureID = featureID;
	}

	@Override
	public String toString() {
		return "Generate Properties (" + featureID + ')';
	}

	@Execute
	public void run(WorkspaceBuild wb, JarManager jarManager, BuildDirectories dirs) throws Exception {
		File propFile = new File(dirs.getSiteDirectory(), TaskPublishProductUpdateSite.PRODUCT_VERSIONS_PROPERTIES);
		FeatureBuild.generateProperty(propFile, jarManager, wb.getFeature(featureID));
	}

}