/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package org.eclipse.tea.samples.taskchain;

import java.io.File;

import javax.inject.Named;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.annotations.TaskChainMenuEntry;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.core.ui.annotations.TaskChainUiInit;
import org.eclipse.tea.library.build.tasks.TaskImportPreferences;
import org.eclipse.tea.samples.menu.SampleMenuDecoration;
import org.osgi.service.component.annotations.Component;

@Component
@TaskChainId(description = "Import Preferences...")
@TaskChainMenuEntry(path = SampleMenuDecoration.SAMPLE_MENU, icon = "resources/sample.gif")
public class SamplePreferenceImportTaskChain implements TaskChain {

	public static final String KEY = "preferenceFile";
	public static final String FORCE = "forceImport";

	@TaskChainContextInit
	public void init(TaskExecutionContext c, @Named(KEY) String filename, @Named(FORCE) boolean force) {

		c.addTask(new TaskImportPreferences(new File(filename), force));
	}

	@TaskChainUiInit
	public void selectFile(Shell parent, IEclipseContext ctx) {
		FileDialog dlg = new FileDialog(parent, SWT.OPEN);
		String path = dlg.open();
		if (path == null) {
			throw new OperationCanceledException();
		}
		ctx.set(FORCE, MessageDialog.openQuestion(parent, "Override preferences",
				"Override existing non-default preferences?"));
		ctx.set(KEY, path);
	}

}
