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
package org.eclipse.tea.core.ui.internal.headless;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tea.core.services.TaskingHeadlessLifeCycle;
import org.eclipse.tea.core.services.TaskingHeadlessLifeCycle.HeadlessPrority;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.core.ui.Activator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.osgi.service.component.annotations.Component;

/**
 * A lot of tasks have a dependency to the UI (Debug UI, Platform UI, ...). Thus
 * we startup the workbench with a headless renderer whenever the TEA UI bundle
 * is part of the launch.
 * <p>
 * Note: this must be ranked higher than any contribution that requires access
 * to the workbench already.
 */
@HeadlessPrority(2000)
@Component
public class WorkbenchStarter implements TaskingHeadlessLifeCycle {

	private static final String PRODUCT_ID = Activator.PLUGIN_ID + ".HeadlessTaskingEngine";

	@HeadlessStartup
	public StartupAction startWorkbench(TaskingLog log) {
		// check for the correct product being launched, and error out if
		// not found. Otherwise a UI window may pop up in unexpected places.
		// Note that a DISPLAY is still required on headless machines (xfvb,
		// Xephyr, ...)
		if (Platform.getProduct() == null || !Platform.getProduct().getId().equals(PRODUCT_ID)) {
			throw new IllegalStateException(
					"Use the " + PRODUCT_ID + " product to launch the tasking engine with Workbench support.");
		}

		log.debug("Starting up E4 Workbench...");
		Thread workbench = new Thread(() -> {
			Thread.currentThread().setName("Tasking Headless Workbench");

			try {
				Display display = PlatformUI.createDisplay();
				PlatformUI.createAndRunWorkbench(display, new WorkbenchAdvisor() {
					@Override
					public String getInitialWindowPerspectiveId() {
						return null;
					}
				});
			} catch (Exception e) {
				log.error("Failed to start the workbench - is a DISPLAY set?", e);
				// no further handling - try to continue without...
			}

			// never reached - headless presentation engine spins event loop
			// endlessly.
		});
		workbench.setDaemon(true);
		workbench.start();

		while (!PlatformUI.isWorkbenchRunning()) {
			Thread.yield(); // quite busy polling :)
		}

		// this is a hack, but there is no propper way to do this :(
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// nevermind
		}

		return StartupAction.CONTINUE;
	}

	@HeadlessShutdown
	public void shutdown(TaskingLog log) throws Exception {
		cancelAutoBuild(); // don't want to wait on this

		// everything else /might/ be important, so wait.
		for (Job j : Job.getJobManager().find(null)) {
			if (j.isSystem()) {
				continue;
			}

			// cancel anything that is sitting in the background, waiting
			if (j.getState() != Job.RUNNING) {
				j.cancel();
			}

			// wait for a little time
			log.debug("Waiting on Job '" + j.getName() + "'...");
			j.join(10_000, new NullProgressMonitor());
		}

		Display.getDefault().syncExec(() -> PlatformUI.getWorkbench().close());
	}

	/**
	 * Cancels all automatic build and other related update and refresh jobs.
	 */
	private static void cancelAutoBuild() {
		Job[] jobs = Job.getJobManager().find(null);
		for (Job j : jobs) {
			if (j.belongsTo(ResourcesPlugin.FAMILY_AUTO_BUILD) || j.belongsTo(ResourcesPlugin.FAMILY_AUTO_REFRESH)) {
				j.cancel();
			}
		}

	}

}
