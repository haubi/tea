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
package org.eclipse.tea.core.ui.internal;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.tea.core.internal.model.TaskingModel;
import org.eclipse.tea.core.internal.model.iface.TaskingContainer;

public class TaskingModelContentProvider implements ITreeContentProvider {

	private static final Object[] EMPTY_ARRAY = new Object[0];

	@Override
	public Object[] getElements(Object inputElement) {
		return new Object[] { ((TaskingModel) inputElement).getRootGroup() };
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TaskingContainer) {
			return ((TaskingContainer) parentElement).getChildren().toArray();
		}
		return EMPTY_ARRAY;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof TaskingContainer) {
			return !((TaskingContainer) element).getChildren().isEmpty();
		}
		return false;
	}

}
