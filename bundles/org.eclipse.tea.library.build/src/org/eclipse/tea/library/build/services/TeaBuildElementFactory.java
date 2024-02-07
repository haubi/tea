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

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.library.build.chain.TeaBuildChain;
import org.eclipse.tea.library.build.chain.TeaBuildElement;

/**
 * Responsible for creating according build elements for projects to be built
 * <p>
 * A factory may have a method annotated with an {@link Execute} annotation
 * which will be executed prior to actual element creation. This allows
 * injection and access to all available TEA constructs.
 */
public interface TeaBuildElementFactory {

	/**
	 * Creates an arbitrary amount of build elements for a given project.
	 * <p>
	 * Usually, a no or a single {@link TeaBuildElement} result from a single
	 * {@link IProject}. There are use cases though where for instance one
	 * element before and one element after all other elements for an
	 * {@link IProject} are required.
	 */
	public Collection<TeaBuildElement> createElements(TeaBuildChain chain, IProject prj);

}
