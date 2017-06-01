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
package org.eclipse.tea.core.internal.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;

import org.eclipse.tea.core.internal.TaskingConfigurationStore;

import com.google.common.base.Strings;

/**
 * Configuration Store that reads configuration values purely from a property
 * file.
 */
public class PropertyConfigurationStore implements TaskingConfigurationStore {

	private final Properties properties;

	public PropertyConfigurationStore(File file) throws IOException {
		properties = new Properties();
		try (InputStream is = new FileInputStream(file)) {
			properties.load(is);
		}

		properties.put("configurationSource", file.getAbsolutePath());
	}

	@Override
	public boolean loadValue(Field property, Object target) throws IllegalAccessException {
		String value = properties.getProperty(TaskingConfigurationStore.getPropertyName(property));
		if (!Strings.isNullOrEmpty(value)) {
			property.set(target, PropertyConversionHelper.convertValue(value, property.getType()));
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "Property file backed PreferenceStore";
	}

}
