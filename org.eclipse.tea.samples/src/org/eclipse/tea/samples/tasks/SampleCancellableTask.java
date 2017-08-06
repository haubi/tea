package org.eclipse.tea.samples.tasks;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskingLog;

public class SampleCancellableTask {

	@Execute
	public void cancellable(TaskingLog log, TaskProgressTracker tracker) throws InterruptedException {
		Thread.sleep(5000);
		
		if(tracker.isCanceled()) {
			throw new OperationCanceledException();
		}
		
		log.info("I am here!");
	}
	
}
