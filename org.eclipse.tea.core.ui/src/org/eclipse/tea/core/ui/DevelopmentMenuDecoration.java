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
package org.eclipse.tea.core.ui;

import org.eclipse.tea.core.TaskingCoreGroupingIds;
import org.eclipse.tea.core.services.TaskingMenuDecoration;
import org.osgi.service.component.annotations.Component;

@Component
public class DevelopmentMenuDecoration implements TaskingMenuDecoration {

	@TaskingMenuGroupingId(menuPath = "Development", beforeGroupingId = NO_GROUPING)
	public static final String DEV_GROUP_LISTS = "tea.core.dev.lists";

	@TaskingMenuPathDecoration(menuPath = "Development", groupingId = TaskingCoreGroupingIds.GID_DEVELOPMENT)
	public static final String ICON_DEV = "resources/tea.png";

}
