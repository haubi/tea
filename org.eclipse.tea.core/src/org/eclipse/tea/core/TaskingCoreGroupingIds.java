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
package org.eclipse.tea.core;

import org.eclipse.tea.core.services.TaskingMenuDecoration;
import org.osgi.service.component.annotations.Component;

/**
 * Defines constants for TEA's own menu grouping IDs.
 * <p>
 * Grouping ID's are used to keep items that belong together sorted in the Menu.
 * Also after each grouping region, a separator is inserted into the menu.
 */
@Component
public class TaskingCoreGroupingIds implements TaskingMenuDecoration {

	@TaskingMenuGroupingId(beforeGroupingId = NO_GROUPING)
	public static final String GID_DEVELOPMENT = "tea.core.dev";

	@TaskingMenuGroupingId(afterGroupingId = NO_GROUPING)
	public static final String GID_ABOUT = "tea.core.about";
}
