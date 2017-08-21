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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Creatable;

/**
 * Provides information about building the whole workspace.
 */
@Creatable
public class WorkspaceBuild {

	/**
	 * all complete source plugins
	 */
	protected final Map<String, PluginBuild> sourcePlugins = new TreeMap<>();

	/**
	 * all features
	 */
	protected final Map<String, FeatureBuild> features = new TreeMap<>();

	/**
	 * closed or incomplete projects
	 */
	protected final Set<String> closedOrIncomplete = new TreeSet<>();

	private final Set<PluginBuild> hostLessFragments = new TreeSet<>();

	@Inject
	public WorkspaceBuild(WorkspaceData wsData) {
		// get all complete source plugins
		for (PluginData pd : wsData.getPlugins()) {
			if (pd.hasSource() && pd.isMetadataOK()) {
				PluginBuild pb = new PluginBuild(pd);
				sourcePlugins.put(pb.getPluginName(), pb);
			} else {
				closedOrIncomplete.add(pd.getBundleName());
			}
		}

		// get features
		for (FeatureData fd : wsData.getFeatures()) {
			FeatureBuild fb = new FeatureBuild(fd, this);
			features.put(fb.getFeatureName(), fb);
		}

		// update dependencies
		for (PluginBuild pb : sourcePlugins.values()) {
			pb.updateDependencies(this);
		}
	}

	/**
	 * Returns a new sorted list of all source plugins.
	 */
	public List<PluginBuild> getSourcePlugIns() {
		List<PluginBuild> result = new ArrayList<>(sourcePlugins.values());
		return result;
	}

	/**
	 * Returns a new sorted list of all features in the workspace.
	 */
	public List<FeatureBuild> getSourceFeatures() {
		List<FeatureBuild> result = new ArrayList<>(features.values());
		return result;
	}

	/**
	 * Checks if a project is closed or has no valid meta-data.
	 *
	 * @param name
	 *            project name
	 */
	public boolean isClosedOrIncomplete(String name) {
		return closedOrIncomplete.contains(name);
	}

	/**
	 * Searches for a RCP feature.
	 *
	 * @param name
	 *            feature name
	 * @return the feature; {@code null} if we couldn't find the feature
	 */
	public FeatureBuild getFeature(String name) {
		return features.get(name);
	}

	/**
	 * Searches for a source plugin.
	 *
	 * @param name
	 *            plugin name
	 * @return the plugin; {@code null} if we couldn't find it
	 */
	public PluginBuild getSourcePlugin(String name) {
		return sourcePlugins.get(name);
	}

	/**
	 * Adds a fragment to the workspace build whos fragment host is not part of
	 * the workspace. this means we cannot determine correctly whether this
	 * fragment is required by anybody, thus we have to add it always.
	 *
	 * @param plugin
	 *            the fragment without host in the workspace.
	 */
	public void addHostLessFragment(PluginBuild plugin) {
		hostLessFragments.add(plugin);
	}

	/**
	 * Retrieves a set of all host-less fragments, meaning fragments whos host
	 * is not part of the workspace but an external plugin.
	 */
	public Set<PluginBuild> getHostLessFragments() {
		return hostLessFragments;
	}

}
