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
package org.eclipse.tea.library.build.jar;

import org.eclipse.tea.core.services.TaskingConfigurationExtension;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfig;
import org.osgi.service.component.annotations.Component;

@TaskingConfig(description = "TEA ZIP Configuration")
@Component
public class ZipConfig implements TaskingConfigurationExtension {

	/**
	 * External ZIP is faster, especially on UNIX/Linux, default to using it if
	 * installed
	 */
	@TaskingConfigProperty(description = "Path to external ZIP application")
	public String zipProgramExecutable = "/usr/bin/zip";

	@TaskingConfigProperty(description = "Maximum parallel ZIP threads", name = "jarInstallThreadCount")
	public long zipParallelThreads = 5;

}
