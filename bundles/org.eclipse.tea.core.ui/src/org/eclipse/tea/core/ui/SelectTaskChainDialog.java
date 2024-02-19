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
package org.eclipse.tea.core.ui;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tea.core.internal.model.TaskingModel;
import org.eclipse.tea.core.internal.model.iface.TaskingItem;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.ui.internal.run.RunTaskChain;

@Creatable
public class SelectTaskChainDialog extends TitleAreaDialog {

	private TaskChain selectedChain;
	private final TaskingModel model;
	private TaskingModelTreePanel root;

	@Inject
	public SelectTaskChainDialog(TaskingModel model) {
		super(null);
		setBlockOnOpen(true);

		this.model = model;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(550, 500);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Execute TaskChain");
		setMessage("Select Task Chain to execute...");
		setTitleImage(
				Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "resources/tea-full60.png").createImage());

		root = new TaskingModelTreePanel(parent, model);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(root);

		root.addSelectionListener((e) -> {
			Object o = root.getSelection();
			if (o instanceof TaskingItem) {
				// prevent recursion
				getButton(OK).setEnabled(((TaskingItem) o).getChain().getClass() != RunTaskChain.class);
			} else {
				getButton(OK).setEnabled(false);
			}
		});

		return root;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(OK).setEnabled(false);
	}

	@Override
	protected void okPressed() {
		// OK button is only enabled when a TaskingItem is selected
		this.selectedChain = ((TaskingItem) root.getSelection()).getChain();

		super.okPressed();
	}

	public TaskChain getSelectedChain() {
		return selectedChain;
	}

}
