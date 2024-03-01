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
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.library.build.menu.BuildLibraryMenuDecoration;
import org.eclipse.tea.library.build.menu.OtherMenuDecoration;
import org.eclipse.tea.library.build.tasks.TaskBuildWorkspace;
import org.eclipse.tea.library.build.tasks.TaskCleanWorkspace;
import org.osgi.service.component.annotations.Component;

/**
 * Simple {@link TaskChain} to clean and build all projects in the workspace.
 */
@TaskChainMenuEntry(path = { OtherMenuDecoration.MENU_OTHER,
		BuildLibraryMenuDecoration.MENU_BUILD }, groupingId = BuildLibraryMenuDecoration.GROUP_BUILD, icon = BuildLibraryMenuDecoration.ICON_BUILD)
@TaskChainId(description = "Clean and Build all Projects", alias = "CleanBuildAllProjects")
@Component
public class CleanBuildAllProjects implements TaskChain {

	@TaskChainContextInit
	public void init(TaskExecutionContext c) {
		c.addTask(TaskCleanWorkspace.class);
		c.addTask(TaskBuildWorkspace.class);
	}

}
