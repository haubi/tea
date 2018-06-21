package org.eclipse.tea.samples.tasks;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;

public class SampleDirectStatusAccessTask {


	@Execute
	public void withStatus(IEclipseContext ctx) {
		ctx.set(IStatus.class, new Status(IStatus.WARNING, "org.eclipse.tea.smaples", "Some warning"));
	}
	
}
