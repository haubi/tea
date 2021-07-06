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
package org.eclipse.tea.core.ui.config;

import java.lang.reflect.Field;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.di.extensions.Service;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.internal.TaskingConfigurationStore;
import org.eclipse.tea.core.services.TaskingConfigurationExtension;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfigProperty;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.core.ui.Activator;

/**
 * Initializes the Eclipse preference store from the default values in the
 * configuration fields.
 */
public class TaskingPreferenceInitializer extends AbstractPreferenceInitializer {

	@Inject
	@Service
	private List<TaskingConfigurationExtension> extensions;

	@Inject
	private TaskingLog log;

	public TaskingPreferenceInitializer() {
		ContextInjectionFactory.inject(this,
				TaskingInjectionHelper.createConfiguredContext(new TaskingEclipsePreferenceStore()));
	}

	@Override
	public void initializeDefaultPreferences() {
		for (TaskingConfigurationExtension ext : extensions) {
			for (Field f : ext.getClass().getDeclaredFields()) {
				TaskingConfigProperty config = f.getAnnotation(TaskingConfigProperty.class);
				if (config == null) {
					continue;
				}

				String propertyName = TaskingConfigurationStore.getPropertyName(f);

				try {
					Object v = f.get(ext);

					if (v == null) {
						continue;
					}

					if (f.getType().equals(String.class)) {
						Activator.getInstance().getPreferenceStore().setDefault(propertyName, (String) v);
					} else if (f.getType().equals(Integer.class) || f.getType().equals(int.class)) {
						Activator.getInstance().getPreferenceStore().setDefault(propertyName, (Integer) v);
					} else if (f.getType().equals(Long.class) || f.getType().equals(long.class)) {
						Activator.getInstance().getPreferenceStore().setDefault(propertyName, (Long) v);
					} else if (f.getType().equals(Boolean.class) || f.getType().equals(boolean.class)) {
						Activator.getInstance().getPreferenceStore().setDefault(propertyName, (Boolean) v);
					} else if (Enum.class.isAssignableFrom(f.getType())) {
						Activator.getInstance().getPreferenceStore().setDefault(propertyName, ((Enum<?>) v).name());
					} else {
						throw new RuntimeException("Unsupported configuration property type" + f.getType());
					}
				} catch (Exception e) {
					log.error("Error while initializing default preferences", e);
				}
			}
		}
	}

}
