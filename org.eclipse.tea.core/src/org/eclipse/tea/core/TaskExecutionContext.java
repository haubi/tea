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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Service;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.annotations.TaskChainSuppressLifecycle;
import org.eclipse.tea.core.annotations.lifecycle.BeginTask;
import org.eclipse.tea.core.annotations.lifecycle.BeginTaskChain;
import org.eclipse.tea.core.annotations.lifecycle.CreateContext;
import org.eclipse.tea.core.annotations.lifecycle.DisposeContext;
import org.eclipse.tea.core.annotations.lifecycle.FinishTask;
import org.eclipse.tea.core.annotations.lifecycle.FinishTaskChain;
import org.eclipse.tea.core.internal.OutputRedirector;
import org.eclipse.tea.core.internal.TaskProgressEstimationService;
import org.eclipse.tea.core.internal.TaskProgressExtendedTracker;
import org.eclipse.tea.core.internal.TaskProgressTrackerImpl;
import org.eclipse.tea.core.internal.TaskingEngineActivator;
import org.eclipse.tea.core.internal.model.TaskingModel;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskProgressTracker.TaskProgressProvider;
import org.eclipse.tea.core.services.TaskingLifeCycleListener;
import org.eclipse.tea.core.services.TaskingLifeCycleListener.TaskingLifeCyclePriority;
import org.eclipse.tea.core.services.TaskingLog;

/**
 * Controls the execution of a {@link TaskChain}.
 */
public class TaskExecutionContext {

	private final TaskChain chain;
	private final IEclipseContext context;

	private final List<Object> tasks = new ArrayList<>();
	private final List<TaskingLifeCycleListener> listeners = new ArrayList<>();

	@Inject
	public TaskExecutionContext(IEclipseContext context, TaskChain chain,
			@Service List<TaskingLifeCycleListener> listeners) throws Exception {
		this.chain = chain;
		this.context = context;

		// create fresh instances of all listeners - later use PROTOTYPE in DS
		// 1.3
		for (TaskingLifeCycleListener listener : listeners) {
			TaskingLifeCycleListener l = listener.getClass().newInstance();
			this.listeners.add(l);

			// register listener for internal direct access
			context.set(l.getClass().getName(), l);
		}

		this.listeners.sort((a, b) -> {
			int prioA = 10;
			int prioB = 10;

			TaskingLifeCyclePriority pA = a.getClass().getAnnotation(TaskingLifeCyclePriority.class);
			TaskingLifeCyclePriority pB = b.getClass().getAnnotation(TaskingLifeCyclePriority.class);

			if (pA != null) {
				prioA = pA.value();
			}
			if (pB != null) {
				prioB = pB.value();
			}

			int x = prioB - prioA;
			if (x != 0) {
				return x;
			}
			return a.getClass().getName().compareTo(b.getClass().getName());
		});

		// make ourself available to the task chain
		context.set(TaskExecutionContext.class, this);

		// initialize this context
		ContextInjectionFactory.invoke(chain, TaskChainContextInit.class, context);

		// in case the context contains background-able tasks
		Object barrierTask = BackgroundTask.allBarrier(tasks);
		if (barrierTask != null) {
			addTask(barrierTask);
		}

		// only notify about context creation if there is something to do. an
		// empty context will not have any effect on anything and will, in fact,
		// not be executed at all by the engine.
		if (!isEmpty()) {
			notifyAll(CreateContext.class, context);
		}
	}

	/**
	 * @return the dependency injection context for the
	 *         {@link TaskExecutionContext}
	 */
	public IEclipseContext getContext() {
		return context;
	}

	/**
	 * @return the number of retries that are allowed on failure
	 */
	public int getRetries() {
		TaskChainId id = chain.getClass().getAnnotation(TaskChainId.class);
		if (id == null) {
			return 1;
		}
		return id.retries();
	}

	/**
	 * @return the underlying {@link TaskChain} which serves as a template for
	 *         this execution context. Only to be used for informational
	 *         purposes
	 */
	public TaskChain getUnderlyingChain() {
		return chain;
	}

	/**
	 * @return whether there are any tasks to execute in this context.
	 */
	public boolean isEmpty() {
		return tasks.isEmpty();
	}

	/**
	 * @param o
	 *            an arbitrary task that has a method annotated with the
	 *            {@link Execute} annotation. Can be either the actual object or
	 *            a the {@link Class} for the object to be created. It is also
	 *            possible to add an instance or {@link Class} object of type
	 *            {@link TaskChain}, which will inline expand all the tasks
	 *            contained in the {@link TaskChain}.
	 */
	public void addTask(Object o) {
		if (o instanceof TaskChain) {
			ContextInjectionFactory.invoke(o, TaskChainContextInit.class, context);
		} else if (o instanceof Class && TaskChain.class.isAssignableFrom((Class<?>) o)) {
			Object tc = ContextInjectionFactory.make((Class<?>) o, context);
			ContextInjectionFactory.invoke(tc, TaskChainContextInit.class, context);
		} else {
			tasks.add(o);
		}
	}

	public void addTaskAt(int index, Object o) {
		tasks.add(index, o);
	}

	/**
	 * @param chain
	 *            a TaskChain which should be initialized and executed lazily at
	 *            this point in the task chain.
	 */
	public void addLazyChain(TaskChain chain) {
		tasks.add(new TaskLazyChainWrapper(this, chain));
	}

	/**
	 * Executes the tasks in the context. Exposes status and progress handling
	 * to tasks, manages life cycle events.
	 */
	@Execute
	public void execute(TaskingLog log, @Optional @Service TaskProgressEstimationService progressService,
			@Optional IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus(TaskingEngineActivator.PLUGIN_ID, IStatus.OK,
				"Tasking Execution Context Status", null);

		context.set(IWorkspace.class, ResourcesPlugin.getWorkspace());
		context.set(MultiStatus.class, status);
		context.activate();

		Map<Object, IEclipseContext> taskContexts = new LinkedHashMap<>();
		int totalAmount = prepareTaskProgressTracking(log, progressService, taskContexts);
		context.set(TaskingInjectionHelper.CTX_TASK_CONTEXTS, taskContexts);

		notifyAll(BeginTaskChain.class, context);

		// monitor!=null=> taskChainName is already in monitor
		SubMonitor rootMonitor = SubMonitor.convert(monitor,
				monitor != null ? "" : TaskingModel.getTaskChainName(chain), totalAmount);

		// execute tasks
		try {
			for (Map.Entry<Object, IEclipseContext> ctx : taskContexts.entrySet()) {
				Object task = ctx.getKey();
				IEclipseContext taskCtx = ctx.getValue();

				Integer amount = (Integer) taskCtx.get(TaskingInjectionHelper.CTX_TASK_WORK_AMOUNT);

				// setup dedicated progress monitor, based on previous work
				// amount calculation
				String taskName = TaskingModel.getTaskName(task);
				SubMonitor taskMonitor = rootMonitor.split(amount).setWorkRemaining(amount);
				taskMonitor.setTaskName(taskName);

				TaskProgressTracker tracker = new TaskProgressTrackerImpl(task, taskMonitor);
				taskCtx.set(TaskProgressExtendedTracker.class, (TaskProgressExtendedTracker) tracker);

				notifyAll(BeginTask.class, taskCtx);

				// handle estimation request of tasks
				// (TaskProgressEstimated)
				String estimationId = progressService == null ? null : progressService.calculateId(task);
				if (estimationId != null) {
					// begin tracking with the real tracker
					progressService.begin(estimationId, tracker);

					// forbid explicit updating of worked amount for tasks.
					tracker = new TaskProgressTrackerImpl.RestrictedProgressTrackerImpl(tracker);
				}

				// tracker is available in any case. if estimated it is
				// restricted.
				taskCtx.set(TaskProgressTracker.class, tracker);

				try {
					// actually execute the task
					executeSingleTask(log, task, taskCtx);

				} finally {
					// override status if cancelled
					if (rootMonitor.isCanceled()) {
						taskCtx.set(IStatus.class, Status.CANCEL_STATUS);
					}

					// in case the task set it's own status
					IStatus taskStatus = taskCtx.get(IStatus.class);
					status.add(taskStatus);

					// stop estimated progress for this task
					if (estimationId != null && progressService != null) {
						progressService.finish(estimationId, taskStatus);
					}

					notifyAll(FinishTask.class, taskCtx);
				}

				// handle ERROR and CANCEL status publishes when the task did
				// not throw an exception
				IStatus taskStatus = taskCtx.get(IStatus.class);
				if (taskStatus.getSeverity() >= IStatus.ERROR) {
					log.error("Task aborted with status " + taskStatus);
					break;
				}
			}
		} catch (Throwable t) {
			status.add(
					new Status(IStatus.ERROR, TaskingEngineActivator.PLUGIN_ID, "Failed to texecute " + toString(), t));
			throw t;
		} finally {
			notifyAll(FinishTaskChain.class, context);

			// need a second step to avoid races with listener list.
			notifyAll(DisposeContext.class, context);
			context.deactivate();
		}
	}

	/**
	 * @param log
	 *            the log to use if required
	 * @param task
	 *            the task to execute
	 * @param taskCtx
	 *            the {@link IEclipseContext} to use for dependency injection.
	 */
	private static void executeSingleTask(TaskingLog log, Object task, IEclipseContext taskCtx) {
		OutputRedirector redir = new OutputRedirector(task, log);

		taskCtx.set(IStatus.class,
				new Status(IStatus.OK, TaskingEngineActivator.PLUGIN_ID, "Task: " + TaskingModel.getTaskName(task)));
		try {
			// possibly redirect system.out and system.err to the log
			redir.begin();

			// make task's context the active leaf
			taskCtx.activate();

			// and run the task
			Object result = ContextInjectionFactory.invoke(task, Execute.class, taskCtx);

			// check if a status was returned
			if (result instanceof IStatus) {
				taskCtx.set(IStatus.class, (IStatus) result);
			}
		} catch (Throwable t) {
			if (t instanceof InjectionException && t.getCause() instanceof OperationCanceledException) {
				OperationCanceledException oce = (OperationCanceledException) t.getCause();
				taskCtx.set(IStatus.class, new Status(IStatus.CANCEL, TaskingEngineActivator.PLUGIN_ID,
						"Cancelled: " + TaskingModel.getTaskName(task), oce));
			} else {
				taskCtx.set(IStatus.class, new Status(IStatus.ERROR, TaskingEngineActivator.PLUGIN_ID,
						"Fatal failure while executing " + TaskingModel.getTaskName(task), t));
			}
		} finally {
			// reset redirection
			redir.finish();

			// notify listeners and deactivate the context last.
			taskCtx.deactivate();
		}
	}

	/**
	 * Prepares progress tracking for tasks by:
	 * <ol>
	 * <li>creating task instances where required, so that all tasks are
	 * available
	 * <li>call
	 * {@link #prepareSingleTaskProgressTracking(TaskingLog, TaskProgressEstimationService, Map, Object)}
	 * for each task
	 * <li>summing up the amount of work for each task and returning the total
	 * amount of work.
	 * </ol>
	 *
	 * @param log
	 *            the log to use if required
	 * @param progressService
	 *            the {@link TaskProgressEstimationService} if available,
	 *            otherwise <code>null</code>
	 * @param taskContexts
	 *            a map (probably want to use {@link LinkedHashMap}) that will
	 *            be filled with task-to-context mapping
	 * @return the total amount of work for all tasks
	 */
	private int prepareTaskProgressTracking(TaskingLog log, TaskProgressEstimationService progressService,
			Map<Object, IEclipseContext> taskContexts) {
		List<Object> instances = prepareTaskInstances();
		context.set(TaskingInjectionHelper.CTX_PREPARED_TASKS, instances);

		// gather progress information in two stages.
		int totalAmount = 0;
		for (Object o : instances) {
			totalAmount += prepareSingleTaskProgressTracking(log, progressService, taskContexts, o);
		}
		return totalAmount;
	}

	/**
	 * @param log
	 *            the log to use if required
	 * @param progressService
	 *            the {@link TaskProgressEstimationService} if available,
	 *            <code>null</code> otherwise.
	 * @param taskContexts
	 *            a map (probably want to use {@link LinkedHashMap}) that will
	 *            be filled with task-to-context mapping
	 * @param o
	 *            the task to prepare tracking for
	 * @return the amount of work that this single task will allocate
	 */
	private int prepareSingleTaskProgressTracking(TaskingLog log, TaskProgressEstimationService progressService,
			Map<Object, IEclipseContext> taskContexts, Object o) {
		Integer amount = getTaskWorkAmount(log, o, progressService);

		IEclipseContext taskContext = context.createChild(o.getClass().getName());
		taskContext.set(TaskingInjectionHelper.CTX_TASK_WORK_AMOUNT, amount);
		taskContext.set(TaskingInjectionHelper.CTX_TASK, o);

		taskContexts.put(o, taskContext);
		return amount;
	}

	/**
	 * Determines the work amount of a single task. This may either be
	 * <ul>
	 * <li>an explicit value given by a {@link TaskProgressProvider} method
	 * <li>an estimation provided by the given
	 * {@link TaskProgressEstimationService}.
	 * </ul>
	 *
	 * @param log
	 *            the log to use if required
	 * @param o
	 *            the task to get work amount for
	 * @param service
	 *            the {@link TaskProgressEstimationService} if available,
	 *            <code>null</code> otherwise
	 * @return the absolute amount of work for this single task
	 */
	private Integer getTaskWorkAmount(TaskingLog log, Object o, TaskProgressEstimationService service) {
		String id = service == null ? null : service.calculateId(o);
		if (id != null) {
			return service.getEstimatedTicks(id);
		} else {
			// this code path will only hit if there is a TaskProgressProvider
			// method found by the service
			try {
				return (Integer) ContextInjectionFactory.invoke(o, TaskProgressProvider.class, context,
						Integer.valueOf(1));
			} catch (Exception e) {
				log.debug("Failed to determine amount of work for " + TaskingModel.getTaskName(o), e);
				return 1;
			}
		}
	}

	/**
	 * Prepares tasks by creating instances for tasks that have been added as
	 * {@link Class}.
	 *
	 * @return a list of task instances
	 */
	private List<Object> prepareTaskInstances() {
		List<Object> result = new ArrayList<>();

		for (Object o : tasks) {
			if (o instanceof Class) {
				o = ContextInjectionFactory.make((Class<?>) o, context);
			}
			result.add(o);
		}

		return result;
	}

	private boolean isSuppressed(Class<? extends TaskChain> tcClass) {
		TaskChainSuppressLifecycle ann = tcClass.getAnnotation(TaskChainSuppressLifecycle.class);
		if (ann == null || ann.value() == false) {
			return false;
		}

		return true;
	}

	/**
	 * Notify all registered {@link TaskingLifeCycleListener} about a given
	 * event.
	 *
	 * @param event
	 *            the event
	 * @param ctx
	 *            the context used for dependency injection on the listeners.
	 */
	private void notifyAll(Class<? extends Annotation> event, IEclipseContext ctx) {
		if (isSuppressed(chain.getClass())) {
			return;
		}

		for (TaskingLifeCycleListener l : listeners) {
			ContextInjectionFactory.invoke(l, event, ctx, null);
		}
	}

	@Override
	public String toString() {
		return "ExecutionContext[" + chain.getClass().getName() + "]";
	}

}
