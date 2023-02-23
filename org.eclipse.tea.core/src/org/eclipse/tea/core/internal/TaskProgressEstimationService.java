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
package org.eclipse.tea.core.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tea.core.services.TaskProgressTracker;

/**
 * Handles progress management for tasks that want their progress reporting
 * estimated based on previous runs.
 */
public interface TaskProgressEstimationService {

	/**
	 * Lookup a stored estimation for the given ID. Can be used to initialize
	 * progress tracking.
	 *
	 * @param id
	 *            the ID of the piece of work to lookup estimation for
	 * @return an estimation if available, never returns < 1.
	 */
	public int getEstimatedTicks(String id);

	/**
	 * Lookup a stored estimation for the given ID,
	 *
	 * @param id
	 *            the ID of the piece of work to lookup estimation for
	 * @return an estimation if available in milliseconds
	 */
	public long getEstimatedMillis(String id);

	/**
	 * Informs the service that a piece of work is starting. The progress
	 * service will start reporting progress depending on the stored estimation
	 * for the given ID.
	 *
	 * @param id
	 *            the ID of the piece of work that is about to start
	 * @param tracker
	 *            the tracker to keep busy with progress based on existing
	 *            estimations
	 */
	public void begin(String id, TaskProgressTracker tracker);

	/**
	 * Informs the service that a piece of work is finished. The progress
	 * service will stop reporting progress to the tracker passed in
	 * {@link #begin(String, TaskProgressTracker)}.
	 *
	 * @param id
	 *            the ID of the piece of work that has been finished.
	 * @param status
	 *            the status that this piece of work resulted in. A new
	 *            estimation for the given ID is only stored if the status'
	 *            severity is < {@link IStatus#ERROR}.
	 */
	public void finish(String id, IStatus status);

	/**
	 * Calculates the ID that should be used for all other APIs of the service
	 * from a task instance.
	 *
	 * @param task
	 *            the task instance
	 * @return a (hopefully) unique ID for this task that can be used for
	 *         progress estimation.
	 */
	public String calculateId(Object task);

}
