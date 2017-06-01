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
package org.eclipse.tea.core.internal;

import java.lang.reflect.Field;

import org.eclipse.tea.core.services.TaskingConfigurationExtension;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfigProperty;

import com.google.common.base.Strings;

/**
 * Encapsulates loading {@link TaskingConfigProperty} annotated fields on
 * {@link TaskingConfigurationExtension}s.
 */
@FunctionalInterface
public interface TaskingConfigurationStore {

	/**
	 * Updates the value of the given Field with the actual value of from the
	 * configuration store.
	 * <p>
	 * Implementors decide how to load/convert values.
	 * <p>
	 * The {@link Field} is guaranteed to have the {@link TaskingConfigProperty}
	 * annotation which can be used to determine the actual property name,
	 * default value, etc.
	 * <p>
	 * The type of the property must be derived from the {@link Field} itself.
	 *
	 * @return whether a value has been found or not.
	 */
	public boolean loadValue(Field property, Object target) throws IllegalAccessException;

	/**
	 * Calculate the actual property name for an annotated field.
	 */
	public static String getPropertyName(Field target) {
		TaskingConfigProperty prop = target.getAnnotation(TaskingConfigProperty.class);
		if (Strings.isNullOrEmpty(prop.name())) {
			return target.getName();
		}
		return prop.name();
	}

}
