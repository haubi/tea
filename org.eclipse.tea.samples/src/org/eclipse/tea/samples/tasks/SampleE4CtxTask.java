package org.eclipse.tea.samples.tasks;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.tea.core.services.TaskingLog;

public class SampleE4CtxTask {

	@Execute
	public void e4(TaskingLog log, @Named("E4Context") IEclipseContext e4ctx) {
		ESelectionService sel = e4ctx.getActive(ESelectionService.class);
		log.warn("service = " + sel + ", sel="
				+ (sel != null && sel.getSelection() != null ? sel.getSelection().getClass().getName() : null));
	}
	
}
