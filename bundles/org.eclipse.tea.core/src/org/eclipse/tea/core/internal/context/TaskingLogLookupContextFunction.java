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
package org.eclipse.tea.core.internal.context;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.core.services.TaskingLog.TaskingLogInit;
import org.eclipse.tea.core.services.TaskingLog.TaskingLogQualifier;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Looks up a usable {@link TaskingLog} instance. This may be either the console
 * (or an output file) if running headless, or the "Tasking Log" view in the
 * IDE.
 */
@Component(service = IContextFunction.class, property = {
		"service.context.key=org.eclipse.tea.core.services.TaskingLog" })
public class TaskingLogLookupContextFunction extends ContextFunction {

	private final List<TaskingLog> services = new ArrayList<>();

	@Override
	public Object compute(IEclipseContext context, String contextKey) {
		Boolean headless = TaskingInjectionHelper.isHeadless(context);

		for (TaskingLog s : services) {
			TaskingLogQualifier q = s.getClass().getAnnotation(TaskingLogQualifier.class);
			if (q == null) {
				continue;
			}

			if (q.headless() == headless) {
				context.set(TaskingLog.class, s);
				ContextInjectionFactory.invoke(s, TaskingLogInit.class, context);
				return s;
			}
		}

		return IInjector.NOT_A_VALUE;
	}

	@Reference(service = TaskingLog.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	public void addTaskingLog(TaskingLog log) {
		services.add(log);
	}

	public void removeTaskingLog(TaskingLog log) {
		services.remove(log);
	}

}
