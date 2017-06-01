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
package org.eclipse.tea.samples.taskchain;

import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.annotations.TaskChainMenuEntry;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.samples.taskchain.DemoTaskInheritance.InnerInheritance;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

@TaskChainId(description = "Do Something Useful", alias = { "Sample", "Test" })
@TaskChainMenuEntry(path = "Samples", icon = "resources/sample.gif")
@Component(property = { Constants.SERVICE_RANKING + "=900" })
public class SampleTaskChain implements TaskChain {

	@TaskChainContextInit
	public void initContext(TaskExecutionContext context) {
		// context.addTask(new EaseScriptTask("/scripts/test-python.py"));
		context.addTask(DemoTask.class);
		context.addTask(InnerInheritance.class);
	}

}
