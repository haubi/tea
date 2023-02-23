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
package org.eclipse.tea.samples.taskchain;

import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.annotations.TaskChainMenuEntry;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.samples.menu.SampleMenuDecoration;
import org.eclipse.tea.samples.tasks.SampleSimpleTask;
import org.osgi.service.component.annotations.Component;

@TaskChainId(description = "Sample Task Chain", alias = { "Sample", "Test" })
@TaskChainMenuEntry(path = SampleMenuDecoration.SAMPLE_MENU, icon = "resources/sample.gif")
@Component
public class SampleTaskChain implements TaskChain {

	@TaskChainContextInit
	public void initContext(TaskExecutionContext context) {
		// context.addTask(new EaseScriptTask("/scripts/test-python.py"));
		context.addTask(SampleSimpleTask.class);
	}

}
