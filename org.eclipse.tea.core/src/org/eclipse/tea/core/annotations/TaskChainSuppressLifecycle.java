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

import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskingLifeCycleListener;

/**
 * Annotates a task chain for the TEA (Tasking Engine Advanced).
 * <p>
 * This annotation has the effect that no {@link TaskingLifeCycleListener} will
 * be called whenever the annotated {@link TaskChain} (or any of it's tasks) is
 * executed.
 * <p>
 * The purpose of this annotation lies in wrapping and nesting
 * {@link TaskChain}s around third party technologies. In this case it can
 * happen that there is a task chain that holds a single proxy task that
 * executes the actual {@link TaskChain} (which will/should have the full
 * lifecycle).
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface TaskChainSuppressLifecycle {

	boolean value() default true;

}
