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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import org.eclipse.tea.core.services.TaskingLog;

/**
 * Annotates a task, hinting the framework to capture stdout and stderr output
 * and redirecting it to the {@link TaskingLog} used during execution.
 * <p>
 * Should be used with care as this redirects ALL stdout and stderr output in
 * the VM.
 */
@Documented
@Inherited
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TaskCaptureStdOutput {
	/**
	 * @return whether to capture stdout
	 */
	boolean out() default true;

	/**
	 * @return whether to capture stderr
	 */
	boolean err() default true;

	/**
	 * @return whether to redirect stderr to where stdout is redirected instead
	 *         of normal error redirection. If {@link #out()} is false, this
	 *         will simply redirect out to err, otherwise both are redirected to
	 *         the {@link TaskingLog#info()} stream.
	 */
	boolean errToOut() default false;
}