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
package org.eclipse.tea.library.build.chain.plugin;

import org.eclipse.tea.library.build.chain.TeaBuildChain;
import org.eclipse.tea.library.build.chain.TeaBuildElement;
import org.eclipse.tea.library.build.chain.TeaDependencyWire;
import org.eclipse.tea.library.build.model.PluginBuild;
import org.eclipse.tea.library.build.services.TeaDependencyWireFactory;
import org.osgi.service.component.annotations.Component;

/**
 * Creates {@link TeaDependencyWire}s based on plugin dependencies.
 */
@Component
public class TeaBuildPluginDependencyWireFactory implements TeaDependencyWireFactory {

	@Override
	public void createWires(TeaBuildChain chain) {
		for (TeaBuildElement element : chain.getAllElements()) {
			if (element instanceof TeaBuildPluginElement) {
				PluginBuild pb = ((TeaBuildPluginElement) element).getPlugin();

				for (String name : pb.getWorkspaceDependencies()) {
					TeaBuildElement target = chain.getElementFor(name);
					if (target != null) {
						// it is legal to only build certain elements without
						// dependencies.
						element.addDependencyWire(target.createWire());
					}
				}
			}
		}
	}

}
