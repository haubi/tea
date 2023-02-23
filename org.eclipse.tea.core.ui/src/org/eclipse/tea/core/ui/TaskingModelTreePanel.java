/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.core.ui;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tea.core.internal.model.TaskingModel;
import org.eclipse.tea.core.internal.model.iface.TaskingElement;
import org.eclipse.tea.core.ui.internal.TaskingModelContentProvider;
import org.eclipse.tea.core.ui.internal.TaskingModelLabelProvider;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

public class TaskingModelTreePanel extends Composite {

	private FilteredTree tree;

	public TaskingModelTreePanel(Composite parent, TaskingModel model) {
		super(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(this);

		tree = new FilteredTree(this, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER, new PatternFilter() {
			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				String labelText = ((TaskingElement) element).getLabel();
				if (labelText == null) {
					return false;
				}
				return wordMatches(labelText);
			}
		}, true);
		tree.getViewer().setContentProvider(new TaskingModelContentProvider());
		tree.getViewer().setLabelProvider(new DelegatingStyledCellLabelProvider(new TaskingModelLabelProvider()));
		tree.getViewer().setInput(model);

		tree.getViewer().setExpandedState(model.getRootGroup(), true);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);
	}

	public void addSelectionListener(ISelectionChangedListener listener) {
		tree.getViewer().addSelectionChangedListener(listener);
	}

	public TaskingElement getSelection() {
		Object o = tree.getViewer().getStructuredSelection().getFirstElement();
		if (o instanceof TaskingElement) {
			return (TaskingElement) o;
		}

		return null;
	}

}
