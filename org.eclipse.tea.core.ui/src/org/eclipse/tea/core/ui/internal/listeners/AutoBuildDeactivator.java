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
package org.eclipse.tea.core.ui.internal.listeners;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.annotations.lifecycle.BeginTaskChain;
import org.eclipse.tea.core.annotations.lifecycle.FinishTaskChain;
import org.eclipse.tea.core.services.TaskingLifeCycleListener;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.component.annotations.Component;

/**
 * Automatically deactivates and re-activates auto-build in the workspace while
 * task chains are running.
 */
@Component
public class AutoBuildDeactivator implements TaskingLifeCycleListener {

	private boolean autoBuildOriginalState = false;
	private static AtomicInteger nestCount = new AtomicInteger(0);

	@BeginTaskChain
	public synchronized void begin(TaskingLog log) {
		if (nestCount.getAndIncrement() == 0) {
			log.debug("Disabling automatic build...");
			autoBuildOriginalState = setAutoBuild(log, false);
		}
	}

	@FinishTaskChain
	public synchronized void finish(TaskExecutionContext context, TaskingLog log, MultiStatus status) {
		if (nestCount.decrementAndGet() != 0) {
			return;
		}

		// restore auto build to its original setting, ask for enabling if OK,
		// previous disabled and run in the IDE.
		if (!TaskingInjectionHelper.isHeadless(context.getContext()) && !autoBuildOriginalState
				&& status.getSeverity() < IStatus.ERROR) {
			// ask for restore auto build
			Display.getDefault().asyncExec(() -> {
				boolean autoBuild = MessageDialog.openQuestion(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Automatic Build",
						"The Eclipse automatic build is disabled. Since the WPoB build was successful,"
								+ " enabling the automatic build is recommended."
								+ " Should the automatic build be enabled now?");
				setAutoBuild(log, autoBuild);
			});
		} else {
			setAutoBuild(log, autoBuildOriginalState);
		}
	}

	private boolean setAutoBuild(TaskingLog log, boolean autoBuild) {
		boolean originalState = false;
		try {
			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			final IWorkspaceDescription workspaceDesc = workspace.getDescription();
			originalState = workspaceDesc.isAutoBuilding();
			workspaceDesc.setAutoBuilding(autoBuild);
			workspace.setDescription(workspaceDesc);
		} catch (Exception e) {
			log.error("Failed to restore AutoBuild, target=" + autoBuild, e);
		}
		return originalState;
	}

}
