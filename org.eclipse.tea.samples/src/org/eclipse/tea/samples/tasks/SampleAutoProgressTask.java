package org.eclipse.tea.samples.tasks;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskingLog;

public class SampleAutoProgressTask {

	@Execute
	public void progressTracked(TaskingLog log, TaskProgressTracker tracker) throws InterruptedException {
		Thread.sleep(10_000);
	}
	
}
