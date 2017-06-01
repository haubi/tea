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
package org.eclipse.tea.samples.taskchain;

import javax.inject.Named;

import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.tea.core.annotations.TaskCaptureStdOutput;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.samples.config.SampleConfigContributor;

@TaskCaptureStdOutput
public class DemoTask {

	@Execute
	public void someTask(IEclipseContext context, SampleConfigContributor config, TaskingLog log,
			TaskProgressTracker tracker, MultiStatus status, @Optional @Named("E4Context") IEclipseContext e4ctx)
			throws Exception {
		if (e4ctx != null) {
			ESelectionService sel = e4ctx.getActive(ESelectionService.class);
			log.warn("service = " + sel + ", sel="
					+ (sel != null && sel.getSelection() != null ? sel.getSelection().getClass().getName() : null));
		}

		System.out.println("this is a test");
		System.err.println("another test");

		doSomething();

		log.debug("My Config: " + config.myProperty + ", " + config.myBoolean);
		Thread.sleep(2000);
		log.info("My Config: " + config.myProperty + ", " + config.myBoolean);
		Thread.sleep(1000);
		tracker.setTaskName("Something new from " + config.myProperty);
		log.warn("My Config: " + config.myProperty + ", " + config.myBoolean);
		Thread.sleep(1000);
		if (tracker.isCanceled()) {
			throw new OperationCanceledException();
		}
		log.error("My Config: " + config.myProperty + ", " + config.myBoolean);
		Thread.sleep(2000);
	}

	protected void doSomething() {
		// nothing
	}

	@Override
	public String toString() {
		return "My Demo Task";
	}

}