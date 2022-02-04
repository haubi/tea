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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.internal.events.BuildManager;
import org.eclipse.core.internal.events.InternalBuilder;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.Job;
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
@SuppressWarnings("restriction")
public class AutoBuildDeactivator implements TaskingLifeCycleListener {

	private boolean autoBuildOriginalState = false;
	private static AtomicInteger nestCount = new AtomicInteger(0);
	private static final Map<IProject, ElementTree> suppressedProjects = new HashMap<>();

	public Workspace getWorkspace() {
		return ((Workspace) ResourcesPlugin.getWorkspace());
	}

	@BeginTaskChain
	public synchronized void begin(TaskingLog log) throws CoreException {
		if (nestCount.getAndIncrement() == 0) {
			log.debug("Disabling automatic build...");
			autoBuildOriginalState = setAutoBuild(log, false, false);
			cancelAutoBuild();
		}
	}

	private static void cancelAutoBuild() {
		BuildManager mgr = ((Workspace) ResourcesPlugin.getWorkspace()).getBuildManager();
		mgr.shutdown(null);
	}

	@FinishTaskChain
	public synchronized void finish(TaskExecutionContext context, TaskingLog log, MultiStatus status)
			throws CoreException {
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
						"The Eclipse automatic build is disabled. Since the build was successful,"
								+ " enabling the automatic build is recommended."
								+ " Should the automatic build be enabled now?");
				setAutoBuild(log, autoBuild, true); // suppress in headed mode
			});
		} else {
			setAutoBuild(log, autoBuildOriginalState, !TaskingInjectionHelper.isHeadless(context.getContext()));
		}
	}

	/**
	 * Allows to suppress a build of the specified projects. This has two
	 * effects. After finishing the currently running task chain, auto build
	 * will be enabled and forced by Eclipse - at this point, auto build is
	 * immediately cancelled and the force flag is reset. After that, the "last
	 * built" state of each of the given projects is updated with the current
	 * state.
	 *
	 * @param project
	 *            the project to assume to be cleanly built.
	 */
	public static void avoidBuild(IProject project) {
		synchronized (suppressedProjects) {
			ElementTree currentTree = null;
			try {
				IBuildConfiguration bc = project.getActiveBuildConfig();
				int highestStamp = 0;
				for (ICommand c : ((Project) project).internalGetDescription().getBuildSpec(false)) {
					IncrementalProjectBuilder builder = ((BuildCommand) c).getBuilder(bc);

					Method getTree = InternalBuilder.class.getDeclaredMethod("getLastBuiltTree");
					getTree.setAccessible(true);
					ElementTree t = (ElementTree) getTree.invoke(builder);
					getTree.setAccessible(false);

					if (t != null) {
						Field stampField = ElementTree.class.getDeclaredField("treeStamp");
						stampField.setAccessible(true);
						int stamp = (int) stampField.get(t);
						stampField.setAccessible(false);

						if (stamp > highestStamp) {
							highestStamp = stamp;
							currentTree = t;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				// oups;
			}

			if (currentTree != null) {
				suppressedProjects.put(project, currentTree);
			} else {
				System.err.println("no tree for " + project);
			}
		}
	}

	private boolean setAutoBuild(TaskingLog log, boolean autoBuild, boolean suppressBuild) {
		boolean originalState = false;
		synchronized (suppressedProjects) {
			try {
				final IWorkspace workspace = ResourcesPlugin.getWorkspace();
				final IWorkspaceDescription workspaceDesc = workspace.getDescription();
				originalState = workspaceDesc.isAutoBuilding();
				workspaceDesc.setAutoBuilding(autoBuild);
				workspace.setDescription(workspaceDesc);

				// we have 100ms worst case to react (scheduling delay of auto
				// build).
				if (!suppressedProjects.isEmpty() && suppressBuild) {
					suppressBuild(suppressedProjects);
				}
			} catch (Exception e) {
				log.error("Failed to restore AutoBuild, target=" + autoBuild, e);
			} finally {
				suppressedProjects.clear();
			}
		}
		return originalState;
	}

	/**
	 * Immediately cancels auto build, and suppresses any forced build and all
	 * deltas for the given projects.
	 *
	 * @param suppressedProjects
	 */
	private static void suppressBuild(Map<IProject, ElementTree> suppressedProjects) {
		// This is a little hacky but avoids that the autobuild will immediately
		// re-build what we built just now.
		try {
			BuildManager mgr = ((Workspace) ResourcesPlugin.getWorkspace()).getBuildManager();

			// get the job
			Field jobField = mgr.getClass().getDeclaredField("autoBuildJob");
			jobField.setAccessible(true);
			Object o = jobField.get(mgr);
			jobField.setAccessible(false);

			// cancel the job
			cancelAutoBuild();

			// reset force flag
			Field force = o.getClass().getDeclaredField("forceBuild");
			force.setAccessible(true);
			force.set(o, false);
			force.setAccessible(false);

			// now we need re-set the current delta trees on all projects
			if (!suppressedProjects.isEmpty()) {
				for (Entry<IProject, ElementTree> entry : suppressedProjects.entrySet()) {
					IProject project = entry.getKey();
					IBuildConfiguration bc = project.getActiveBuildConfig();
					ElementTree elementTree = suppressedProjects.get(project);
					if (elementTree != null) {
						ICommand[] commands;
						// yay - now make sure the tree of all builders is the
						// current tree of the project
						if (project.isAccessible()) {
							commands = ((Project) project).internalGetDescription().getBuildSpec(false);
						} else {
							continue;
						}

						for (ICommand c : commands) {
							IncrementalProjectBuilder b = ((BuildCommand) c).getBuilder(bc);
							Method setTree = InternalBuilder.class.getDeclaredMethod("setLastBuiltTree",
									ElementTree.class);
							setTree.setAccessible(true);
							setTree.invoke(b, elementTree);
							setTree.setAccessible(false);
						}
					}
				}
			}

			// Now schedule the job
			((Job) o).schedule();
		} catch (Exception e) {
			System.err.println("cannot avoid autobuild");
			e.printStackTrace();
		}
	}

}
