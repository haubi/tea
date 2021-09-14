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
package org.eclipse.tea.library.build.chain.plugin;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.core.ui.internal.listeners.AutoBuildDeactivator;
import org.eclipse.tea.library.build.chain.TeaBuildChain;
import org.eclipse.tea.library.build.chain.TeaBuildElement;
import org.eclipse.tea.library.build.config.TeaBuildConfig;
import org.eclipse.tea.library.build.internal.Activator;
import org.eclipse.tea.library.build.services.TeaBuildVisitor;
import org.eclipse.tea.library.build.services.TeaElementFailurePolicy.FailurePolicy;
import org.eclipse.tea.library.build.util.TeaBuildUtil;
import org.osgi.service.component.annotations.Component;

/**
 * Visits all {@link TeaBuildPluginElement} and compiles the according
 * underlying plugin.
 * <p>
 * Uses {@link TeaBuildConfig#batchCompile} to determine whether to batch
 * compile plugins that have no dependencies between each other.
 * <p>
 * Note that batch compile will severely reduce the usefulness of the returned
 * results, and also disable retry on project compile error.
 */
@Component
public class TeaBuildPluginVisitor implements TeaBuildVisitor {

	private TaskingLog log;
	private TeaBuildConfig config;
	private TaskProgressTracker tracker;

	@Execute
	public void prepare(TaskingLog log, TaskProgressTracker tracker, TeaBuildConfig config) {
		this.log = log;
		this.tracker = tracker;
		this.config = config;
	}

	@Override
	public Map<TeaBuildElement, IStatus> visit(List<TeaBuildElement> elements) {
		Map<TeaBuildElement, IStatus> results = new TreeMap<>();

		if (config.batchCompile) {
			// Step 1: find all plugins and their projects
			Map<IProject, TeaBuildPluginElement> projects = elements.stream()
					.filter(e -> e instanceof TeaBuildPluginElement).map(e -> (TeaBuildPluginElement) e)
					.collect(Collectors.toMap(e -> e.getPlugin().getData().getProject(), Function.identity()));

			log.info("compile " + projects.size() + " plugins in group.");

			// Step 2: compile all projects as a single batch to avoid multiple
			// locks/events
			try {
				TeaBuildUtil.tryCompile(projects.keySet());
				projects.values().stream().forEach(e -> {
					results.put(e, Status.OK_STATUS);
					AutoBuildDeactivator.avoidBuild(e.getPlugin().getData().getProject());
				});
			} catch (Exception ex) {
				projects.values().stream().forEach(e -> results.put(e,
						new Status(IStatus.ERROR, Activator.PLUGIN_ID, "failed to batch compile group", ex)));
			}
		} else {
			// Step 1: compile all projects, each by itself
			for (TeaBuildElement e : elements) {
				if (e instanceof TeaBuildPluginElement) {
					TeaBuildPluginElement p = (TeaBuildPluginElement) e;
					tracker.setTaskName(p.getName());
					if (!p.isAllDependenciesBuilt()) {
						results.put(p, Status.CANCEL_STATUS);
						log.warn("skipping " + p.getName() + " due to errors in dependencies.");
					} else {
						IStatus s = TeaBuildUtil.tryCompile(log, tracker, p, config);
						if (s.getSeverity() > IStatus.WARNING) {
							p.error();
							results.put(p, s);
							if (TeaBuildChain.getFailurePolicyFor(p) == FailurePolicy.ABORT_IMMEDIATE) {
								break; // fail fast
							}
						} else {
							p.done();
							results.put(p, Status.OK_STATUS);
							AutoBuildDeactivator.avoidBuild(p.getPlugin().getData().getProject());
						}
					}
				}
			}
		}

		// Step 3: return all results
		return results;
	}

}
