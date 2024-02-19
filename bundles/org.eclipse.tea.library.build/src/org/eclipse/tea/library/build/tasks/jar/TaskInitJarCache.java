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
package org.eclipse.tea.library.build.tasks.jar;

import java.io.File;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskingLog;

public class TaskInitJarCache {

	private final File directory;

	public TaskInitJarCache(File directory) {
		this.directory = directory;
	}

	@Execute
	public void run() {
		TaskRunJarExport.initCache(directory);
	}

	@Override
	public String toString() {
		return "Initialize Cache (" + directory + ")";
	}

	public Object getCleanup() {
		return new Object() {
			@Execute
			public void run(TaskingLog log) {
				TaskRunJarExport.cleanCache(log);
			}

			@Override
			public String toString() {
				return "Clean Cache (" + directory + ")";
			}
		};
	}

}
