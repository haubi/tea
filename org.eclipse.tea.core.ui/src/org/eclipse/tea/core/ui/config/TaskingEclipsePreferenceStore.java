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
package org.eclipse.tea.core.ui.config;

import java.lang.reflect.Field;

import org.eclipse.tea.core.internal.TaskingConfigurationStore;
import org.eclipse.tea.core.internal.config.PropertyConversionHelper;
import org.eclipse.tea.core.ui.Activator;

import com.google.common.base.Strings;

public class TaskingEclipsePreferenceStore implements TaskingConfigurationStore {

	@Override
	public boolean loadValue(Field property, Object target) throws IllegalAccessException {
		String value = Activator.getInstance().getPreferenceStore()
				.getString(TaskingConfigurationStore.getPropertyName(property));
		if (!Strings.isNullOrEmpty(value)) {
			property.set(target, PropertyConversionHelper.convertValue(value, property.getType()));
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "Eclipse Preferences backed PreferenceStore";
	}

}
