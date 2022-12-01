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
package org.eclipse.tea.library.build.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.model.PluginData;
import org.eclipse.tea.library.build.model.WorkspaceData;

/**
 * Updater for the class-path of plugins.
 */
@SuppressWarnings("restriction")
public class ClasspathUpdater {

	private final WorkspaceData wsData;
	private Predicate<IProject> predicate;
	private Map<String, IPath> sourcePaths;

	/**
	 * Creates the updater on top of a {@link WorkspaceData} instance.
	 *
	 * @param wsData
	 *            workspace data
	 */
	public ClasspathUpdater(WorkspaceData wsData) {
		this.wsData = wsData;
	}

	public void setPredicate(Predicate<IProject> predicate) {
		this.predicate = predicate;
	}

	/**
	 * @param sourcePaths
	 *            a map from plugin name to source path.
	 */
	public void setSourcePaths(Map<String, IPath> sourcePaths) {
		this.sourcePaths = sourcePaths;
	}

	public void update(TaskingLog console, IProgressMonitor monitor) {
		console.info("running class-path update");
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		SubMonitor updMon = mon.newChild(70);
		updMon.beginTask("Update classpaths...", wsData.getPlugins().size());
		List<PluginData> refreshList = new ArrayList<>();
		for (PluginData pd : wsData.getPlugins()) {
			updMon.worked(1);

			final String bundleName = pd.getBundleName();
			try {
				IProject project = pd.getProject();
				if (project == null || !JavaProject.hasJavaNature(project)) {
					console.warn("skipping " + bundleName);
					continue;
				}

				if (predicate != null && !predicate.test(project)) {
					continue; // without warning - user wants us to skip.
				}

				IPluginModelBase model = PluginRegistry.findModel(project);
				if (model == null) {
					console.warn("skipping " + bundleName);
					continue;
				}

				IClasspathEntry[] entries = ClasspathComputer.getClasspath(project, model, null, true, true);

				// add source attachments where possible. PDE cannot do it,
				// because of reasons... :(
				List<IClasspathEntry> processed = new ArrayList<>();
				for (IClasspathEntry e : entries) {
					if (sourcePaths != null && !sourcePaths.isEmpty()) {
						if (e.getEntryKind() == IClasspathEntry.CPE_LIBRARY && e.getSourceAttachmentPath() == null
								&& sourcePaths.containsKey(e.getPath().lastSegment())) {
							processed.add(
									JavaCore.newLibraryEntry(e.getPath(), sourcePaths.get(e.getPath().lastSegment()),
											null, e.getAccessRules(), e.getExtraAttributes(), e.isExported()));
							continue;
						}
					}
					processed.add(e);
				}

				JavaCore.create(project).setRawClasspath(processed.toArray(new IClasspathEntry[processed.size()]),
						null);
				refreshList.add(pd);
			} catch (Exception ex) {
				console.error("cannot update " + bundleName + ": " + ex);
				ex.printStackTrace(console.warn());
			}
		}
		console.debug("refreshing " + refreshList.size() + " bundles");
		SubMonitor refMon = mon.newChild(30);
		refMon.beginTask("Refreshing plugins...", refreshList.size());
		for (PluginData pd : refreshList) {
			refMon.worked(1);
			pd.refreshProject();
		}
		console.info("update finished");
		if (monitor != null) {
			monitor.done();
		}
	}

}
