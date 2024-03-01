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

import org.eclipse.tea.core.TeaMenuTopLevelGrouping;
import org.eclipse.tea.core.services.TaskingMenuDecoration;
import org.osgi.service.component.annotations.Component;

@Component
public class DevelopmentMenuDecoration implements TaskingMenuDecoration {

	public static final String MENU_DEVELOPMENT = "Development";

	@TaskingMenuPathDecoration(menuPath = MENU_DEVELOPMENT, groupingId = TeaMenuTopLevelGrouping.GRP_DEVELOPMENT)
	public static final String ICON_DEV = "resources/tea.png";

}
