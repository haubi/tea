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
package org.eclipse.tea.core.ui.internal.menu;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.tea.core.TaskingEngine;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.internal.config.TaskingDevelopmentConfig;
import org.eclipse.tea.core.internal.model.TaskingModel;
import org.eclipse.tea.core.internal.model.iface.TaskingContainer;
import org.eclipse.tea.core.internal.model.iface.TaskingElement;
import org.eclipse.tea.core.internal.model.iface.TaskingItem;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.ui.TaskingEngineJob;
import org.eclipse.tea.core.ui.config.TaskingEclipsePreferenceStore;
import org.eclipse.tea.core.ui.internal.TaskingImageHelper;

public class DynamicTaskingMenu {
	@Inject
	private TaskingModel model;

	@AboutToShow
	public void aboutToShow(List<MMenuElement> items) {
		IEclipseContext configuredContext = TaskingInjectionHelper
				.createConfiguredContext(new TaskingEclipsePreferenceStore());
		try {
			TaskingDevelopmentConfig cfg = configuredContext.get(TaskingDevelopmentConfig.class);

			createMenu(cfg, items, model.getRootGroup());
		} finally {
			configuredContext.dispose();
		}
	}

	private void createMenu(TaskingDevelopmentConfig cfg, List<MMenuElement> target, TaskingContainer group) {
		String lastGroupingId = null;
		for (TaskingElement e : group.getChildren()) {
			if (e.isDevelopment() && !cfg.showHiddenTaskChains) {
				continue;
			}

			// create separators per menu whenever the grouping switches.
			if (lastGroupingId != null && !lastGroupingId.equals(e.getGroupingId())) {
				// group change. add separator.
				target.add(MMenuFactory.INSTANCE.createMenuSeparator());
			}
			lastGroupingId = e.getGroupingId();

			if (e instanceof TaskingContainer) {
				MMenu sub = MMenuFactory.INSTANCE.createMenu();
				sub.setLabel(getLabel(cfg, e));
				sub.setIconURI(TaskingImageHelper.getIconUri(e.getIconBundle(), e.getIconPath()));
				target.add(sub);

				createMenu(cfg, sub.getChildren(), (TaskingContainer) e);
			} else if (e instanceof TaskingItem && ((TaskingItem) e).isVisibleInMenu()) {
				MDirectMenuItem item = MMenuFactory.INSTANCE.createDirectMenuItem();
				item.setLabel(getLabel(cfg, e));
				item.setIconURI(TaskingImageHelper.getIconUri(e.getIconBundle(), e.getIconPath()));
				item.setObject(new DynamicTaskChainMenuHandler(((TaskingItem) e).getChain()));
				target.add(item);
			}
		}
	}

	private String getLabel(TaskingDevelopmentConfig config, TaskingElement element) {
		if (!config.showGroupingIds) {
			return element.getLabel();
		}
		return "[" + element.getGroupingId() + "] " + element.getLabel();
	}

	public static final class DynamicTaskChainMenuHandler {

		private final TaskChain chain;

		public DynamicTaskChainMenuHandler(TaskChain chain) {
			this.chain = chain;
		}

		@Execute
		public void handle() {
			TaskingEngine engine = TaskingEngine.withConfiguration(new TaskingEclipsePreferenceStore());
			Job j = new TaskingEngineJob(engine, chain);
			j.schedule();
		}

		@CanExecute
		public boolean checkRunning() {
			return Job.getJobManager().find(TaskChain.class).length == 0;
		}

	}
}