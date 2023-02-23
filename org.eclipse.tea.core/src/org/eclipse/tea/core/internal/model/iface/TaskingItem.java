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
package org.eclipse.tea.core.internal.model.iface;

import org.eclipse.tea.core.services.TaskChain;

/**
 * Represents a single {@link TaskChain} in the system
 */
public interface TaskingItem extends TaskingElement {

	/**
	 * @return the underlying {@link TaskChain} service.
	 */
	public TaskChain getChain();

	/**
	 * @param id
	 *            the ID to check.
	 * @return whether the {@link TaskChain} or any of it's alias' matches the
	 *         given ID.
	 */
	public boolean matchesId(String id);

	/**
	 * @return whether this {@link TaskingItem} should be visible in a menu
	 *         representation of all {@link TaskChain}s.
	 */
	public boolean isVisibleInMenu();

}
