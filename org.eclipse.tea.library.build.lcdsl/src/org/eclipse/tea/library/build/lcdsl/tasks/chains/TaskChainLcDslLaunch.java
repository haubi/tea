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
package org.eclipse.tea.library.build.lcdsl.tasks.chains;

import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.library.build.lcdsl.tasks.TaskLcDslLaunch;
import org.eclipse.tea.library.build.lcdsl.tasks.config.LcDslLaunchConfig;
import org.osgi.service.component.annotations.Component;

@TaskChainId(description = "Run the named launch configuration", alias = "TaskChainLcDslLaunch")
@Component
public class TaskChainLcDslLaunch implements TaskChain {

	@TaskChainContextInit
	public void init(TaskExecutionContext c, LcDslLaunchConfig cfg) {
		c.addTask(new TaskLcDslLaunch(cfg.launchConfig, true));
	}

}
