/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.core.ui.internal.menu;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tea.core.ui.Activator;

public class AboutTea {
	@Execute
	public void execute() {
		new AboutDlg().open();
	}

	private static final class AboutDlg extends Dialog {

		protected AboutDlg() {
			super((Shell) null);
		}

		@Override
		protected Point getInitialSize() {
			return new Point(300, 220);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			getShell().setText("About Eclipse TEA");

			Composite comp = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().applyTo(comp);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);

			Label logo = new Label(comp, SWT.NONE);
			logo.setImage(
					Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "resources/tea-full60.png").createImage());
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).grab(true, true).applyTo(logo);

			Label l = new Label(comp, SWT.CENTER);
			l.setText("Tasking Engine Advanced\nIDE Task Automation made easy.");
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).grab(true, false).applyTo(l);

			return comp;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		}

	}

}