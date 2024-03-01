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
 * Defines constants for TEA's menu grouping IDs.
 * <p>
 * Grouping ID's are used to keep items that belong together sorted in the Menu.
 * Also after each grouping region, a separator is inserted into the menu.
 */
@Component
public class TeaMenuTopLevelGrouping implements TaskingMenuDecoration {

	@TaskingMenuGroupingId(beforeGroupingId = NO_GROUPING)
	public static final String GRP_DEVELOPMENT = "tea.core.dev";

	@TaskingMenuGroupingId(afterGroupingId = GRP_DEVELOPMENT)
	public static final String GRP_ORGANIZE = "tea.organize";

	@TaskingMenuGroupingId(afterGroupingId = GRP_ORGANIZE)
	public static final String GRP_OFTEN_USED = "tea.often_used";

	@TaskingMenuGroupingId(afterGroupingId = GRP_OFTEN_USED)
	public static final String GRP_ADVANCED = "tea.advanced";

	@TaskingMenuGroupingId(afterGroupingId = GRP_ADVANCED)
	public static final String GRP_GENERATORS = "tea.generators";

}
