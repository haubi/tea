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

/**
 * Aids in converting values from configuration store values to supported field
 * types.
 */
public class PropertyConversionHelper {

	public static Object convertValue(String value, Class<?> targetType) {
		if (targetType == String.class) {
			return value;
		} else if (targetType == Integer.class || targetType == int.class) {
			return Integer.parseInt(value);
		} else if (targetType == Long.class || targetType == long.class) {
			return Long.parseLong(value);
		} else if (targetType == Boolean.class || targetType == boolean.class) {
			return Boolean.parseBoolean(value);
		} else if (Enum.class.isAssignableFrom(targetType)) {
			try {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				Enum valueOf = Enum.valueOf((Class<Enum>) targetType, value);
				return valueOf;
			} catch (Exception e) {
				throw new RuntimeException("Unsupported field type: " + targetType, e);
			}
		}

		throw new RuntimeException("Unsupported field type: " + targetType);
	}

}
