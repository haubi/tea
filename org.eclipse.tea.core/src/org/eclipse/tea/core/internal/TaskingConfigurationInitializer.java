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
package org.eclipse.tea.core.internal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.Service;
import org.eclipse.tea.core.services.TaskingConfigurationExtension;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfig;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfigProperty;
import org.eclipse.tea.core.services.TaskingLog;

/**
 * Responsible for loading the configuration from the store, and publishing
 * values in configuration instances
 */
public class TaskingConfigurationInitializer {

	@Execute
	protected void initConfiguration(IEclipseContext context, TaskingConfigurationStore store,
			@Service List<TaskingConfigurationExtension> configurations) throws Exception {
		List<String> warnings = new ArrayList<>();
		Map<String, TaskingConfig> keys = new TreeMap<>();
		for (TaskingConfigurationExtension config : configurations) {
			// create a fresh instance of the configuration just for this engine
			// This would be better to do it with PROTOTYPE policy on the
			// service, but this is not available in DS 1.2
			config = config.getClass().getDeclaredConstructor().newInstance();

			// register configuration for injection into task chain and task
			context.set(config.getClass().getName(), config);

			TaskingConfig cfg = config.getClass().getAnnotation(TaskingConfig.class);
			if (cfg == null) {
				warnings.add("Configuration without @TeaConfig annotation: " + config);
			}

			for (Field field : config.getClass().getDeclaredFields()) {
				if (field.getAnnotation(TaskingConfigProperty.class) == null) {
					continue;
				}

				String key = TaskingConfigurationStore.getPropertyName(field);
				TaskingConfig old = keys.put(key, cfg);
				if (old != null) {
					warnings.add("duplicate configuration key '" + key + "' defined in " + old.description()
							+ " and in " + cfg.description());
				}

				// it is a configuration property. initialize it's value.
				try {
					store.loadValue(field, config);
				} catch (Exception e) {
					warnings.add("cannot load configuration value for " + field + " in " + config);
					e.printStackTrace();
				}
			}
		}

		// need to make the log here in case it has configuration
		TaskingLog log = context.get(TaskingLog.class);
		for (String w : warnings) {
			log.warn(w);
		}
	}

}
