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
package org.eclipse.tea.ease.internal;

import java.util.List;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.ease.IScriptEngine;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.ease.EaseScriptTask;

/**
 * Dynamic {@link TaskChain} that wraps any "unknown" task (i.e. not a task (has
 * a method annotated with {@link Execute}), not a {@link TaskChain}) into an
 * {@link EaseTaskWrapper}. If it *is* a TEA Task, it is used as is, if it is a
 * {@link TaskChain}, the chain is expanded inline and all it's tasks are copied
 * to this {@link TaskChain}.
 */
public final class EaseTaskChainWrapper implements TaskChain {
	private final String name;
	private final IScriptEngine scriptEngine;
	private final List<Object> tasks;

	public EaseTaskChainWrapper(String name, IScriptEngine scriptEngine, List<Object> tasks) {
		this.name = name;
		this.scriptEngine = scriptEngine;
		this.tasks = tasks;
	}

	@TaskChainContextInit
	public void init(TaskExecutionContext context) {
		for (int i = 0; i < tasks.size(); ++i) {
			Object task = tasks.get(i);
			if (task instanceof TaskChain) {
				// instead of directly adding, expand the task chain into the
				// context;
				ContextInjectionFactory.invoke(task, TaskChainContextInit.class, context.getContext());
			} else if (task instanceof Class && TaskChain.class.isAssignableFrom((Class<?>) task)) {
				Object tc = ContextInjectionFactory.make((Class<?>) task, context.getContext());
				ContextInjectionFactory.invoke(tc, TaskChainContextInit.class, context.getContext());
			} else if (task instanceof String) {
				context.addTask(new EaseTaskWrapper(getShortScriptName((String) task), task, scriptEngine));
			} else {
				context.addTask(new EaseTaskWrapper("Anonymous " + i, task, scriptEngine));
			}
		}
	}

	private String getShortScriptName(String task) {
		if (task.length() > 20) {
			return task.substring(0, 17) + "...";
		}
		return task;
	}

	public void addTask(Object o) {
		tasks.add(o);
	}

	public void addScriptTask(String script) {
		tasks.add(new EaseScriptTask(script));
	}

	@Override
	public String toString() {
		return "EASE Dynamic TaskChain: " + name;
	}
}