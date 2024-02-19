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
package org.eclipse.tea.core.internal.stats;

import org.eclipse.tea.core.services.TaskingConfigurationExtension;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfig;
import org.osgi.service.component.annotations.Component;

/**
 * Holds configuration for statistics reporting to a remote server.
 */
@TaskingConfig(description = "TEA Statistics Reporting")
@Component
public class TaskingStatConfig implements TaskingConfigurationExtension {

	@TaskingConfigProperty(description = "POST JSON to Server")
	public String statServer = null;

	@TaskingConfigProperty(description = "Debug Mode (print JSON to console; post even if error)")
	public boolean statDebugMode = false;

}
