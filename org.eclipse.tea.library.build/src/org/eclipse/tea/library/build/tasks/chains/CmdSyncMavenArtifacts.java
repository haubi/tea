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
import org.eclipse.tea.library.build.model.WorkspaceBuild;
import org.eclipse.tea.library.build.tasks.maven.SynchronizeMavenArtifact;
import org.osgi.service.component.annotations.Component;

@TaskChainId(description = "Synchronize Maven Artifacts")
@TaskChainMenuEntry(path = BuildLibraryMenu.MENU_BUILD, icon = "icons/jar_l_obj.png", groupingId = BuildLibraryMenu.GROUP_BUILD)
@Component
public class CmdSyncMavenArtifacts implements TaskChain {

	@TaskChainContextInit
	public void init(TaskExecutionContext c, WorkspaceBuild build) {
		c.addTask(SynchronizeMavenArtifact.class);
	}

}
