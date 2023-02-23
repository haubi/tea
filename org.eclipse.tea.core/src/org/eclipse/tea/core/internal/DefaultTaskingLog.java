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

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.internal.config.TaskingDevelopmentConfig;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.core.services.TaskingLog.TaskingLogQualifier;
import org.osgi.service.component.annotations.Component;

/**
 * Default log implementation that simply logs to a single stream. The
 * destination can be changed by providing a named {@link PrintStream}
 * ({@link TaskingInjectionHelper#CTX_OUTPUT}).
 */
@Component(service = TaskingLog.class)
@TaskingLogQualifier(headless = true)
public class DefaultTaskingLog extends TaskingLog {

	private PrintStream target;

	@Override
	public PrintStream debug() {
		return target;
	}

	@Override
	public PrintStream info() {
		return target;
	}

	@Override
	public PrintStream warn() {
		return target;
	}

	@Override
	public PrintStream error() {
		return target;
	}

	@TaskingLogInit
	public void initLog(@Optional @Named(TaskingInjectionHelper.CTX_OUTPUT) PrintStream output,
			TaskingDevelopmentConfig devConfig) {
		setShowDebug(devConfig.showDebugLogs);

		if (output == null) {
			output = System.out;
		}

		target = output;
	}

}
