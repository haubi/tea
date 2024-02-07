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

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.library.build.chain.TeaBuildChain;

/**
 * Factory responsible for creating dependency wires between projects that allow
 * a proper calculation of build order.
 * <p>
 * A factory may have a method annotated with an {@link Execute} annotation
 * which will be executed prior to actual element creation. This allows
 * injection and access to all available TEA constructs.
 */
public interface TeaDependencyWireFactory {

	public void createWires(TeaBuildChain chain);

}
