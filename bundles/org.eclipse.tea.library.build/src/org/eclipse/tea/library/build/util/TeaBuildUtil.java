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
package org.eclipse.tea.library.build.util;

import java.util.Collection;

import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.chain.TeaBuildProjectElement;
import org.eclipse.tea.library.build.config.TeaBuildConfig;
import org.eclipse.tea.library.build.internal.Activator;
import org.eclipse.tea.library.build.model.WorkspaceData;

/**
 * Utility method collection for the build library.
 */
public class TeaBuildUtil {

	private static final MultiStatus DEF_STATUS = new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR, "", null);

	public static void tryCompile(Collection<IProject> projects) {
		// Step 1: find all active configurations for all projects
		IBuildConfiguration[] toBuild = projects.stream().map(TeaBuildUtil::getActiveConfigSafe)
				.toArray(IBuildConfiguration[]::new);

		// Step 2: tell the workspace to build
		try {
			ResourcesPlugin.getWorkspace().build(toBuild, IncrementalProjectBuilder.FULL_BUILD, false, null);
		} catch (CoreException e) {
			// internal error
			throw new RuntimeException(e);
		}
	}

	public static IBuildConfiguration getActiveConfigSafe(IProject project) {
		try {
			return project.getActiveBuildConfig();
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Compile a single {@link TeaBuildProjectElement}'s {@link IProject}.
	 * Retries according to {@link TeaBuildConfig}.
	 *
	 * @param log
	 *            log output destination
	 * @param tracker
	 *            {@link TaskProgressTracker} to provide cancellation support
	 * @param element
	 *            {@link TeaBuildProjectElement} to compile
	 * @param config
	 *            {@link TeaBuildConfig} determining retry behavior
	 * @return the resulting {@link IStatus} of the operation.
	 */
	public static IStatus tryCompile(TaskingLog log, TaskProgressTracker tracker, TeaBuildProjectElement element,
			TeaBuildConfig config) {
		IProject project = element.getProject();

		boolean isBinary = WorkspaceData.isBinaryProject(project);
		String logTxt = (isBinary ? "    binProject" : "compileProject") + ": " + element.getName();
		Exception compilerException = null;
		MultiStatus status = DEF_STATUS;

		/*
		 * Compiling a project may need several attempts since the Eclipse
		 * builder is a little stupid.
		 */
		for (int pass = 0; pass < config.compileRetries; ++pass) {
			if (tracker.isCanceled()) {
				throw new OperationCanceledException();
			}
			try {
				if (pass == 0) {
					log.info(logTxt);

					// first try: the conventional way
					project.build(IncrementalProjectBuilder.FULL_BUILD, null);
				} else {
					if (isBinary) {
						break; // never recompile
					}

					log.info(logTxt + ": recompile " + Integer.toString(pass));

					// subsequent attempts: cleanup a little first
					project.close(null);
					project.open(null);
					project.refreshLocal(IResource.DEPTH_INFINITE, null);

					project.build(IncrementalProjectBuilder.CLEAN_BUILD, null);
					project.build(IncrementalProjectBuilder.FULL_BUILD, null);
				}

				compilerException = null;
				status = getStatus(element);

				if (status.getSeverity() <= IStatus.WARNING) {
					break;
				}

				if (pass == 0 && status.getSeverity() > IStatus.WARNING) {
					// In case of an error in pass 0, there is a probability for
					// the errors to
					// disappear by just triggering another full build (without
					// clean!). Causes
					// can be compiler confusion and dependencies between
					// generated artifacts.

					log.info(logTxt + ": fast recompile");

					project.build(IncrementalProjectBuilder.FULL_BUILD, null);
					status = getStatus(element);

					if (status.getSeverity() > IStatus.WARNING) {
						for (IStatus s : status.getChildren()) {
							if (s.getSeverity() > IStatus.WARNING) {
								log.debug(s.getMessage());
							}
						}
					}
				}

			} catch (Exception e) {
				compilerException = e;
				status = new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR,
						"Exception during build of " + element.getName(), e);
				log.info(logTxt + ": got exception: " + e.toString());
			}
		}

		if (compilerException != null) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, logTxt + ": project can't be compiled",
					compilerException);
		}

		return status;
	}

	public static MultiStatus getStatus(TeaBuildProjectElement element) {
		MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, element.getName(), null);
		element.getStatus().forEach(status::add);
		return status;
	}

}
