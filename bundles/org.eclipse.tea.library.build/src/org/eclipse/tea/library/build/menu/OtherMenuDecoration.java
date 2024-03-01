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
package org.eclipse.tea.library.build.menu;

import org.eclipse.tea.core.services.TaskingMenuDecoration;

public class OtherMenuDecoration implements TaskingMenuDecoration {

	// OTHERS - label
	public static final String MENU_OTHER = "Others";

	// OTHERS - menu groupings
	@TaskingMenuGroupingId(menuPath = MENU_OTHER, beforeGroupingId = NO_GROUPING)
	public static final String GRP_OTHER_TEA_BUILD = "other.tea.build";
	@TaskingMenuGroupingId(menuPath = MENU_OTHER, afterGroupingId = GRP_OTHER_TEA_BUILD)
	public static final String GRP_OTHER_UPDATE = "other.update";

	// OTHERS - decorations
	@TaskingMenuPathDecoration(menuPath = MENU_OTHER)
	public static final String DECO_OTHER_MENU = "icons/other.png";

}
