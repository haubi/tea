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
package org.eclipse.tea.samples.menu;

import org.eclipse.tea.core.services.TaskingMenuDecoration;
import org.osgi.service.component.annotations.Component;

@Component(service = TaskingMenuDecoration.class)
public class SampleMenuDecoration implements TaskingMenuDecoration {

	public static final String SAMPLE_MENU = "Samples";

	@TaskingMenuPathDecoration(menuPath = SAMPLE_MENU)
	public static final String SAMPLE_ICON = "resources/sample.gif";

}
