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
package org.eclipse.tea.library.build.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.services.TeaBuildVersionService;

/**
 * Stores useful information about OSGi bundles of an Eclipse workspace.
 */
@SuppressWarnings("restriction")
@Creatable
public class WorkspaceData {

	private static final String JAVA_NATURE = "org.eclipse.jdt.core.javanature";

	private final List<IProject> unknownProjects = new ArrayList<>();
	private final Map<String, PluginData> plugins = new TreeMap<>();
	private final Map<String, FeatureData> features = new TreeMap<>();

	@Inject
	public WorkspaceData(TaskingLog console, TeaBuildVersionService bvService) {
		this(ResourcesPlugin.getWorkspace(), console, bvService);
	}

	/**
	 * Creates the workspace data for a source distribution.
	 *
	 * @param workspace
	 *            Eclipse workspace; may not be empty
	 * @param console
	 *            Tasking log destination
	 * @param bvService
	 *            provider for build version relevant information
	 */
	public WorkspaceData(IWorkspace workspace, TaskingLog console, TeaBuildVersionService bvService) {
		final IWorkspaceRoot root = workspace.getRoot();
		for (IProject project : root.getProjects()) {
			try {
				if (!project.exists()) {
					console.error("project doesn't exist: " + project.getName());
					continue;
				}
				if (!project.isOpen()) {
					continue;
				}

				final IProjectDescription desc;
				try {
					desc = project.getDescription();
				} catch (org.eclipse.core.internal.resources.ResourceException ex) {
					// Resource does not exist. - project is still opening
					console.warn("Failed to check project " + project.getName() + "", ex);
					continue;
				} catch (CoreException ex) {
					throw new IllegalStateException(ex);
				}
				if (isPluginProject(project)) {
					PluginData pd = new PluginData(project);
					plugins.put(pd.getBundleName(), pd);
					continue;
				}
				if (isFeatureProject(project)) {
					FeatureData fd = new FeatureData(project, bvService);
					features.put(fd.getBundleName(), fd);
					continue;
				}

				if (desc.hasNature(JAVA_NATURE)) {
					if (!org.eclipse.pde.internal.core.SearchablePluginsManager.PROXY_PROJECT_NAME
							.equals(project.getName())) {
						console.warn("skipping plain Java project: " + project.getName());
					}
				}
				unknownProjects.add(project);

			} catch (Throwable t) {
				console.error(t.getMessage(), t);
				throw t;
			}
		}

	}

	/**
	 * @param project
	 *            the project to check
	 * @return <code>true</code> if project is binary, <code>false</code> if it
	 *         is source.
	 */
	public static boolean isBinaryProject(IProject project) {
		return WorkspaceModelManager.isBinaryProject(project);
	}

	/**
	 * @param project
	 *            the project to check
	 * @return <code>true</code> if project is a feature, <code>false</code> if
	 *         not.
	 */
	public static boolean isFeatureProject(IProject project) {
		// DON'T use the WorkspaceModelManager. It will check for the existence
		// of a feature.xml,
		// which may not exist for features that are yet to be generated. Rather
		// check the nature.
		// return WorkspaceModelManager.isFeatureProject(project);

		try {
			return project.hasNature("org.eclipse.pde.FeatureNature");
		} catch (CoreException e) {
			org.eclipse.tea.library.build.internal.Activator.log(IStatus.WARNING,
					"cannot determine feature nature for " + project, null);
			return false;
		}
	}

	/**
	 * @param project
	 *            the project to check
	 * @return <code>true</code> if project is a plugin, <code>false</code> if
	 *         not.
	 */
	public static boolean isPluginProject(IProject project) {
		return WorkspaceModelManager.isPluginProject(project);
	}

	/**
	 * Will look in the workspace for a project named <code>projectName</code>
	 * and return it in the form of an <code>IProject</code>
	 *
	 * @param projectName
	 *            name of the project to look for
	 * @return an <code>IProject</code> representation of the found project or
	 *         <code>null</code> if the project does not exist in the workspace
	 */
	public static IProject getProject(final String projectName) {
		if (projectName == null) {
			return null;
		}
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject project = root.getProject(projectName);
		if (project != null && project.exists()) {
			return project;
		}
		return null;
	}

	/**
	 * Returns all plugin projects.
	 */
	public Collection<PluginData> getPlugins() {
		return plugins.values();
	}

	/**
	 * Returns all feature projects.
	 */
	public Collection<FeatureData> getFeatures() {
		return features.values();
	}

	/**
	 * Returns all Eclipse projects, which don't have the plugin nature.
	 */
	public Collection<IProject> getUnknownProjects() {
		return unknownProjects;
	}

	/**
	 * Searches for a RCP feature.
	 *
	 * @param name
	 *            feature name
	 * @return the feature; {@code null} if we couldn't find the feature
	 */
	public FeatureData getFeature(String name) {
		return features.get(name);
	}

	/**
	 * Searches for a RCP plugin.
	 *
	 * @param name
	 *            plugin name
	 * @return the plugin; {@code null} if we couldn't find the plugin
	 */
	public PluginData getPlugin(String name) {
		return plugins.get(name);
	}

}
