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
package org.eclipse.tea.ease.internal;

import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.annotations.TaskChainMenuEntry;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.core.ui.DevelopmentMenuDecoration;
import org.eclipse.tea.ease.TaskListEaseScripts;
import org.osgi.service.component.annotations.Component;

/**
 * Very simple task that shows a list of all registered EASE scripts.
 */
@TaskChainId(description = "List available EASE Scripts")
@TaskChainMenuEntry(path = "Development", development = true, icon = "platform:/plugin/org.eclipse.ease.ui/icons/eobj16/script.png", groupingId = DevelopmentMenuDecoration.DEV_GROUP_LISTS)
@Component
public class TaskChainListEaseScripts implements TaskChain {

	@TaskChainContextInit
	public void init(TaskExecutionContext c) {
		c.addTask(new TaskListEaseScripts());
	}

}
