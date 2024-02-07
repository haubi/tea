/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package org.eclipse.tea.samples.tasks;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskingLog;

/**
 * Task which requires some other tasks state at construction time.
 */
public class SampleTaskPrintState {

	private final String state;

	public SampleTaskPrintState(String state) {
		this.state = state;

	}

	@Execute
	public IStatus run(TaskingLog log) {
		log.info(state);
		return new Status(IStatus.WARNING, "org.eclipse.tea.samples", "Something smells fishy");
	}

}
