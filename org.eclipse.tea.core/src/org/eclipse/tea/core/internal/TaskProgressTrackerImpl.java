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
package org.eclipse.tea.core.internal;

import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.tea.core.services.TaskProgressTracker;

/**
 * {@link SubMonitor} based implementation of the {@link TaskProgressTracker}
 * interface.
 */
public class TaskProgressTrackerImpl implements TaskProgressTracker {

	private final SubMonitor monitor;

	public TaskProgressTrackerImpl(SubMonitor monitor) {
		this.monitor = monitor;
	}

	@Override
	public void worked(int amount) {
		monitor.worked(amount);
	}

	@Override
	public boolean isCanceled() {
		return monitor.isCanceled();
	}

	@Override
	public void setTaskName(String name) {
		monitor.subTask(name);
	}

	/**
	 * Allows to make a progress tracker "restricted", which means that it is
	 * not allowed to programmatically update the worked amount. It is allowed
	 * to set the current task name and to check for cancellation though.
	 */
	public static class RestrictedProgressTrackerImpl implements TaskProgressTracker {

		private final TaskProgressTracker delegate;

		public RestrictedProgressTrackerImpl(TaskProgressTracker delegate) {
			this.delegate = delegate;
		}

		@Override
		public void worked(int amount) {
			// ignore updates - updates are valid on the delegate itself only
		}

		@Override
		public boolean isCanceled() {
			return delegate.isCanceled();
		}

		@Override
		public void setTaskName(String name) {
			delegate.setTaskName(name);
		}

	}

}
