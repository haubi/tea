package org.eclipse.tea.samples.tasks;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskProgressTracker.TaskProgressProvider;
import org.eclipse.tea.core.services.TaskingLog;

public class SampleExplicitProgressTask {

	private int work;

	public SampleExplicitProgressTask(int work) {
		this.work = work;
	}
	
	@Execute
	public void work(TaskingLog log, TaskProgressTracker tracker) throws InterruptedException {
		log.info("starting...");
		
		for(int i = 0; i < work; ++i) {
			tracker.worked(1);
			log.info("worked");
			Thread.sleep(100);
		}
		
		log.info("done");
	}
	
	@TaskProgressProvider
	public int getAmount() {
		return work;
	}
	
}
