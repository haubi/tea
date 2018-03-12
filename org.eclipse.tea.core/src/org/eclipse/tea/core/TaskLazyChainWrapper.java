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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.internal.model.TaskingModel;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskingLog;

class TaskLazyChainWrapper {

	private final TaskExecutionContext parent;
	private final TaskChain child;

	/**
	 * Creates a wrapper around another {@link TaskChain}
	 */
	public TaskLazyChainWrapper(TaskExecutionContext parent, TaskChain child) {
		this.parent = parent;
		this.child = child;
	}

	@Execute
	public IStatus execute(TaskingLog log) {
		IEclipseContext ec = parent.getContext().createChild(toString());
		TaskingEngine engine = ContextInjectionFactory.make(TaskingEngine.class, ec);
		TaskExecutionContext ctx = TaskingInjectionHelper.createNewChainContext(
				ec.createChild(TaskingModel.getTaskChainName(child)), child,
				parent.getContext().get(IProgressMonitor.class));

		return engine.runTaskChain(ctx);
	}

	@Override
	public String toString() {
		return "Lazy: " + TaskingModel.getTaskChainName(child);
	}

}
