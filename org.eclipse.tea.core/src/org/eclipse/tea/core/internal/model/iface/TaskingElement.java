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

/**
 * Base interface for {@link TaskingContainer} and {@link TaskingItem}.
 */
public interface TaskingElement {

	/**
	 * @return the symbolic name of the bundle containing the icon, if there is
	 *         one available via {@link #getIconPath()}.
	 */
	public String getIconBundle();

	/**
	 * @return the icon path relative to the root of the bundle identified by
	 *         {@link #getIconBundle()}.
	 */
	public String getIconPath();

	/**
	 * @return the human readable name of the {@link TaskingElement} (for UI
	 *         display).
	 */
	public String getLabel();

	/**
	 * @return the grouping ID that this {@link TaskingElement} should be
	 *         associated with.
	 */
	public String getGroupingId();

	/**
	 * @return whether this {@link TaskingElement} should only be visible in
	 *         development mode.
	 */
	public boolean isDevelopment();

}
