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
package org.eclipse.tea.core.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.eclipse.tea.core.services.TaskingMenuDecoration;
import org.osgi.framework.Constants;

/**
 * Annotates a task chain for the TEA (Tasking Engine Advanced).
 * <p>
 * The order in which menus appear can be influenced to some degree by settings
 * the {@link Constants#SERVICE_RANKING} property on the TaskChain itself, as
 * this influences the order in which menu items are created (including their
 * sub-menu-path!).
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface TaskChainMenuEntry {

	/**
	 * @return The menu path that the annotated TaskChain should be made
	 *         available in under in the UI. Includes any sub-menu and the name
	 *         of the actual item itself. If not given, the description of the
	 *         task chain is used instead.
	 */
	String[] path() default {};

	/**
	 * TODO: support other URIs, not only relative paths
	 *
	 * @return the path to an icon to display. The path is relative to the
	 *         bundle declaring the annotated TaskChain.
	 */
	String icon() default "";

	/**
	 * @return whether the annotated task chain is for development purposes
	 *         only. development task chains are excluded from the UI menu by
	 *         default.
	 */
	boolean development() default false;

	/**
	 * @return an optional group ID that allows grouping together and ranking of
	 *         menu items in the final menu.
	 */
	String groupingId() default TaskingMenuDecoration.NO_GROUPING;
}
