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
package org.eclipse.tea.samples.config;

import org.eclipse.tea.core.services.TaskingConfigurationExtension;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfig;
import org.osgi.service.component.annotations.Component;

@TaskingConfig(description = "Sample Configuration")
@Component(service = TaskingConfigurationExtension.class)
public class SampleConfig implements TaskingConfigurationExtension {

	@TaskingConfigProperty(description = "Sample String", headlessOnly = true)
	public String myProperty;

	@TaskingConfigProperty(description = "Sample Boolean")
	public Boolean myBoolean = true;

}
