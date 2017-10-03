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
package org.eclipse.tea.library.build.chain.jdk;

import org.eclipse.tea.library.build.chain.TeaBuildChain;
import org.eclipse.tea.library.build.chain.TeaBuildElement;
import org.eclipse.tea.library.build.services.TeaDependencyWireFactory;
import org.osgi.service.component.annotations.Component;

@Component
public class TeaJdkLibWires implements TeaDependencyWireFactory {

	@Override
	public void createWires(TeaBuildChain chain) {
		chain.getAllElements().stream().filter(TeaJdkLibBuildElement.class::isInstance).map(TeaJdkLibBuildElement.class::cast).forEach(e -> {
			// the element for the project depends on the element which copies the library.
			TeaBuildElement prj = chain.getElementFor(e.getProject().getName());
			prj.addDependencyWire(e.createWire());
		});
	}

}
