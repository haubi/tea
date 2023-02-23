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
package org.eclipse.tea.core.services;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Marker interface that identifies parties that need to contribute to the
 * statistical data that can be posted to a server by TEA
 */
public interface TaskingStatisticsContribution {

	/**
	 * Annotates a method that may want to contribute to the tasking statistics
	 * posted to a configured server. The method must return a single object
	 * that can be serialized to JSON
	 */
	@Qualifier
	@Documented
	@Retention(RUNTIME)
	@Target(METHOD)
	public @interface TaskingStatisticProvider {
		public String qualifier() default "";
	}

}
