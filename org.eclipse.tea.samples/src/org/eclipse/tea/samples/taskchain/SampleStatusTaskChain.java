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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.annotations.TaskChainMenuEntry;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.samples.menu.SampleMenuDecoration;
import org.osgi.service.component.annotations.Component;

@TaskChainId(description = "Status Test Chain", retries = 2)
@TaskChainMenuEntry(path = SampleMenuDecoration.SAMPLE_MENU)
@Component
public class SampleStatusTaskChain implements TaskChain {

	@TaskChainContextInit
	protected void init(TaskExecutionContext context, TaskingLog log) {
		log.debug("creating task chain");

		context.addTask(new MyTask(Status.OK_STATUS));
		context.addTask(new MyTask(new Status(IStatus.WARNING, "ID", "Some warning happened!")));
		context.addTask(new MyTask(new Status(IStatus.ERROR, "ID", "Some ERROR happened!")));
		context.addTask(new MyTask(null));
		context.addTask(new MyTask(new Status(IStatus.ERROR, "ID", "Some ERROR happened!")));
		context.addTask(new MyTask(Status.CANCEL_STATUS));
	}

	public final static class MyTask {

		private final IStatus status;

		public MyTask(IStatus s) {
			this.status = s;
		}

		@Execute
		public IStatus doIt(TaskingLog log) throws Exception {
			log.warn("I'm here: " + status);
			Thread.sleep(500);

			if (status == null) {
				throw new NullPointerException("woah..");
			}

			return status;
		}

		@Override
		public String toString() {
			return "Publishing status test for " + (status == null ? -1 : status.getSeverity());
		}

	}

}
