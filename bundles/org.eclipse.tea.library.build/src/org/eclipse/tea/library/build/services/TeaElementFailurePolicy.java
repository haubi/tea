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
package org.eclipse.tea.library.build.services;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.eclipse.tea.library.build.chain.TeaBuildChain;
import org.eclipse.tea.library.build.chain.TeaBuildElement;

/**
 * Defines how to deal with a build failure in {@link TeaBuildChain} for the
 * annotated {@link TeaBuildElement}.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface TeaElementFailurePolicy {

	public enum FailurePolicy {
		IGNORE, ABORT_IMMEDIATE, USE_THRESHOLD
	}

	public FailurePolicy value() default FailurePolicy.USE_THRESHOLD;

}
