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
package org.eclipse.tea.samples.config;

import org.eclipse.tea.core.services.TaskingMenuDecoration;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

@Component(service = TaskingMenuDecoration.class, property = { Constants.SERVICE_RANKING + "=-100" })
public class SubMenuIcon implements TaskingMenuDecoration {

	@TaskingMenuPathDecoration(menuPath = "Samples")
	public static final String SAMPLE_ICON = "resources/sample.gif";

}
