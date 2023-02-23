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
package org.eclipse.tea.library.build.services;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.eclipse.tea.library.build.chain.TeaBuildChain;
import org.eclipse.tea.library.build.chain.TeaBuildElement;

/**
 * Defines how to deal with a build failure in {@link TeaBuildChain} prior to
 * running the annotated {@link TeaBuildElement} (i.e. something /before/ this
 * element went wrong).
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface TeaElementVisitPolicy {

	public enum VisitPolicy {
		ABORT_IF_PREVIOUS_ERROR, USE_THRESHOLD
	}

	public VisitPolicy value() default VisitPolicy.USE_THRESHOLD;

}
