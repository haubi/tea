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
package org.eclipse.tea.library.build.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.PDECore;
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
	private Map<String, IPath> sourcePaths = new HashMap<>();

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
		this.sourcePaths = sourcePaths != null ? sourcePaths : new HashMap<>();
	}

	public void update(TaskingLog console, IProgressMonitor monitor) {
		console.info("running class-path update");
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		SubMonitor updMon = mon.newChild(70);
		updMon.beginTask("Update classpaths...", wsData.getPlugins().size());
		List<PluginData> refreshList = new ArrayList<>();
		for (PluginData pd : wsData.getPlugins()) {
			updMon.checkCanceled();
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

				// PDE may fail to update the classpath based on the original,
				// see https://github.com/eclipse-pde/eclipse.pde/pull/497
				// But calculating from scratch drops existing attributes and
				// destroys the existing order.
				// Workaround here merges fresh classpath with the original one.
				IClasspathEntry[] origCP = JavaCore.create(project).getRawClasspath();
				IClasspathEntry[] freshCP = ClasspathComputer.getClasspath(project, model, null, true, true);

				// remove trailing slash from source path if any:
				for (int i = 0; i < freshCP.length; i++) {
					IClasspathEntry old = freshCP[i];
					if (old.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						IPath path = old.getPath().removeTrailingSeparator();
						freshCP[i] = JavaCore.newSourceEntry(path, old.getInclusionPatterns(),
								old.getExclusionPatterns(), old.getOutputLocation(), old.getExtraAttributes());
					}
				}

				Map<IPath, IClasspathEntry> collectedCPofPath = Stream.concat(Stream.of(freshCP), Stream.of(origCP))
						.collect(Collectors.toMap(e -> keyOf(e), e -> e, (first, dupe) -> first));

				List<IClasspathEntry> mergedCP = new ArrayList<>();
				for (final IClasspathEntry orig : origCP) {
					final IClasspathEntry collect = collectedCPofPath.remove(keyOf(orig));
					if (collect == null) {
						continue; // dupe in the original already
					}
					IClasspathEntry merged = collect;
					switch (collect.getEntryKind()) {
					case IClasspathEntry.CPE_LIBRARY:
						if (collect.getContentKind() == IPackageFragmentRoot.K_BINARY) {
							IPath source = sourcePaths.get(collect.getPath().lastSegment()); // override
							source = source != null ? source : orig.getSourceAttachmentPath(); // current
							source = source != null ? source : collect.getSourceAttachmentPath(); // default
							if (!collect.equals(orig) || !Objects.equals(collect.getSourceAttachmentPath(), source)) {
								merged = JavaCore.newLibraryEntry(collect.getPath(), source, null,
										orig.getAccessRules(), orig.getExtraAttributes(), orig.isExported());
							}
						}
						break;
					case IClasspathEntry.CPE_SOURCE:
						merged = orig;
						break;
					case IClasspathEntry.CPE_CONTAINER:
						if (!collect.equals(orig)) {
							merged = JavaCore.newContainerEntry(collect.getPath(), orig.getAccessRules(),
									orig.getExtraAttributes(), orig.isExported());
						}
						break;
					case IClasspathEntry.CPE_PROJECT:
						if (!collect.equals(orig)) {
							merged = JavaCore.newProjectEntry(collect.getPath(), orig.getAccessRules(),
									orig.combineAccessRules(), orig.getExtraAttributes(), orig.isExported());
						}
						break;
					case IClasspathEntry.CPE_VARIABLE:
						merged = orig;
						break;
					}
					mergedCP.add(merged);
				}

				for (IClasspathEntry fresh : freshCP) {
					if (collectedCPofPath.remove(keyOf(fresh)) == null) {
						continue; // not a new one
					}
					if (fresh.getEntryKind() == IClasspathEntry.CPE_LIBRARY
							&& fresh.getContentKind() == IPackageFragmentRoot.K_BINARY) {
						IPath source = sourcePaths.get(fresh.getPath().lastSegment());
						if (source != null && !source.equals(fresh.getSourceAttachmentPath())) {
							fresh = JavaCore.newLibraryEntry(fresh.getPath(), source, null, fresh.getAccessRules(),
									fresh.getExtraAttributes(), fresh.isExported());
						}
					}
					mergedCP.add(fresh);
				}

				JavaCore.create(project).setRawClasspath(mergedCP.toArray(new IClasspathEntry[mergedCP.size()]), null);
				refreshList.add(pd);
			} catch (Exception ex) {
				console.error("skipped classpath update for " + bundleName + " because of: " + ex);
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

	private static IPath keyOf(IClasspathEntry entry) {
		if (PDECore.JRE_CONTAINER_PATH.isPrefixOf(entry.getPath())) {
			return PDECore.JRE_CONTAINER_PATH; // entry path contains properties
		}
		return entry.getPath();
	}

}
