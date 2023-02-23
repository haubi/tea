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

import java.io.PrintStream;

import org.eclipse.tea.core.annotations.TaskCaptureStdOutput;
import org.eclipse.tea.core.services.TaskingLog;

/**
 * Takes care of redirecting stdout and stderr while a task is executing when
 * requested.
 */
public class OutputRedirector {

	private final TaskingLog log;
	private final TaskCaptureStdOutput annotation;
	private final PrintStream origErr;
	private final PrintStream origOut;

	public OutputRedirector(Object task, TaskingLog log) {
		this.log = log;
		this.annotation = task.getClass().getAnnotation(TaskCaptureStdOutput.class);

		this.origOut = System.out;
		this.origErr = System.err;
	}

	public void begin() {
		if (annotation == null) {
			return;
		}

		if (annotation.out()) {
			System.setOut(log.info());
		}

		if (annotation.err()) {
			if (annotation.errToOut()) {
				System.setErr(System.out);
			} else {
				System.setErr(log.error());
			}
		}
	}

	public void finish() {
		System.setOut(origOut);
		System.setErr(origErr);
	}

}
