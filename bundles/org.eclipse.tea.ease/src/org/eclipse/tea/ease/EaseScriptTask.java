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
package org.eclipse.tea.ease;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.ease.IScriptEngine;
import org.eclipse.ease.ScriptResult;
import org.eclipse.ease.service.IScriptService;
import org.eclipse.ease.service.ScriptType;
import org.eclipse.ease.ui.scripts.repository.IRepositoryService;
import org.eclipse.ease.ui.scripts.repository.IScript;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.ui.PlatformUI;

/**
 * Task that runs an arbitrary EASE script from any of the registered script
 * locations. Use the {@link TaskListEaseScripts} to find the names that are to be
 * used (relative path from the script location).
 */
public class EaseScriptTask {

	private final String script;

	public EaseScriptTask(String script) {
		this.script = script;
	}

	@Execute
	public void runIt(TaskingLog log) throws Throwable {
		final IScriptService scriptService = PlatformUI.getWorkbench().getService(IScriptService.class);
		final IRepositoryService repoService = PlatformUI.getWorkbench().getService(IRepositoryService.class);

		log.info("Waiting for script locations to load");
		repoService.update(true);
		while (repoService.getScripts().isEmpty()) {
			Thread.sleep(100);
		}
		// we have NO way of knowing whether things are fully loaded... :|
		Thread.sleep(500);
		log.info("...loaded");

		IScript s = repoService.getScript(script);
		if (s == null) {
			throw new RuntimeException("no script named " + script);
		}

		ScriptType scriptType = scriptService.getScriptType(s.getLocation());
		IScriptEngine engine = scriptService.getEngine(scriptType.getName()).createEngine();

		engine.setErrorStream(log.error());
		engine.setOutputStream(log.info());
		engine.setCloseStreamsOnTerminate(false);

		ScriptResult result = engine.execute(s.getResource());
		result.get(); // waits and throws in case.
	}

	@Override
	public String toString() {
		return "EASE: " + script;
	}

}
