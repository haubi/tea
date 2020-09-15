/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package org.eclipse.tea.ease.chains;

import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.ease.EaseScriptTask;
import org.eclipse.tea.ease.config.EaseScriptConfig;
import org.osgi.service.component.annotations.Component;

@TaskChainId(description = "Run the named EASE script", alias = "TaskChainRunEaseScript")
@Component
public class TaskChainRunEaseScript implements TaskChain {

	@TaskChainContextInit
	public void init(TaskExecutionContext c, EaseScriptConfig cfg) {
		c.addTask(new EaseScriptTask(cfg.easeScript));
	}

}
