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
package org.eclipse.tea.library.build.jar;

import java.io.File;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.tea.core.services.TaskingLog;

/**
 * Factory for creating ZIP executors.
 */
@Creatable
public class ZipExecFactory {

	/**
	 * ZIP executable; null if not set
	 */
	protected final File zipExe;

	/**
	 * stream for warnings and errors
	 */
	public final TaskingLog log;

	private boolean showWarning = true;

	/**
	 * HACK: forcefully use internal zip and ignore external. the internal one
	 * is stricter and used to verify metadata propperly.
	 */
	private static boolean ignoreExternalZipExe = false;

	@Inject
	public ZipExecFactory(TaskingLog log, ZipConfig config) {
		this(log, config.zipProgramExecutable);
	}

	/**
	 * Creates the factory by (optionally) using an external ZIP application.
	 *
	 * @param log
	 *            stream for warnings and errors
	 * @param zipProgramExecutable
	 *            path and name of the ZIP application; {@code null} if we don't
	 *            have an external ZIP application
	 */
	public ZipExecFactory(TaskingLog log, String zipProgramExecutable) {
		this.log = log;

		if (zipProgramExecutable == null) {
			this.zipExe = null;
		} else {
			zipProgramExecutable = zipProgramExecutable.trim();
			if (zipProgramExecutable.isEmpty()) {
				this.zipExe = null;
			} else {
				this.zipExe = new File(zipProgramExecutable);
			}
		}
	}

	public static void setIgnoreExternalZipExe(boolean ignoreExternalZipExe) {
		ZipExecFactory.ignoreExternalZipExe = ignoreExternalZipExe;
	}

	/**
	 * Creates a fresh ZIP executor.
	 */
	public synchronized ZipExec createZipExec() {
		if (ignoreExternalZipExe) {
			return new InternalZipExec(log);
		}

		if (zipExe == null) {
			if (showWarning) {
				log.info("no ZIP application defined");
				showWarning = false;
			}
			return new InternalZipExec(log);
		}
		if (!zipExe.isFile()) {
			if (showWarning) {
				log.info("cannot find " + zipExe);
				showWarning = false;
			}
			return new InternalZipExec(log);
		}
		return new ExternalZipExec(zipExe);
	}

}
