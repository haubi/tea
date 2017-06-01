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
package org.eclipse.tea.core.internal.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Named;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.annotations.lifecycle.BeginTask;
import org.eclipse.tea.core.annotations.lifecycle.BeginTaskChain;
import org.eclipse.tea.core.annotations.lifecycle.DisposeContext;
import org.eclipse.tea.core.annotations.lifecycle.FinishTask;
import org.eclipse.tea.core.annotations.lifecycle.FinishTaskChain;
import org.eclipse.tea.core.internal.model.TaskingModel;
import org.eclipse.tea.core.services.TaskingLifeCycleListener;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

/**
 * Tracks status and duration of tasks executed in a task chain
 */
@Component(property = { Constants.SERVICE_RANKING + "=1000" })
public class TaskingStatusTracker implements TaskingLifeCycleListener {

	private final Map<Key, IStatus> statusMap = new HashMap<>();
	private final Map<Key, Long> durationMap = new HashMap<>();
	private final Map<TaskExecutionContext, Long> ctxDurationMap = new HashMap<>();
	private final Map<TaskExecutionContext, IStatus> ctxStatusMap = new HashMap<>();

	@BeginTaskChain
	public void init(TaskExecutionContext context, MultiStatus overallStatus) {
		ctxDurationMap.put(context, System.currentTimeMillis());
		ctxStatusMap.put(context, overallStatus);
	}

	@BeginTask
	public void start(TaskExecutionContext context, @Named(TaskingInjectionHelper.CTX_TASK) Object task) {
		durationMap.put(new Key(task, context), System.currentTimeMillis());
	}

	@FinishTask
	public void stop(TaskExecutionContext context, @Named(TaskingInjectionHelper.CTX_TASK) Object task,
			IStatus taskStatus) {
		Key key = new Key(task, context);
		Long start = durationMap.get(key);
		if (start == null) {
			throw new IllegalStateException("no start time set for task: " + TaskingModel.getTaskName(task));
		}
		durationMap.put(key, System.currentTimeMillis() - start);
		statusMap.put(key, taskStatus);
	}

	@FinishTaskChain
	public void done(TaskExecutionContext context, MultiStatus overallStatus) {
		Long start = ctxDurationMap.get(context);
		if (start == null) {
			throw new IllegalStateException("no start time set for context: " + context);
		}
		ctxDurationMap.put(context, System.currentTimeMillis() - start);
	}

	@DisposeContext
	public void removeContext(TaskExecutionContext context) {
		ctxDurationMap.remove(context);
		ctxStatusMap.remove(context);

		Set<Key> toRemove = new HashSet<>();
		Stream.concat(durationMap.keySet().stream(), statusMap.keySet().stream()).filter(k -> k.context.equals(context))
				.forEach(toRemove::add);

		toRemove.forEach(durationMap::remove);
		toRemove.forEach(statusMap::remove);
	}

	public long getDuration(TaskExecutionContext context) {
		return ctxDurationMap.getOrDefault(context, 0l);
	}

	public IStatus getStatus(TaskExecutionContext context) {
		return ctxStatusMap.getOrDefault(context, null);
	}

	public long getDuration(TaskExecutionContext context, Object task) {
		return durationMap.getOrDefault(new Key(task, context), 0l);
	}

	public IStatus getStatus(TaskExecutionContext context, Object task) {
		return statusMap.getOrDefault(new Key(task, context), null);
	}

	private static class Key {

		private final TaskExecutionContext context;
		private final Object task;

		public Key(Object task, TaskExecutionContext context) {
			this.task = task;
			this.context = context;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((context == null) ? 0 : context.hashCode());
			result = prime * result + ((task == null) ? 0 : task.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Key other = (Key) obj;
			if (context == null) {
				if (other.context != null) {
					return false;
				}
			} else if (!context.equals(other.context)) {
				return false;
			}
			if (task == null) {
				if (other.task != null) {
					return false;
				}
			} else if (!task.equals(other.task)) {
				return false;
			}
			return true;
		}
	}

}
