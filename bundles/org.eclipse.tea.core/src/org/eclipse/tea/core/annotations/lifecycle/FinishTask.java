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
package org.eclipse.tea.core.annotations.lifecycle;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import org.eclipse.tea.core.TaskingInjectionHelper;

/**
 * Annotate method on lifecycle service to be called when task chain execution
 * finishes.
 * <p>
 * The actual task can be injected using the named
 * {@link TaskingInjectionHelper#CTX_TASK} argument. The {@link Object} injected
 * is the actual instance of the task.
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Qualifier
public @interface FinishTask {
}
