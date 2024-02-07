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
package org.eclipse.tea.core.ui.internal.listeners;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.tea.core.annotations.lifecycle.BeginTask;
import org.eclipse.tea.core.annotations.lifecycle.BeginTaskChain;
import org.eclipse.tea.core.annotations.lifecycle.CreateContext;
import org.eclipse.tea.core.annotations.lifecycle.DisposeContext;
import org.eclipse.tea.core.annotations.lifecycle.FinishTask;
import org.eclipse.tea.core.annotations.lifecycle.FinishTaskChain;
import org.eclipse.tea.core.services.TaskingLifeCycleListener;
import org.eclipse.tea.core.ui.internal.context.E4WorkbenchContextFunction;
import org.osgi.service.component.annotations.Component;

@Component
public class EventBrokerBridge implements TaskingLifeCycleListener {

	public static final String EVENT_TOPIC_BASE = "org/eclipse/tea/";

	public static final String EVENT_CTX_CREATE = EVENT_TOPIC_BASE + "CreateContext";
	public static final String EVENT_CTX_DISPOSE = EVENT_TOPIC_BASE + "DisposeContext";

	public static final String EVENT_CHAIN_BEGIN = EVENT_TOPIC_BASE + "BeginTaskChain";
	public static final String EVENT_CHAIN_FINISH = EVENT_TOPIC_BASE + "FinishTaskChain";

	public static final String EVENT_TASK_BEGIN = EVENT_TOPIC_BASE + "BeginTask";
	public static final String EVENT_TASK_FINISH = EVENT_TOPIC_BASE + "FinishTask";

	@CreateContext
	public void ctxCreate(@Optional @Named(E4WorkbenchContextFunction.E4_CONTEXT_ID) IEclipseContext ctx,
			IEclipseContext context) {
		broadcast(ctx, EVENT_CTX_CREATE, context);
	}

	@DisposeContext
	public void ctxDispose(@Optional @Named(E4WorkbenchContextFunction.E4_CONTEXT_ID) IEclipseContext ctx,
			IEclipseContext context) {
		broadcast(ctx, EVENT_CTX_DISPOSE, context);
	}

	@BeginTaskChain
	public void chainBegin(@Optional @Named(E4WorkbenchContextFunction.E4_CONTEXT_ID) IEclipseContext ctx,
			IEclipseContext context) {
		broadcast(ctx, EVENT_CHAIN_BEGIN, context);
	}

	@FinishTaskChain
	public void chainFinish(@Optional @Named(E4WorkbenchContextFunction.E4_CONTEXT_ID) IEclipseContext ctx,
			IEclipseContext context) {
		broadcast(ctx, EVENT_CHAIN_FINISH, context);
	}

	@BeginTask
	public void taskBegin(@Optional @Named(E4WorkbenchContextFunction.E4_CONTEXT_ID) IEclipseContext ctx,
			IEclipseContext context) {
		broadcast(ctx, EVENT_TASK_BEGIN, context);
	}

	@FinishTask
	public void taskFinish(@Optional @Named(E4WorkbenchContextFunction.E4_CONTEXT_ID) IEclipseContext ctx,
			IEclipseContext context) {
		broadcast(ctx, EVENT_TASK_FINISH, context);
	}

	private void broadcast(IEclipseContext e4ctx, String event, Object data) {
		IEventBroker eventBroker = e4ctx.get(IEventBroker.class);
		if (eventBroker != null) {
			eventBroker.send(event, data);
		}
	}

}
