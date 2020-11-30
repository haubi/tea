/*******************************************************************************
 *  Copyright (c) 2018 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.library.build.tasks.p2;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.app.ILog;
import org.eclipse.tea.core.services.TaskingLog;

/**
 * Logger that is attached to the director application to redirect the log files
 * to the build controller and not to system out.
 */
@SuppressWarnings("restriction")
public class TaskingLogLoggerDelegate implements ILog {

	private final TaskingLog log;

	/** Creates a new logger for the given controller */
	public TaskingLogLoggerDelegate(TaskingLog log) {
		this.log = log;
	}

	@Override
	public void log(IStatus status) {
		log(status, "");
	}

	private void log(IStatus status, String indent) {
		if (status.matches(IStatus.ERROR)) {
			log.info(indent + "ERROR: " + status.getMessage(), status.getException());
		} else if (status.matches(IStatus.WARNING)) {
			log.info(indent + "WARNING: " + status.getMessage(), status.getException());
		} else if (status.matches(IStatus.INFO)) {
			log.info(indent + "INFO: " + status.getMessage(), status.getException());
		} else {
			log.info(status.getMessage(), status.getException());
		}

		for (IStatus child : status.getChildren()) {
			log(child, indent + "  ");
		}
	}

	@Override
	public void log(String message) {
		log.info(message);
	}

	@Override
	public void close() {
		// nothing to do
	}

}
