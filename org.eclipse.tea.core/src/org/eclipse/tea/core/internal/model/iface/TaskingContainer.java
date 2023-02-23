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

import java.util.Set;

/**
 * A container for {@link TaskingElement}s.
 */
public interface TaskingContainer extends TaskingElement {

	/**
	 * @return children, sorted by grouping ID
	 */
	public Set<TaskingElement> getChildren();

	/**
	 * Lookup a nested {@link TaskingContainer} with the specified name.
	 *
	 * @param name
	 *            the name of the {@link TaskingContainer} to find.
	 * @return the {@link TaskingContainer} if found.
	 */
	public TaskingContainer getContainer(String name);

	/**
	 * Lookup a {@link TaskingItem} with the given ID.
	 *
	 * @param id
	 *            the id of the {@link TaskingItem} to find.
	 * @return the {@link TaskingItem} if found.
	 */
	public TaskingItem getItem(String id);

}
