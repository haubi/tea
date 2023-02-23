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
package org.eclipse.tea.library.build.lcdsl.tasks;

import java.io.File;

import org.eclipse.core.runtime.Assert;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.library.build.config.BuildDirectories;
import org.eclipse.tea.library.build.util.FileUtils;

import com.wamas.ide.launching.lcDsl.LaunchConfig;
import com.wamas.ide.launching.ui.LcDslHelper;

/**
 * Task that can run any LcDsl configuration.
 */
public class TaskLcDslLaunch {

	private final String configName;
	private final boolean wait;

	public TaskLcDslLaunch(String configName) {
		this(configName, true);
	}

	public TaskLcDslLaunch(String configName, boolean wait) {
		this.configName = configName;
		this.wait = wait;
	}

	@Execute
	public void run(BuildDirectories dirs) throws Exception {
		LaunchConfig lc = LcDslHelper.getInstance().findLaunchConfig(configName);
		Assert.isNotNull(lc, "cannot find " + configName);

		File logFile = new File(dirs.getLogDirectory(), configName + ".log");
		FileUtils.delete(logFile);

		LcDslHelper.getInstance().launch(lc, LcDslHelper.MODE_RUN, false, wait, logFile);
	}

	protected LaunchConfig processConfig(LaunchConfig cfg) {
		return cfg; // default do nothing
	}

	@Override
	public String toString() {
		return "Launcher [" + configName + "]";
	}

}
