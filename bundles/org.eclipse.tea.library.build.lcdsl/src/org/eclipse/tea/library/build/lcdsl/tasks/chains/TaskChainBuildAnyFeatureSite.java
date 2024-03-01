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
package org.eclipse.tea.library.build.lcdsl.tasks.chains;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.annotations.TaskChainMenuEntry;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.core.ui.annotations.TaskChainUiInit;
import org.eclipse.tea.library.build.lcdsl.tasks.p2.TaskGenFeatureFromLcDsl;
import org.eclipse.tea.library.build.menu.BuildLibraryMenuDecoration;
import org.eclipse.tea.library.build.menu.OtherMenuDecoration;
import org.eclipse.tea.library.build.model.FeatureBuild;
import org.eclipse.tea.library.build.model.WorkspaceBuild;
import org.eclipse.tea.library.build.model.WorkspaceData;
import org.eclipse.tea.library.build.tasks.jar.TaskRunFeaturePluginJarExport;
import org.eclipse.tea.library.build.tasks.p2.TaskPublishFeatureUpdateSite;
import org.eclipse.tea.library.build.tasks.p2.TaskUpdateStrictFeatureVersions;
import org.eclipse.tea.library.build.ui.SelectProjectDialog;
import org.osgi.service.component.annotations.Component;

@TaskChainId(description = "Create Update Site from Feature...")
@TaskChainMenuEntry(path = { OtherMenuDecoration.MENU_OTHER,
		BuildLibraryMenuDecoration.MENU_BUILD }, groupingId = BuildLibraryMenuDecoration.GROUP_MISC, icon = "icons/site_obj.png")
@Component
public class TaskChainBuildAnyFeatureSite implements TaskChain {

	private static final String KEY_FEATURE = "feature_to_export";
	private static final String KEY_SITE_NAME = "feature_site_name";
	private static final String KEY_STRICT = "feature_strict_versions";

	@TaskChainUiInit
	public void selectFeature(IEclipseContext context, Shell parentShell, WorkspaceBuild wb) {
		SelectProjectDialog dlg = new SelectProjectDialog(parentShell, "Select feature to export",
				"Select a feature that should be exported", true, new ViewerFilter() {

					@Override
					public boolean select(Viewer viewer, Object parentElement, Object element) {
						if (element instanceof IProject) {
							return WorkspaceData.isFeatureProject((IProject) element);
						}
						return false;
					}
				});
		if ((dlg.open() == SelectProjectDialog.OK)) {
			IProject[] ps = dlg.getMultiResult();
			context.set(KEY_FEATURE,
					Arrays.stream(ps).map(e -> wb.getFeature(e.getName())).collect(Collectors.toList()));
		} else {
			throw new OperationCanceledException();
		}

		// TODO prompt update site name, strict mode
		context.set(KEY_SITE_NAME, "dynamic_site");
		context.set(KEY_STRICT, Boolean.TRUE);
	}

	@TaskChainContextInit
	public void init(TaskExecutionContext c, TaskingLog log, @Named(KEY_FEATURE) List<FeatureBuild> fbs,
			@Named(KEY_SITE_NAME) String name, @Named(KEY_STRICT) Boolean strict) {
		boolean first = true;
		List<TaskUpdateStrictFeatureVersions> updates = new ArrayList<>();
		for (FeatureBuild fb : fbs) {
			File featureDir = fb.getData().getBundleDir();
			File propFile = new File(featureDir, "content.properties");
			if (propFile.exists()) {
				Properties p = new Properties();
				try (FileInputStream fis = new FileInputStream(propFile)) {
					p.load(fis);
				} catch (IOException e) {
					e.printStackTrace(log.error());
				}
				if (p.containsKey("dependencies")) {
					c.addTask(new TaskGenFeatureFromLcDsl(fb.getFeatureName(), true));
				}
			}

			if (strict) {
				c.addTask(TaskUpdateStrictFeatureVersions.create(fb.getFeatureName(), updates));
			}

			c.addTask(new TaskRunFeaturePluginJarExport(fb.getFeatureName(), !first));
			first = false;
		}
		c.addTask(new TaskPublishFeatureUpdateSite(name,
				fbs.stream().map(e -> e.getFeatureName()).collect(Collectors.toList())));

		if (strict) {
			updates.forEach(u -> c.addTask(u.restore()));
		}
	}

}
