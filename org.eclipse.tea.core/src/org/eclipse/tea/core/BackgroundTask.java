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
package org.eclipse.tea.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.internal.model.TaskingModel;

public class BackgroundTask {

	private static final ExecutorService backgroundTasks = Executors
			.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	private final Object task;
	private Future<?> future;

	public BackgroundTask(Object task) {
		Object actualTask;
		if (task instanceof Class) {
			try {
				actualTask = ((Class<?>) task).getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new IllegalStateException("Cannot create task " + task);
			}
		} else {
			actualTask = task;
		}

		this.task = actualTask;
	}

	@Execute
	public void run(IEclipseContext context) throws Exception {
		future = backgroundTasks.submit(() -> {
			ContextInjectionFactory.invoke(task, Execute.class, context);
		});
	}

	public Object barrier() {
		return new Object() {
			@Execute
			public Object doWait() throws Exception {
				return future.get();
			}

			@Override
			public String toString() {
				return "Wait for: " + BackgroundTask.this.toString();
			}
		};
	}

	public static Object allBarrier(List<Object> tasks) {
		List<BackgroundTask> toAwait = new ArrayList<>();
		for (Object task : tasks) {
			if (task instanceof BackgroundTask) {
				toAwait.add((BackgroundTask) task);
			}
		}
		if (toAwait.isEmpty()) {
			return null;
		}
		return new Object() {
			@Execute
			public void doWaitAll(IEclipseContext context) throws Exception {
				for (BackgroundTask bt : toAwait) {
					bt.future.get();
				}
			}

			@Override
			public String toString() {
				return "Await unfinished background tasks.";
			}
		};
	}

	@Override
	public String toString() {
		return TaskingModel.getTaskName(task) + " (parallel)";
	}

}
