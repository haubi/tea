/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package org.eclipse.tea.samples.tasks;

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
	public void run(TaskingLog log) {
		log.info(state);
	}

}
