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
package org.eclipse.tea.library.build.tasks;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskingLog;

public class TaskCleanProjectsWithErrors {

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

}
