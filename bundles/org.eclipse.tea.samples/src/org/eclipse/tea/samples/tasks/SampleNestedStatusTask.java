package org.eclipse.tea.samples.tasks;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;

public class SampleNestedStatusTask {

	@Execute
	public IStatus withStatus(IEclipseContext ctx) {
		MultiStatus ms = new MultiStatus("org.eclipse.tea.samples", 0, "OK", null);
		ms.add(new Status(IStatus.WARNING, "org.eclipse.tea.samples", "First warning"));
		ms.add(new Status(IStatus.WARNING, "org.eclipse.tea.smaples", "Second warning"));
		
		return ms;
	}

}
