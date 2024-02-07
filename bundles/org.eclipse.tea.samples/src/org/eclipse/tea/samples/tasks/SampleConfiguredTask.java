package org.eclipse.tea.samples.tasks;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.samples.config.SampleConfig;

public class SampleConfiguredTask {

	@Execute
	public void configured(TaskingLog log, SampleConfig cfg) {
		log.info("config: " + cfg.myProperty + " - " + cfg.myBoolean);
	}
	
}
