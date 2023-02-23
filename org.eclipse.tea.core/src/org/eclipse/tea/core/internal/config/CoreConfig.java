/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.core.internal.config;

import org.eclipse.tea.core.services.TaskingConfigurationExtension;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfig;
import org.osgi.service.component.annotations.Component;

/**
 * Holds core TEA configuration.
 */
@TaskingConfig(description = "TEA Core Configuration")
@Component
public class CoreConfig implements TaskingConfigurationExtension {

	@TaskingConfigProperty(description = "Measure memory usage after each task")
	public Boolean measureMemoryUsage = false;

	@TaskingConfigProperty(description = "Path to the configuration file of the headless build", headlessOnly = true)
	public String configurationSource;

	@TaskingConfigProperty(description = "Accessible Mode")
	public Boolean useAccessibleMode = false;

}
