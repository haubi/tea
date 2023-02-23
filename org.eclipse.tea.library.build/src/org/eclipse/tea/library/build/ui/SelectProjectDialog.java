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
package org.eclipse.tea.library.build.ui;

import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

@SuppressWarnings("restriction")
public class SelectProjectDialog extends TitleAreaDialog {

	private IProject selected;
	private final String desc;
	private final String title;
	private String defaultSel;
	private final ViewerFilter[] filters;
	private final boolean multi;
	private IProject[] multiSelected;

	public SelectProjectDialog(Shell parentShell, String title, String desc, boolean multi, ViewerFilter... filters) {
		super(parentShell);

		this.title = title;
		this.desc = desc;
		this.multi = multi;
		this.filters = filters;
	}

	public void setDefaultSelection(String defaultSel) {
		this.defaultSel = defaultSel;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(title);
		setMessage(desc, IMessageProvider.INFORMATION);

		Composite comp = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
		GridLayoutFactory.fillDefaults().margins(20, 20).applyTo(comp);

		TableViewer tv;
		if (multi) {
			tv = CheckboxTableViewer.newCheckList(comp, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		} else {
			tv = new TableViewer(comp, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		}
		GridDataFactory.fillDefaults().grab(true, true).hint(300, 300).applyTo(tv.getControl());

		tv.setContentProvider(ArrayContentProvider.getInstance());
		tv.setLabelProvider(new DecoratingJavaLabelProvider(new JavaUILabelProvider()));
		tv.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
				if (sel == null || sel.isEmpty()) {
					selected = null;
				} else {
					selected = (IProject) sel.getFirstElement();
				}
				Button button = getButton(IDialogConstants.OK_ID);
				if (button != null) {
					button.setEnabled(selected != null);
				}
			}
		});

		if (multi) {
			((CheckboxTableViewer) tv).addCheckStateListener((e) -> {
				multiSelected = Arrays.stream(((CheckboxTableViewer) tv).getCheckedElements()).map(x -> (IProject) x)
						.toArray(IProject[]::new);
			});
		}

		if (filters != null && filters.length > 0) {
			tv.setFilters(filters);
		}

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		tv.setInput(root.getProjects());

		if (defaultSel != null) {
			// pre-select default selection bundle.
			IProject project = root.getProject(defaultSel);
			if (project != null) {
				tv.setSelection(new StructuredSelection(project), true);
				selected = project;
			}
		}

		return comp;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		getButton(IDialogConstants.OK_ID).setEnabled(selected != null);
	}

	public IProject getResult() {
		return selected;
	}

	public IProject[] getMultiResult() {
		return multiSelected;
	}

}
