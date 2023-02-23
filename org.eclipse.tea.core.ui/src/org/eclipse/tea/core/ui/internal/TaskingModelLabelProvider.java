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

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tea.core.internal.model.iface.TaskingContainer;
import org.eclipse.tea.core.internal.model.iface.TaskingElement;
import org.eclipse.tea.core.internal.model.iface.TaskingItem;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;

import com.google.common.base.Joiner;

public class TaskingModelLabelProvider extends LabelProvider implements IStyledLabelProvider {

	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof TaskingContainer) {
			return new StyledString(((TaskingContainer) element).getLabel());
		} else {
			TaskingItem item = (TaskingItem) element;
			StyledString s = new StyledString(item.getLabel());

			TaskChainId id = item.getChain().getClass().getAnnotation(TaskChainId.class);
			if (id != null && id.alias().length != 0) {
				s.append(" - " + Joiner.on(", ").join(id.alias()), StyledString.DECORATIONS_STYLER);
			}

			return s.append(" - " + item.getChain().getClass().getName(), StyledString.QUALIFIER_STYLER);
		}
	}

	@Override
	public Image getImage(Object element) {
		TaskingElement te = (TaskingElement) element;
		return TaskingImageHelper.getSharedIcon(te.getIconBundle(), te.getIconPath());
	}

}
