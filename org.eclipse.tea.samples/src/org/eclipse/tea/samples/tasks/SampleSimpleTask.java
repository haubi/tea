package org.eclipse.tea.samples.tasks;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskingLog;

public class SampleSimpleTask {

	@Execute
	public void doIt(TaskingLog log) {
		log.info("Hello World");
	}
	
}
