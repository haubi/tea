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
package org.eclipse.tea.library.build.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.PreferenceFilterEntry;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tea.core.annotations.TaskReloadConfiguration;
import org.eclipse.tea.core.services.TaskingLog;
import org.osgi.service.prefs.BackingStoreException;

@TaskReloadConfiguration
public class TaskImportPreferences {

	private final File file;
	private final boolean force;

	public TaskImportPreferences(File file, boolean force) {
		this.file = file;
		this.force = force;
	}

	@Execute
	public void importFile(TaskingLog log) {
		try {
			InputStream in = new FileInputStream(file.getAbsolutePath());
			IPreferencesService preferencesService = Platform.getPreferencesService();
			IExportedPreferences readPreferences = preferencesService.readPreferences(in);

			PreferenceVisitor visitor = new PreferenceVisitor();
			readPreferences.accept(visitor);

			IPreferenceFilter filter = new IPreferenceFilter() {

				@Override
				public String[] getScopes() {
					String[] scopes = new String[visitor.filteredNodes.keySet().size()];
					scopes = visitor.filteredNodes.keySet().toArray(scopes);
					return scopes;
				}

				@Override
				public Map<String, PreferenceFilterEntry[]> getMapping(String scope) {
					return visitor.filteredNodes.get(scope);
				}

			};
			Display.getDefault().syncExec(() -> {
				try {
					preferencesService.applyPreferences(readPreferences, new IPreferenceFilter[] { filter });
				} catch (CoreException e) {
					log.error("Error importing preferences: " + file, e);
				}
			});
		} catch (Exception e) {
			log.error("Error importing preferences: " + file, e);
		}
	}

	private class PreferenceVisitor implements IPreferenceNodeVisitor {

		public Map<String, Map<String, PreferenceFilterEntry[]>> filteredNodes = new HashMap<>();

		public PreferenceVisitor() {
			filteredNodes.put(InstanceScope.SCOPE, new HashMap<String, PreferenceFilterEntry[]>());
			filteredNodes.put(ConfigurationScope.SCOPE, new HashMap<String, PreferenceFilterEntry[]>());
		}

		private List<IEclipsePreferences> getPath(IEclipsePreferences leaf) {
			List<IEclipsePreferences> result = new ArrayList<>();
			IEclipsePreferences current = leaf;
			while (current != null) {
				result.add(current);
				current = (IEclipsePreferences) current.parent();
			}
			Collections.reverse(result);
			if (result.size() < 2) {
				return Collections.emptyList();
			}
			return result.subList(2, result.size());
		}

		private IEclipsePreferences getScopeNode(IScopeContext scope, List<IEclipsePreferences> path) {
			IEclipsePreferences node = scope.getNode(path.get(0).name());
			for (int i = 1; i < path.size(); i++) {
				node = (IEclipsePreferences) node.node(path.get(i).name());
			}

			return node;
		}

		private String getPathString(List<IEclipsePreferences> path) {
			String pathString = "";
			for (IEclipsePreferences node : path) {
				pathString += node.name() + "/";
			}
			pathString = pathString.substring(0, pathString.length() - 1);
			return pathString;
		}

		@Override
		public boolean visit(IEclipsePreferences node) throws BackingStoreException {
			if (node.keys().length == 0) {
				return true;
			}

			List<IEclipsePreferences> path = getPath(node);
			if (path.isEmpty()) {
				return true;
			}

			IScopeContext setContext = null;
			if (node.absolutePath().startsWith("/" + InstanceScope.SCOPE)) {
				setContext = InstanceScope.INSTANCE;
			} else if (node.absolutePath().startsWith("/" + ConfigurationScope.SCOPE)) {
				setContext = ConfigurationScope.INSTANCE;
			} else {
				return true;
			}

			IEclipsePreferences setNode = getScopeNode(setContext, path);
			IEclipsePreferences defNode = getScopeNode(DefaultScope.INSTANCE, path);

			List<PreferenceFilterEntry> preferenceFilterEntries = new ArrayList<>();
			for (String key : node.keys()) {
				String setValue = setNode.get(key, null);
				String defValue = defNode.get(key, null);

				if (setValue != null && !force) {
					if (defValue != null && setValue.equals(defValue)) {
						preferenceFilterEntries.add(new PreferenceFilterEntry(key));
					}
				} else {
					preferenceFilterEntries.add(new PreferenceFilterEntry(key));
				}

			}

			if (preferenceFilterEntries.size() > 0) {
				PreferenceFilterEntry[] preferenceFilters = new PreferenceFilterEntry[preferenceFilterEntries.size()];
				preferenceFilters = preferenceFilterEntries.toArray(preferenceFilters);
				filteredNodes.get(setContext.getName()).put(getPathString(path), preferenceFilters);
			}

			return true;
		}

	}

	@Override
	public String toString() {
		return "Import Preferences from " + file;
	}

}
