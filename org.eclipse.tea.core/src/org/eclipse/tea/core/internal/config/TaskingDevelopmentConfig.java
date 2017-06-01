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
package org.eclipse.tea.core.internal.config;

import org.eclipse.tea.core.services.TaskingConfigurationExtension;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfig;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

/**
 * Holds configuration for TEA related to development of TEA itself.
 */
@TaskingConfig(description = "TEA Development Options")
@Component(service = TaskingConfigurationExtension.class, property = { Constants.SERVICE_RANKING + "=1000" })
public class TaskingDevelopmentConfig implements TaskingConfigurationExtension {

	@TaskingConfigProperty(description = "Show TaskChains intended for development/debugging")
	public boolean showHiddenTaskChains = false;

	@TaskingConfigProperty(description = "Show Configuration for headless-only properties")
	public boolean showHeadlessConfig = false;

	@TaskingConfigProperty(description = "Show GroupingIDs before Menu Items")
	public boolean showGroupingIds = false;

	@TaskingConfigProperty(description = "Show debug log output")
	public boolean showDebugLogs = true; // default true for compat.

}
