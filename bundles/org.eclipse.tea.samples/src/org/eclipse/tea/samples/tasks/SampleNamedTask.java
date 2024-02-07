package org.eclipse.tea.samples.tasks;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskingLog;

public class SampleNamedTask {

	@Execute
	public void hello(TaskingLog log) {
		log.info("Hello World");
	}
	
	@Override
	public String toString() {
		return "Custom Named Task";
	}
	
}
