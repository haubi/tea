package org.eclipse.tea.samples.tasks;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.annotations.TaskCaptureStdOutput;

@TaskCaptureStdOutput
public class SampleOutputCaptureTask {

	private void someExternalStuff() {
		System.err.println("Something I would not see");
	}
	
	@Execute
	public void callExternal() {
		someExternalStuff();
	}
	
}
