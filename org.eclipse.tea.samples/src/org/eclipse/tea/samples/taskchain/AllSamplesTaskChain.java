package org.eclipse.tea.samples.taskchain;

import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.annotations.TaskChainMenuEntry;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.samples.menu.SampleMenuDecoration;
import org.eclipse.tea.samples.tasks.SampleAutoProgressTask;
import org.eclipse.tea.samples.tasks.SampleCancellableTask;
import org.eclipse.tea.samples.tasks.SampleConfiguredTask;
import org.eclipse.tea.samples.tasks.SampleDirectStatusAccessTask;
import org.eclipse.tea.samples.tasks.SampleE4CtxTask;
import org.eclipse.tea.samples.tasks.SampleExplicitProgressTask;
import org.eclipse.tea.samples.tasks.SampleNamedTask;
import org.eclipse.tea.samples.tasks.SampleOutputCaptureTask;
import org.eclipse.tea.samples.tasks.SampleSimpleTask;
import org.osgi.service.component.annotations.Component;

@TaskChainId(description="All Sample Tasks")
@TaskChainMenuEntry(path = SampleMenuDecoration.SAMPLE_MENU)
@Component
public class AllSamplesTaskChain implements TaskChain {

	@TaskChainContextInit
	public void init(TaskExecutionContext t) {
		t.addTask(SampleSimpleTask.class);
		t.addTask(SampleConfiguredTask.class);
		t.addTask(SampleCancellableTask.class);
		t.addTask(SampleDirectStatusAccessTask.class);
		t.addTask(SampleE4CtxTask.class);
		t.addTask(SampleNamedTask.class);
		t.addTask(SampleOutputCaptureTask.class);
		
		t.addTask(SampleAutoProgressTask.class);
		t.addTask(new SampleExplicitProgressTask(30));
	}
	
}
