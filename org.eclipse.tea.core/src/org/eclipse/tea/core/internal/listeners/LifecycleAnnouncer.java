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

import java.text.NumberFormat;
import java.util.List;

import javax.inject.Named;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Service;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.annotations.lifecycle.BeginTask;
import org.eclipse.tea.core.annotations.lifecycle.BeginTaskChain;
import org.eclipse.tea.core.annotations.lifecycle.FinishTask;
import org.eclipse.tea.core.annotations.lifecycle.FinishTaskChain;
import org.eclipse.tea.core.internal.TaskProgressEstimationService;
import org.eclipse.tea.core.internal.TimeHelper;
import org.eclipse.tea.core.internal.config.CoreConfig;
import org.eclipse.tea.core.internal.model.TaskingModel;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskingLifeCycleListener;
import org.eclipse.tea.core.services.TaskingLifeCycleListener.TaskingLifeCyclePriority;
import org.eclipse.tea.core.services.TaskingLog;
import org.osgi.service.component.annotations.Component;

/**
 * Announces {@link TaskChain}s and tasks as they are executed by the
 * {@link TaskExecutionContext}.
 * <p>
 * Also prints a summary of task execution after a {@link TaskChain} has
 * finished executing
 */
@Component
@TaskingLifeCyclePriority(90)
public class LifecycleAnnouncer implements TaskingLifeCycleListener {

	private static final int WIDTH = 100;
	private long startTime = 0;

	@BeginTaskChain
	public void beginChain(TaskingLog log, CoreConfig config, TaskExecutionContext context, TaskChain chain) {
		log.info(header(config, "CHAIN START " + TaskingModel.getTaskChainName(chain)));
		measureMemoryUsage(log, config);

		startTime = System.currentTimeMillis();
	}

	@FinishTaskChain
	public void finishChain(TaskingLog log, CoreConfig config, TaskExecutionContext context, MultiStatus result,
			@Named(TaskingInjectionHelper.CTX_PREPARED_TASKS) List<Object> allTasks, TaskingStatusTracker tracker) {
		log.info(header(config, "Results"));
		for (Object task : allTasks) {
			String nameStatus = readableStatus(tracker.getStatus(context, task)) + " " + TaskingModel.getTaskName(task)
					+ ": ";
			log.info(
					nameStatus + fillLeft(config, TimeHelper.formatDetailedDuration(tracker.getDuration(context, task)),
							' ', WIDTH - nameStatus.length()));
		}
		log.info(separator(config));
		String msg = "TOTAL (" + (result.getSeverity() < IStatus.ERROR ? "SUCCESS" : "FAILED") + "): ";
		log.info(msg + fillLeft(config, TimeHelper.formatDuration(System.currentTimeMillis() - startTime), ' ',
				WIDTH - msg.length()));
	}

	@BeginTask
	public void beginTask(TaskingLog log, CoreConfig config, @Named(TaskingInjectionHelper.CTX_TASK) Object task,
			@Optional @Service TaskProgressEstimationService svc) {
		String id = svc == null ? null : svc.calculateId(task);
		if (id != null) {
			log.info(header(config, "(  GO  ) " + TaskingModel.getTaskName(task) + " [ETA: "
					+ TimeHelper.formatDetailedDuration(svc.getEstimatedMillis(id)) + "]"));
		} else {
			log.info(header(config, "(  GO  ) " + TaskingModel.getTaskName(task)));
		}
	}

	@FinishTask
	public void finishTask(TaskingLog log, CoreConfig config, @Named(TaskingInjectionHelper.CTX_TASK) Object task,
			IStatus taskStatus) {
		measureMemoryUsage(log, config);
		log.info(header(config, readableStatus(taskStatus) + " " + TaskingModel.getTaskName(task)));

		if (taskStatus.getException() != null && taskStatus.getSeverity() != IStatus.CANCEL) {
			log.error("Error while executing task", taskStatus.getException());
		}
	}

	private String readableStatus(IStatus s) {
		if (s == null) {
			return "(      )";
		}
		switch (s.getSeverity()) {
		case IStatus.CANCEL:
			return "(CANCEL)";
		case IStatus.ERROR:
			if (s.getException() != null) {
				return "( EXCPT)";
			} else {
				return "( ERROR)";
			}
		case IStatus.INFO:
			return "( INFO )";
		case IStatus.WARNING:
			return "( WARN )";
		case IStatus.OK:
			return "(  OK  )";
		default:
			return "(      )";
		}
	}

	private void measureMemoryUsage(TaskingLog log, CoreConfig config) {
		if (!config.measureMemoryUsage) {
			return;
		}

		System.gc();
		long totalMemory = Runtime.getRuntime().totalMemory();
		long maxMemory = Runtime.getRuntime().maxMemory();
		long freeMemory = Runtime.getRuntime().freeMemory();

		long usedMemory = totalMemory - freeMemory;
		long availableMemory = maxMemory - usedMemory;

		NumberFormat format = NumberFormat.getNumberInstance();

		String msg = "used memory=" + format.format(bytesToMega(usedMemory)) + "M, available memory="
				+ format.format(bytesToMega(availableMemory)) + "M";

		log.debug(msg);
	}

	private static long bytesToMega(long bytes) {
		return bytes / (1024 * 1024);
	}

	private String fillRight(CoreConfig config, String s, char pad, int length) {
		if (s.length() >= length || config.useAccessibleMode) {
			return s;
		}
		return s + getPaddedString(pad, length - s.length());
	}

	private String fillLeft(CoreConfig config, String s, char pad, int length) {
		if (s.length() >= length || config.useAccessibleMode) {
			return s;
		}
		return getPaddedString(pad, length - s.length()) + s;
	}

	private String getPaddedString(char pad, int length) {
		return String.format("%" + (length) + "s", pad).replace(' ', pad);
	}

	private String header(CoreConfig cfg, String s) {
		if (cfg.useAccessibleMode) {
			return s;
		}

		return fillLeft(cfg, " " + s + " ---", '-', WIDTH);
	}

	private String separator(CoreConfig config) {
		return fillRight(config, "-", '-', WIDTH);
	}

}
