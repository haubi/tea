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
package org.eclipse.tea.library.build.lcdsl;

import org.eclipse.tea.library.build.chain.TeaBuildChain;
import org.eclipse.tea.library.build.chain.TeaUnhandledElement;
import org.eclipse.tea.library.build.services.TeaDependencyWireFactory;
import org.osgi.service.component.annotations.Component;

@Component
public class TeaLcDslElementWiring implements TeaDependencyWireFactory {

	@Override
	public void createWires(TeaBuildChain chain) {
		TeaLcDslElement pre = (TeaLcDslElement) chain.getElementFor(TeaLcDslElement.NAME_PRE);
		TeaLcDslElement post = (TeaLcDslElement) chain.getElementFor(TeaLcDslElement.NAME_POST);
		if (pre != null && post != null) {
			chain.getAllElements().forEach(e -> {
				if (e instanceof TeaUnhandledElement) {
					return;
				}

				// pre should be first, all others have a dependency
				if (e != pre) {
					e.addDependencyWire(pre.createWire());
				}

				// post has a dependency to all others.
				if (post != e) {
					post.addDependencyWire(e.createWire());
				}
			});

		}
	}

}
