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
package org.eclipse.tea.library.build.tasks.chains;

import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.annotations.TaskChainMenuEntry;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.library.build.menu.BuildLibraryMenuDecoration;
import org.eclipse.tea.library.build.menu.OtherMenuDecoration;
import org.eclipse.tea.library.build.tasks.TaskCleanProjectsWithErrors;
import org.osgi.service.component.annotations.Component;

@TaskChainMenuEntry(path = { OtherMenuDecoration.MENU_OTHER,
		BuildLibraryMenuDecoration.MENU_BUILD }, groupingId = BuildLibraryMenuDecoration.GROUP_MISC, icon = BuildLibraryMenuDecoration.ICON_BUILD)
@Component
public class TaskChainCleanProjectsWithErrors implements TaskChain {

	@TaskChainContextInit
	public void init(TaskExecutionContext c) {
		c.addTask(new TaskCleanProjectsWithErrors());
	}

}
