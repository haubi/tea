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
package org.eclipse.tea.core.ui.internal;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.tea.core.internal.TaskProgressEstimationService;
import org.eclipse.tea.core.internal.model.TaskingModel;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskProgressTracker.TaskProgressProvider;
import org.eclipse.tea.core.ui.Activator;
import org.osgi.service.component.annotations.Component;

/**
 * Handles estimated progress. Estimation number is the amount of 100 ms steps.
 */
@Component
public class TaskProgressEstimationImpl implements TaskProgressEstimationService {

	private static final int RESOLUTION = 100; // 100 ms resolution;

	private static final AtomicInteger threadNo = new AtomicInteger(0);
	private final ExecutorService executor = Executors.newCachedThreadPool((r) -> {
		return new Thread(r, "Tasking Progress Reporter " + threadNo.incrementAndGet());
	});

	private final Map<String, Future<?>> futures = new TreeMap<>();
	private final Map<String, Long> startTimes = new TreeMap<>();

	@Override
	public int getEstimatedTicks(String id) {
		int pref = Activator.getInstance().getPreferenceStore().getInt(makeId(id));
		if (pref <= 0) {
			return 1;
		}

		return pref; // = ms / RESOLUTION
	}

	@Override
	public long getEstimatedMillis(String id) {
		return getEstimatedTicks(id) * RESOLUTION;
	}

	private void updateEstimation(String id, long runtime) {
		int current = getEstimatedTicks(id);
		int newValue = (int) (runtime / RESOLUTION);

		// if there is a deviation of more than 50%, just use the new result
		int estimate;
		if ((newValue / 2) > current) {
			estimate = newValue; // new value is much longer
		} else if ((current / 2) > newValue) {
			estimate = newValue; // new value is much shorter
		} else {
			// within deviation range, update estimation with average
			estimate = (newValue + current) / 2;
		}

		IPreferenceStore preferenceStore = Activator.getInstance().getPreferenceStore();
		preferenceStore.setValue(makeId(id), estimate);
		if (preferenceStore instanceof IPersistentPreferenceStore) {
			try {
				((IPersistentPreferenceStore) preferenceStore).save();
			} catch (Exception e) {
				Activator.getInstance().getLog()
						.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Cannot save task runtime estimation", e));
			}
		}
	}

	private static String makeId(String id) {
		return "tea.progress.estimation." + id;
	}

	@Override
	public void begin(String id, TaskProgressTracker rootTracker) {
		// start execution
		futures.put(id, executor.submit(() -> {
			int currentEstimation = getEstimatedTicks(id);

			while (currentEstimation > 0) {
				try {
					Thread.sleep(RESOLUTION);
				} catch (InterruptedException e) {
					return; // expected if task completes earlier
				}
				rootTracker.worked(1);
				currentEstimation--;
			}

			// exceeded estimation! we're quitting nevertheless, nothing to do
			// anymore.
		}));
		startTimes.put(id, System.currentTimeMillis());
	}

	@Override
	public void finish(String id, IStatus status) {
		try {
			Future<?> future = futures.remove(id);
			if (future == null) {
				// oups - not expected - do nothing
				return;
			}
			future.cancel(true); // tell it we're done

			Long start = startTimes.remove(id);
			if (start == null) {
				// oh f*ck...?! even less expected!
				return;
			}

			// only update if not ERROR or CANCEL
			if (status.getSeverity() < IStatus.ERROR) {
				updateEstimation(id, System.currentTimeMillis() - start);
			}
		} catch (Exception e) {
			// uh oh - and now?
			Activator.getInstance().getLog()
					.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Cannot finish progress tracking"));
		}
	}

	@Override
	public String calculateId(Object task) {
		for (Method m : task.getClass().getMethods()) {
			if (m.getAnnotation(TaskProgressProvider.class) != null) {
				return null; // uses explicit progress.
			}
		}

		String name = TaskingModel.getTaskName(task);
		return name.replaceAll("[^A-Za-z0-9_.]+", "_");
	}

}
