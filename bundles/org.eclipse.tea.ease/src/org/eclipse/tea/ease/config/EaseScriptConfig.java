/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package org.eclipse.tea.ease.config;

import org.eclipse.tea.core.services.TaskingConfigurationExtension;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfig;
import org.osgi.service.component.annotations.Component;

@TaskingConfig(description = "EASE Script Configuration")
@Component
public class EaseScriptConfig implements TaskingConfigurationExtension {

	@TaskingConfigProperty(description = "Name of the EASE script to start", headlessOnly = true)
	public String easeScript;

}
