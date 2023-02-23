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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import org.eclipse.tea.core.TaskingEngine;

/**
 * Service interface for contributors. Provides fields annotated with the
 * {@link TaskingConfigProperty} annotation. Those fields will be initialized by
 * the {@link TaskingEngine} on startup.
 * <p>
 * NOTE: in declarative services 1.2 it is not possible to create a fresh
 * instance of the service on each request. Implementors must be aware of this
 * limitation. With DS 1.3 the PROTOTYPE policy should be used to create fresh
 * configuration instances for each {@link TaskingEngine}.
 */
public interface TaskingConfigurationExtension {

	@Documented
	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface TaskingConfig {
		String description();
	}

	/**
	 * Annotates a configuration property for TEA. The possible types of the
	 * field are restricted to {@link String}, {@link Boolean} and {@link Long}.
	 */
	@Documented
	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface TaskingConfigProperty {
		/**
		 * @return a human readable description of the property for
		 *         configuration interfaces
		 */
		String description();

		/**
		 * @return the name of the property in the underlying store/file. if
		 *         this is not set, the name of the field this property is
		 *         applied to is used instead.
		 */
		String name() default "";

		/**
		 * @return whether this configuration property is useful for headless
		 *         environments only. There will be no control rendered in the
		 *         preference page for Tasking
		 */
		boolean headlessOnly() default false;
	}

}
