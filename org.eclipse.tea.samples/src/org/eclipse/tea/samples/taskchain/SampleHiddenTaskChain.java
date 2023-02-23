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
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.samples.tasks.SampleSimpleTask;
import org.osgi.service.component.annotations.Component;

@Component
public class SampleHiddenTaskChain implements TaskChain {

	@TaskChainContextInit
	public void init(TaskExecutionContext context) {
		context.addTask(SampleSimpleTask.class);
	}

}
