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
package org.eclipse.tea.library.build.chain;

import org.eclipse.core.resources.IProject;
import org.eclipse.tea.library.build.services.TeaBuildElementFactory;

/**
 * Represents a named element for which no handler could have been found.
 * <p>
 * The primary use is to create a {@link TeaUnhandledElement} for each
 * {@link IProject} in the workspace where no {@link TeaBuildElementFactory}
 * produced a valid {@link TeaBuildElement}. This can be the case if a certain
 * {@link IProject} type is not supported by any {@link TeaBuildElementFactory}.
 */
public class TeaUnhandledElement extends TeaBuildElement {

	private final String name;

	public TeaUnhandledElement(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isBuilder() {
		return false;
	}
}
