/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package org.eclipse.tea.ease;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.ease.ui.scripts.repository.IRepositoryService;
import org.eclipse.ease.ui.scripts.repository.IScript;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.ui.PlatformUI;

public class TaskListEaseScripts {

	@Execute
	public void list(TaskingLog log) throws Exception {
		final IRepositoryService repo = PlatformUI.getWorkbench().getService(IRepositoryService.class);

		log.info("Waiting for script locations to load");
		repo.update(true);
		while (repo.getScripts().isEmpty()) {
			Thread.sleep(100);
		}
		// we have NO way of knowing whether things are fully loaded... :|
		Thread.sleep(500);
		log.info("...loaded");

		for (IScript script : repo.getScripts()) {
			log.info(script.getPath().toString());
		}
	}

}
