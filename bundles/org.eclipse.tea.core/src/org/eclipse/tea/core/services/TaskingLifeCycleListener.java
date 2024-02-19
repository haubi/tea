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
package org.eclipse.tea.core.services;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Service interface for parties interested in the life cycle of the TEA
 * controller.
 */
public interface TaskingLifeCycleListener {

	/**
	 * Annotates a lifecycle listener to influence the order in which they are
	 * notified.
	 */
	@Documented
	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface TaskingLifeCyclePriority {
		/**
		 * @return the priority of the annotated
		 *         {@link TaskingLifeCycleListener}. Higher priority listeners
		 *         are called before lower priority ones. 10 is the default
		 *         value if none is specified.
		 */
		int value() default 10;
	}

}
