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
package org.eclipse.tea.core.internal.model.impl;

import org.eclipse.tea.core.internal.model.iface.TaskingItem;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskingAdditionalMenuEntryProvider.TaskingAdditionalMenuEntry;

public class TaskingAdditionalItemImpl implements TaskingItem {

	private final TaskingAdditionalMenuEntry entry;

	public TaskingAdditionalItemImpl(TaskingAdditionalMenuEntry entry) {
		this.entry = entry;
	}

	@Override
	public String getIconBundle() {
		return entry.getIconBundleName();
	}

	@Override
	public String getIconPath() {
		return entry.getIconPath();
	}

	@Override
	public String getLabel() {
		return entry.getLabel();
	}

	@Override
	public String getGroupingId() {
		return entry.getGroupingId();
	}

	@Override
	public boolean isDevelopment() {
		return entry.isDeveloperOnly();
	}

	@Override
	public TaskChain getChain() {
		return entry.getTaskChain();
	}

	@Override
	public boolean matchesId(String id) {
		return id.equals(entry.getMenuId());
	}

	@Override
	public boolean isVisibleInMenu() {
		return true;
	}

}
