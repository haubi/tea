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
package org.eclipse.tea.core.internal.model.impl;

import org.eclipse.tea.core.annotations.TaskChainMenuEntry;
import org.eclipse.tea.core.internal.model.TaskingModel;
import org.eclipse.tea.core.internal.model.iface.TaskingItem;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.core.services.TaskingMenuDecoration;
import org.osgi.framework.FrameworkUtil;

import com.google.common.base.Strings;

public class TaskingItemImpl implements TaskingItem {

	private final TaskChain chain;

	public TaskingItemImpl(TaskChain chain) {
		this.chain = chain;
	}

	@Override
	public TaskChain getChain() {
		return chain;
	}

	@Override
	public String getIconBundle() {
		return FrameworkUtil.getBundle(chain.getClass()).getSymbolicName();
	}

	@Override
	public String getIconPath() {
		TaskChainMenuEntry entry = chain.getClass().getAnnotation(TaskChainMenuEntry.class);
		if (entry == null || Strings.isNullOrEmpty(entry.icon())) {
			return null;
		}

		return entry.icon();
	}

	@Override
	public String getLabel() {
		return TaskingModel.getTaskChainName(chain);
	}

	@Override
	public boolean matchesId(String id) {
		if (id.equals(chain.getClass().getName())) {
			return true;
		}

		TaskChainId tcid = chain.getClass().getAnnotation(TaskChainId.class);
		if (tcid == null) {
			return false;
		}

		for (String alias : tcid.alias()) {
			if (id.equals(alias)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String getGroupingId() {
		TaskChainMenuEntry entry = chain.getClass().getAnnotation(TaskChainMenuEntry.class);
		if (entry == null) {
			return TaskingMenuDecoration.NO_GROUPING;
		}

		return entry.groupingId();
	}

	@Override
	public boolean isDevelopment() {
		TaskChainMenuEntry entry = chain.getClass().getAnnotation(TaskChainMenuEntry.class);
		if (entry == null) {
			return true;
		}

		return entry.development();
	}

	@Override
	public boolean isVisibleInMenu() {
		return chain.getClass().getAnnotation(TaskChainMenuEntry.class) != null;
	}

	@Override
	public String toString() {
		return getLabel();
	}

}
