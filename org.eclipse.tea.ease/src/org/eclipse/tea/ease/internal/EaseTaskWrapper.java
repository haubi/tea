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

import java.lang.reflect.Method;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.ease.IScriptEngine;
import org.eclipse.tea.core.internal.model.TaskingModel;

/**
 * Wraps an arbitrary object into a TEA Task. This might be a TEA task itself
 * (class or instance), or anything that can be executed by scripting.
 */
public final class EaseTaskWrapper {
	private final String name;
	private final Object task;
	private final boolean isTea;
	private final IScriptEngine scriptEngine;

	public EaseTaskWrapper(String name, Object o, IScriptEngine scriptEngine) {
		this.name = name;
		this.task = o;
		this.scriptEngine = scriptEngine;
		this.isTea = isTeaTask(o);
	}

	/**
	 * @param o
	 *            the object to check
	 * @return whether the given object is a "native" TEA task object (i.e. it
	 *         has a method annotated with {@link Execute}). Otherwise the
	 *         object is considered as "something that is recognized by
	 *         scripting".
	 */
	private static boolean isTeaTask(Object o) {
		if (o instanceof Class) {
			return true;
		}

		for (Method m : o.getClass().getDeclaredMethods()) {
			Execute e = m.getAnnotation(Execute.class);
			if (e != null) {
				return true;
			}
		}
		return false;
	}

	@Execute
	public void runThisTask(IEclipseContext ctx) throws Exception {
		if (isTea) {
			Object inst = task;
			if (inst instanceof Class) {
				inst = ContextInjectionFactory.make((Class<?>) inst, ctx);
			}

			ContextInjectionFactory.invoke(inst, Execute.class, ctx);
			return;
		}

		// last try to use whatever it is as script.
		scriptEngine.inject(task, false);
	}

	@Override
	public String toString() {
		if (isTea) {
			return TaskingModel.getTaskName(task);
		}
		return "Script: " + name;
	}
}