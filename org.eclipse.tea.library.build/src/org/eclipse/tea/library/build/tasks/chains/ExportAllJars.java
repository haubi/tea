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
package org.eclipse.tea.library.build.tasks.chains;

import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.annotations.TaskChainMenuEntry;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.library.build.menu.BuildLibraryMenu;
import org.eclipse.tea.library.build.tasks.jar.TaskRunJarExport;
import org.osgi.service.component.annotations.Component;

@TaskChainId(description = "Export All JARs", alias = "ExportAllJars")
@TaskChainMenuEntry(path = BuildLibraryMenu.MENU_BUILD, icon = "icons/jar_obj.png", groupingId = BuildLibraryMenu.GROUP_JAR)
@Component
public class ExportAllJars implements TaskChain {

	@TaskChainContextInit
	public void init(TaskExecutionContext context) {
		context.addTask(TaskRunJarExport.class);
	}

}
