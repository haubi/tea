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
package org.eclipse.tea.core.internal.listeners;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.annotations.TaskReloadConfiguration;
import org.eclipse.tea.core.annotations.lifecycle.FinishTask;
import org.eclipse.tea.core.services.TaskingLifeCycleListener;
import org.eclipse.tea.core.services.TaskingLog;
import org.osgi.service.component.annotations.Component;

@Component
public class ConfigurationRefresher implements TaskingLifeCycleListener {

	@FinishTask
	public void refresh(IEclipseContext taskContext, TaskingLog log,
			@Named(TaskingInjectionHelper.CTX_TASK) Object task) {
		if (task.getClass().getAnnotation(TaskReloadConfiguration.class) != null) {
			// task requested reload of configuration after execution
			log.info("reconfiguring...");
			TaskingInjectionHelper.reConfigureContext(taskContext);
		}
	}

}
