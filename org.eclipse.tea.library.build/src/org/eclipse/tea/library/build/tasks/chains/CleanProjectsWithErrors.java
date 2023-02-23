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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.annotations.TaskChainMenuEntry;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.menu.BuildLibraryMenu;
import org.osgi.service.component.annotations.Component;

@TaskChainId(description = "Clean Projects with Errors")
@TaskChainMenuEntry(path = BuildLibraryMenu.MENU_BUILD, icon = BuildLibraryMenu.ICON_BUILD, groupingId = BuildLibraryMenu.GROUP_MISC)
@Component
public class CleanProjectsWithErrors implements TaskChain {

	@TaskChainContextInit
	public void init(TaskExecutionContext c) {
		c.addTask(new Object() {

			@Execute
			public void doIt(TaskingLog log, TaskProgressTracker tracker) {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IProject projects[] = workspace.getRoot().getProjects();

				log.info("Checking " + projects.length + " projects...");

				/**
				 * Loop over all projects, clean them if necessary.
				 */
				for (IProject project : projects) {
					try {
						int severity = project.findMaxProblemSeverity(null, true, IResource.DEPTH_INFINITE);
						if (severity >= IMarker.SEVERITY_ERROR) {
							log.warn("CLEAN " + project.getName());
							project.build(IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor());
						}

					} catch (Exception e) {
					}

					if (tracker.isCanceled()) {
						break;
					}

				}

				log.info("ready");
			}

			@Override
			public String toString() {
				return "Clean Projects with Errors";
			}
		});
	}

}
