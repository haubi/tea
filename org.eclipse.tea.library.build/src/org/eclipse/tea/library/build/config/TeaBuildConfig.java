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
package org.eclipse.tea.library.build.config;

import org.eclipse.tea.core.services.TaskingConfigurationExtension;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfig;
import org.osgi.service.component.annotations.Component;

@TaskingConfig(description = "TEA Build Configuration")
@Component
public class TeaBuildConfig implements TaskingConfigurationExtension {

	@TaskingConfigProperty(description = "Failure Threshold")
	public long failureThreshold = 10;

	@TaskingConfigProperty(description = "Compile retries per project")
	public long compileRetries = 2;

	@TaskingConfigProperty(description = "Use batch compile mode (experimental)")
	public boolean batchCompile = false;

	@TaskingConfigProperty(description = "Location of the TEA maven configuration file")
	public String mavenConfigFilePath;

	@TaskingConfigProperty(description = "Products to export", headlessOnly = true, name = "products")
	public String productsToExport;

}
