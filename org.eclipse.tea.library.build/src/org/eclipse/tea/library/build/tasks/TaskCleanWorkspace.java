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

import javax.inject.Named;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.library.build.model.WorkspaceData;

/**
 * Cleans and refreshes all {@link IProject}s in the workspace.
 */
@Named("Clean and refresh all projects") // Progress Monitor shows & as _
public class TaskCleanWorkspace {

	@Execute
	public void run(TaskProgressTracker tracker) throws Exception {
		// refresh all projects
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			tracker.setTaskName(project.getName());
			fullCleanAndRefresh(project);

			tracker.worked(1);
		}
	}

	/**
	 * Cleans a project, removes ALL problem markers from it and refreshes all
	 * resources.
	 */
	public static void fullCleanAndRefresh(IProject project) throws CoreException {
		if (WorkspaceData.isBinaryProject(project)) {
			return;
		}

		project.build(IncrementalProjectBuilder.CLEAN_BUILD, null);

		if (project.exists() && project.isOpen()) {
			project.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		}
	}

}
