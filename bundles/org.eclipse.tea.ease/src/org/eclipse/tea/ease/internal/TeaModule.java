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
package org.eclipse.tea.ease.internal;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.ease.IScriptEngine;
import org.eclipse.ease.modules.IEnvironment;
import org.eclipse.ease.modules.IScriptModule;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tea.core.TaskingEngine;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.core.ui.TaskingEngineJob;
import org.eclipse.tea.core.ui.config.TaskingEclipsePreferenceStore;

/**
 * Module for EASE that allows access and interaction with TEA
 */
public class TeaModule implements IScriptModule {

	private IScriptEngine scriptEngine;
	private TaskingEngine taskEngine;

	@Override
	public void initialize(IScriptEngine engine, IEnvironment environment) {
		this.scriptEngine = engine;

		taskEngine = TaskingEngine.withConfiguration(new TaskingEclipsePreferenceStore());
		TaskingLog log = taskEngine.getContext().get(TaskingLog.class);

		engine.setErrorStream(log.error());
		engine.setOutputStream(log.info());
		engine.setCloseStreamsOnTerminate(false);
	}

	/**
	 * @param chain
	 *            the {@link TaskChain} to execute or a name/alias that can be
	 *            mapped to a {@link TaskChain} during context lookup. Use
	 *            {@link #createTaskChain(String, Object[])} to create dynamic
	 *            chains from scripts,
	 *            {@link #createTaskChainFrom(String, String)} to extend an
	 *            existing task chain, or {@link #lookupTaskChain(String)} to
	 *            simply take an existing registered {@link TaskChain}.
	 */
	@WrapToScript
	public IStatus runTaskChain(Object chain) throws Exception {
		TaskingEngineJob[] result = new TaskingEngineJob[1];
		Display.getDefault().syncExec(() -> {
			// must be CREATED on UI thread.
			result[0] = new TaskingEngineJob(taskEngine, chain);
		});
		TaskingEngineJob job = result[0];
		job.schedule();
		try {
			// must be JOINED in the BG, never in UI thread.
			job.join();
		} catch (InterruptedException e) {
			// ignore
		}
		return job.getActualResult();
	}

	/**
	 * @param name
	 *            the user visible name used to announce the task chain in the
	 *            log.
	 * @param tasks
	 *            the tasks that should be executed. These can either be
	 *            instance of TEA tasks (discovered by looking for a method on
	 *            the object that is annotated with {@link Execute}. Virtually
	 *            anything else that is executable by the scripting engine can
	 *            be used instead, to execute code from the script, or java, or
	 *            ...
	 * @return a TaskChain that wraps the given tasks and can be run with
	 *         {@link #runTaskChain(Object)}.
	 */
	@WrapToScript
	public TaskChain createTaskChain(String name, Object[] tasks) {
		return new EaseTaskChainWrapper(name, scriptEngine, new ArrayList<>(Arrays.asList(tasks)));
	}

	/**
	 * @param name
	 *            the name of the wrapper
	 * @param template
	 *            the template {@link TaskChain} class name or alias.
	 * @return a new EASE-wrapping {@link TaskChain} with the given name,
	 *         pre-filled with all Tasks from the given reference
	 *         {@link TaskChain}, identified by full class name or any
	 *         registered alias.
	 */
	@WrapToScript
	public TaskChain createTaskChainFrom(String name, String template) {
		TaskChain chain = lookupTaskChain(template);

		EaseTaskChainWrapper wrapper = new EaseTaskChainWrapper(name, scriptEngine, new ArrayList<>());

		wrapper.addTask(chain);
		return wrapper;
	}

	/**
	 * @param nameOrAlias
	 *            the name of the {@link TaskChain} to lookup in the system
	 * @return the task chain instance or <code>null</code>.
	 */
	@WrapToScript
	public TaskChain lookupTaskChain(String nameOrAlias) {
		IEclipseContext lookup = taskEngine.getContext().createChild();
		lookup.set(TaskingInjectionHelper.CTX_TASK_CHAIN, nameOrAlias);

		TaskChain chain = lookup.get(TaskChain.class); // will use context
														// function
		return chain;
	}

	/**
	 * @return the {@link IEclipseContext} used for dependency injection on the
	 *         underlying {@link TaskingEngine}. Can be used to retrieve any
	 *         required information.
	 */
	@WrapToScript
	public IEclipseContext getContext() {
		return taskEngine.getContext().getActiveLeaf();
	}

	/**
	 * @return the logging facility used by the underlying
	 *         {@link TaskingEngine}.
	 */
	@WrapToScript
	public TaskingLog getLog() {
		return getContext().get(TaskingLog.class);
	}

}
