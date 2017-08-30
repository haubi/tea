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
import org.osgi.service.component.annotations.Component;

@Component
public class BuildLibraryMenu implements TaskingMenuDecoration {

	public static final String MENU_BUILD = "TEA Build Library";

	@TaskingMenuGroupingId(beforeGroupingId = NO_GROUPING)
	public static final String GROUP_TOP_LEVEL_BUILD = "tea.toplevel.build.group";

	@TaskingMenuGroupingId(menuPath = MENU_BUILD, beforeGroupingId = NO_GROUPING)
	public static final String GROUP_BUILD = "tea.build.group";

	@TaskingMenuGroupingId(menuPath = MENU_BUILD, afterGroupingId = GROUP_BUILD)
	public static final String GROUP_JAR = "tea.jar.group";

	@TaskingMenuGroupingId(menuPath = MENU_BUILD, afterGroupingId = GROUP_JAR)
	public static final String GROUP_MISC = "tea.misc.group";

	@TaskingMenuPathDecoration(menuPath = MENU_BUILD, groupingId = GROUP_TOP_LEVEL_BUILD)
	public static final String ICON_BUILD = "icons/build_exec.png";

}
