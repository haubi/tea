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
package org.eclipse.tea.core.internal.listeners;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.tea.core.services.TaskingHeadlessLifeCycle;
import org.eclipse.tea.core.services.TaskingLog;
import org.osgi.service.component.annotations.Component;

/**
 * Makes sure the workspace is saved when execution of all tasks is done.
 */
@Component
public class WorkspaceSaver implements TaskingHeadlessLifeCycle {

	@HeadlessShutdown
	public void shutdown(TaskingLog log) throws Exception {
		log.debug("Saving Workspace...");
		ResourcesPlugin.getWorkspace().save(true, null);
	}

}
