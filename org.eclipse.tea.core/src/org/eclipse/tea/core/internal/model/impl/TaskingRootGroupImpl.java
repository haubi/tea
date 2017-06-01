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

import org.eclipse.tea.core.internal.TaskingEngineActivator;
import org.eclipse.tea.core.internal.model.iface.TaskingContainer;
import org.eclipse.tea.core.internal.model.iface.TaskingElement;

/**
 * Represents the top-most root {@link TaskingContainer}. This is the parent of
 * all {@link TaskingElement}s.
 */
public class TaskingRootGroupImpl extends TaskingContainerImpl {

	public TaskingRootGroupImpl() {
		super(null);
	}

	@Override
	public String getIconBundle() {
		return TaskingEngineActivator.getContext().getBundle().getSymbolicName();
	}

	@Override
	public String getIconPath() {
		return "resources/tea.png";
	}

	@Override
	public String getLabel() {
		return "Tasking";
	}

}
