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
package org.eclipse.tea.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.internal.TaskingConfigurationInitializer;
import org.eclipse.tea.core.internal.TaskingConfigurationStore;
import org.eclipse.tea.core.internal.TaskingEngineActivator;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskingLog;

/**
 * Helper to create tasking related, context dependent stuff
 */
public class TaskingInjectionHelper {

	public static final String CTX_TASK_CHAIN = TaskingEngineActivator.PLUGIN_ID + ".taskchain";
	public static final String CTX_CONFIG = TaskingEngineActivator.PLUGIN_ID + ".configuration";
	public static final String CTX_HEADLESS = TaskingEngineActivator.PLUGIN_ID + ".headless";
	public static final String CTX_OUTPUT = TaskingEngineActivator.PLUGIN_ID + ".output";
	public static final String CTX_TASK = TaskingEngineActivator.PLUGIN_ID + ".task";
	public static final String CTX_PREPARED_TASKS = TaskingEngineActivator.PLUGIN_ID + ".prepared_tasks";
	public static final String CTX_TASK_CONTEXTS = TaskingEngineActivator.PLUGIN_ID + ".prepared_task_contexts";
	public static final String CTX_TASK_WORK_AMOUNT = TaskingEngineActivator.PLUGIN_ID + ".task_work";

	/**
	 * @return the root context, which is the context for the TEA core bundle
	 */
	public static IEclipseContext getRootContext() {
		return EclipseContextFactory.getServiceContext(TaskingEngineActivator.getContext());
	}

	/**
	 * @param store
	 *            the backing configuration store
	 * @return a new {@link TaskingEngine} that can be used to run
	 *         {@link TaskChain}s.
	 */
	public static TaskingEngine createNewEngine(TaskingConfigurationStore store) {
		IEclipseContext engineContext = createConfiguredContext(store);
		return ContextInjectionFactory.make(TaskingEngine.class, engineContext);
	}

	/**
	 * Traverses the context tree upwards to find the context that is associated
	 * with the current {@link TaskExecutionContext}, or the top-level context
	 * if no {@link TaskExecutionContext} is associated with any parent.
	 *
	 * @param any
	 *            any context to traverse
	 * @return the context for the {@link TaskExecutionContext} or the top level
	 *         context.
	 */
	public static IEclipseContext findExecutionContext(IEclipseContext any) {
		if (any.getLocal(TaskExecutionContext.class) != null) {
			return any;
		}

		// don't use the root context, this would create global context members,
		// which we must avoid.
		if (any.getParent() == null || any.getParent().equals(TaskingInjectionHelper.getRootContext())) {
			System.err.println("cannot find execution context, using " + any);
			return any;
		}

		return findExecutionContext(any.getParent());
	}

	/**
	 * Removes the given class from all context's in the given context parent
	 * tree.
	 *
	 * @param context
	 *            the context
	 * @param toRemove
	 *            the key to remove
	 */
	public static void wipe(IEclipseContext context, Class<?> toRemove) {
		if (context == null) {
			return;
		}

		context.remove(toRemove);
		wipe(context.getParent(), toRemove);
	}

	/**
	 * @param store
	 *            the backing configuration store
	 * @return a context that has initialized configuration, and thus can be
	 *         used to obtain things like a configured {@link TaskingLog}
	 *         outside of the {@link TaskingEngine}
	 */
	public static IEclipseContext createConfiguredContext(TaskingConfigurationStore store) {
		IEclipseContext context = getRootContext().createChild("Engine with " + store.toString());
		context.set(TaskingConfigurationStore.class, store);
		TaskingConfigurationInitializer init = ContextInjectionFactory.make(TaskingConfigurationInitializer.class,
				context);
		ContextInjectionFactory.invoke(init, Execute.class, context);

		return context;
	}

	public static TaskExecutionContext createNewChainContext(TaskingEngine engine, String chainName,
			IProgressMonitor monitor) {
		IEclipseContext chainContext = engine.getContext().createChild(chainName);
		chainContext.set(TaskingInjectionHelper.CTX_TASK_CHAIN, chainName);
		chainContext.set(IProgressMonitor.class, monitor);

		return ContextInjectionFactory.make(TaskExecutionContext.class, chainContext);
	}

	/**
	 * @param engine
	 *            the engine that will execute this context
	 * @param chain
	 *            the task chain that should be executed by this context
	 * @return a context that holds all relevant information for execution
	 */
	public static TaskExecutionContext createNewChainContext(TaskingEngine engine, TaskChain chain,
			IProgressMonitor monitor) {
		IEclipseContext chainContext = engine.getContext().createChild(chain.getClass().getName());
		chainContext.set(TaskChain.class, chain);
		chainContext.set(IProgressMonitor.class, monitor);

		return ContextInjectionFactory.make(TaskExecutionContext.class, chainContext);
	}

	public static boolean isHeadless(IEclipseContext context) {
		Boolean headless = (Boolean) context.get(CTX_HEADLESS);
		if (headless == null) {
			headless = Boolean.FALSE;
		}
		return headless;
	}

}
