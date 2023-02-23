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
package org.eclipse.tea.core.internal.context;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.internal.model.TaskingModel;
import org.eclipse.tea.core.internal.model.iface.TaskingItem;
import org.osgi.service.component.annotations.Component;

import com.google.common.base.Strings;

/**
 * {@link ContextFunction} that looks up a task chain by the name stored in the
 * context.
 */
@Component(service = IContextFunction.class, property = {
		"service.context.key=org.eclipse.tea.core.services.TaskChain" })
public class TaskChainLookupContextFunction extends ContextFunction {

	@Override
	public Object compute(IEclipseContext context, String contextKey) {
		String taskChainName = (String) context.get(TaskingInjectionHelper.CTX_TASK_CHAIN);
		if (Strings.isNullOrEmpty(taskChainName)) {
			return IInjector.NOT_A_VALUE;
		}

		TaskingModel model = ContextInjectionFactory.make(TaskingModel.class, context);
		TaskingItem lookup = model.getRootGroup().getItem(taskChainName);
		if (lookup == null) {
			throw new IllegalArgumentException("cannot find task chain named: " + taskChainName);
		}

		return lookup.getChain();
	}

}
