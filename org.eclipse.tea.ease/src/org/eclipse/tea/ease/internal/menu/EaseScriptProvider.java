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
package org.eclipse.tea.ease.internal.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.ease.ui.scripts.repository.IRepositoryService;
import org.eclipse.ease.ui.scripts.repository.IScript;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.annotations.TaskChainSuppressLifecycle;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskingAdditionalMenuEntryProvider;
import org.eclipse.tea.ease.EaseScriptTask;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.component.annotations.Component;

import com.google.common.base.Splitter;

@Component
public class EaseScriptProvider implements TaskingAdditionalMenuEntryProvider {

	private static final String KEYWORD_NAME = "name";
	private static final String KEYWORD_IMAGE = "image";
	private static final String KEYWORD_TEA = "tea";
	private static final String KEYWORD_DEV = "tea-dev";
	private static final String KEYWORD_GROUPING = "tea-grouping";

	@Override
	public List<TaskingAdditionalMenuEntry> getAdditionalEntries() {
		List<TaskingAdditionalMenuEntry> allEntries = new ArrayList<>();
		IRepositoryService repoService = PlatformUI.getWorkbench().getService(IRepositoryService.class);

		for (IScript script : repoService.getScripts()) {
			Map<String, String> keywords = script.getKeywords();
			if (Boolean.parseBoolean(keywords.get(KEYWORD_TEA))) {
				String[] menuPath = null;

				String path = keywords.get(KEYWORD_NAME);
				if (path != null && !path.isEmpty() && path.contains("/")) {
					menuPath = Splitter.on('/').splitToList(path.substring(0, path.lastIndexOf('/')))
							.toArray(new String[] {});
				}

				allEntries.add(new TaskingAdditionalMenuEntry(
						new EaseSingleScriptChain(script.getPath().toString(), script.getName()), menuPath,
						keywords.get(KEYWORD_IMAGE), keywords.get(KEYWORD_GROUPING),
						Boolean.parseBoolean(keywords.get(KEYWORD_DEV))));
			}
		}

		return allEntries;
	}

	@TaskChainSuppressLifecycle
	private static final class EaseSingleScriptChain implements TaskChain {

		private final String name;
		private final String label;

		public EaseSingleScriptChain(String name, String label) {
			this.name = name;
			this.label = label;
		}

		@TaskChainContextInit
		public void init(TaskExecutionContext c) {
			c.addTask(new EaseScriptTask(name));
		}

		@Override
		public String toString() {
			return label;
		}

	}

}
