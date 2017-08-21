package org.eclipse.tea.samples.tasks;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskingLog;

@Named("Another Custom Named Task")
public class SampleNamedTaskAnnotation {

	@Execute
	public void hello(TaskingLog log) {
		log.info("Hello World");
	}

}
