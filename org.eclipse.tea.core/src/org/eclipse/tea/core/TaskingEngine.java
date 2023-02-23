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
package org.eclipse.tea.core;

import javax.inject.Inject;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.internal.TaskingConfigurationStore;
import org.eclipse.tea.core.internal.TaskingEngineActivator;
import org.eclipse.tea.core.services.TaskingLog;

/**
 * Root entry into Tasking. Has a distinct dependency injection context and is
 * responsible to executing {@link TaskExecutionContext}'s.
 */
@Creatable
public class TaskingEngine {

	/**
	 * The main context used by this engine. Engines should not share context.
	 */
	private final IEclipseContext context;
	private final TaskingLog log;

	@Inject
	public TaskingEngine(IEclipseContext context, TaskingLog log) {
		this.context = context;
		this.log = log;
	}

	/**
	 * Creates a new engine with the given configuration store.
	 *
	 * @param config
	 *            the configuration store to use
	 * @return a new tasking engine
	 */
	public static TaskingEngine withConfiguration(TaskingConfigurationStore config) {
		return TaskingInjectionHelper.createNewEngine(config);
	}

	/**
	 * @return the dependency injection context for this {@link TaskingEngine}
	 */
	public IEclipseContext getContext() {
		return context;
	}

	/**
	 * @param chain
	 *            the context that should be run
	 * @return a status of the execution
	 */
	public IStatus runTaskChain(TaskExecutionContext chain) {
		if (chain.isEmpty()) {
			return Status.OK_STATUS;
		}

		int retries = chain.getRetries();
		MultiStatus status = new MultiStatus(TaskingEngineActivator.PLUGIN_ID, IStatus.OK, "", null); // dummy
		for (int i = 0; i < retries; ++i) {
			ContextInjectionFactory.invoke(chain, Execute.class, chain.getContext());
			status = chain.getContext().get(MultiStatus.class);
			if (status.getSeverity() >= IStatus.ERROR) {
				if (retries - 1 == i) {
					// that's it; we're done
					log.error("cannot execute '" + chain + "'");
				} else {
					log.info("Inhibiting failure on '" + chain + "', retrying...");
				}
				continue;
			}
			return status;
		}
		return status;
	}

}
