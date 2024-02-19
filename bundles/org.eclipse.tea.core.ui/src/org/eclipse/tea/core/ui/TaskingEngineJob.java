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
package org.eclipse.tea.core.ui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.TaskingEngine;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.internal.model.TaskingModel;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.ui.annotations.TaskChainUiInit;
import org.eclipse.ui.PlatformUI;

/**
 * Job that wraps running a given task chain.
 * <p>
 * Note that this {@link Job}'s result is always {@link Status#OK_STATUS} to
 * prevent further error handling by Eclipse itself. Errors are handled inside
 * the {@link TaskingEngine} already. In case the actual status of the execution
 * is required, use {@link #getActualResult()} instead.
 */
public final class TaskingEngineJob extends Job {
	private final TaskingEngine engine;
	private final Object chain;
	private IStatus actualResult;

	/**
	 * @param engine
	 *            the engine to use to run the {@link TaskChain}
	 * @param chain
	 *            either a {@link TaskChain} or a name identifying a registered
	 *            {@link TaskChain} ({@link String}).
	 */
	public TaskingEngineJob(TaskingEngine engine, Object chain) {
		super("Tasking");
		this.chain = chain;
		this.engine = engine;

		// it is illegal to instantiate the job from outside the UI thread
		Assert.isLegal(Display.getCurrent() != null, "Cannot instantiate TaskingEngine Job from non-UI thread");

		// fetch the currently active window and set it's shell into the context
		// to allow creation of windows with a correct parent shell argument.
		if (!TaskingInjectionHelper.isHeadless(engine.getContext())) {
			engine.getContext().set(Shell.class, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());

			try {
				// this is run from the UI thread intentionally
				ContextInjectionFactory.invoke(chain, TaskChainUiInit.class, engine.getContext(), null);
			} catch (InjectionException e) {
				if (e.getCause() instanceof OperationCanceledException) {
					// UI init cancelled, should not run chain
					this.actualResult = Status.CANCEL_STATUS;
				} else {
					throw e;
				}
			}
		}
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (actualResult != null) {
			// something happened during initialization.
			return actualResult;
		}

		TaskExecutionContext context;
		if (chain instanceof TaskChain) {
			context = TaskingInjectionHelper.createNewChainContext(engine, (TaskChain) chain, monitor);
		} else if (chain instanceof String) {
			context = TaskingInjectionHelper.createNewChainContext(engine, (String) chain, monitor);
		} else {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Given object is not a TaskChain or String: " + chain);
		}

		setName(TaskingModel.getTaskChainName(context.getUnderlyingChain()));
		actualResult = engine.runTaskChain(context);

		// the engine handles logging of failures internally (using TaskingLog).
		// We do not want to report failures multiple times, thus we swallow any
		// error condition here to prevent message boxes popping up on the user.
		// to still be able to consume the actual status of the job, the
		// getActualResult() method must be used.
		return Status.OK_STATUS;
	}

	public IStatus getActualResult() {
		return actualResult;
	}

	@Override
	public boolean belongsTo(Object family) {
		return family == TaskChain.class;
	}

}