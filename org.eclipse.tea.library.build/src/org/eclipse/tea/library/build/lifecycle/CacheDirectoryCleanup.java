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
package org.eclipse.tea.library.build.lifecycle;

import java.io.File;

import org.eclipse.tea.core.annotations.lifecycle.BeginTaskChain;
import org.eclipse.tea.core.services.TaskingLifeCycleListener;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.config.BuildDirectories;
import org.eclipse.tea.library.build.util.FileUtils;
import org.osgi.service.component.annotations.Component;

/**
 * Clean up any left behind caches from previous task chains. Just in case the
 * previous chain failed and did not clean up properly.
 */
@Component
public class CacheDirectoryCleanup implements TaskingLifeCycleListener {

	@BeginTaskChain
	public void cleanCacheDir(TaskingLog log, BuildDirectories dirs) {
		File directory = dirs.getBaseCacheDirectory();
		try {
			FileUtils.deleteDirectory(directory);
		} catch (Exception e) {
			log.warn("cannot remove: " + directory);
		}
	}

}
