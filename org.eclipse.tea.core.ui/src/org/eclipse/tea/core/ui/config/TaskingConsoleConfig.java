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
package org.eclipse.tea.core.ui.config;

import org.eclipse.tea.core.services.TaskingConfigurationExtension;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfig;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

@TaskingConfig(description = "TEA Console Configuration")
@Component(service = TaskingConfigurationExtension.class, property = { Constants.SERVICE_RANKING + "=900" })
public class TaskingConsoleConfig implements TaskingConfigurationExtension {

	@TaskingConfigProperty(description = "Use colored streams")
	public Boolean useColors = true;

	@TaskingConfigProperty(description = "Adapt colors to dark theme")
	public Boolean useDarkColors = false;

}
