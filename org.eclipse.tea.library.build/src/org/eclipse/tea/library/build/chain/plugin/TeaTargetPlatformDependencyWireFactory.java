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
package org.eclipse.tea.library.build.chain.plugin;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.tea.library.build.chain.TeaBuildChain;
import org.eclipse.tea.library.build.chain.TeaBuildElement;
import org.eclipse.tea.library.build.chain.TeaDependencyWire;
import org.eclipse.tea.library.build.chain.TeaUnhandledElement;
import org.eclipse.tea.library.build.services.TeaDependencyWireFactory;
import org.osgi.service.component.annotations.Component;

/**
 * Creates {@link TeaDependencyWire}s based on plugin dependencies.
 */
@Component
public class TeaTargetPlatformDependencyWireFactory implements TeaDependencyWireFactory {

	@Override
	public void createWires(TeaBuildChain chain) {
		for (IPluginModelBase model : PluginRegistry.getActiveModels(false)) {
			String pluginName = model.getPluginBase().getId();
			BundleDescription bundleDescription = model.getBundleDescription();
			if (bundleDescription == null) {
				throw new IllegalStateException("Manifest not in OSGi form: " + pluginName + "/META-INF/MANIFEST.MF");
			}
			BundleSpecification[] requiredBundles = bundleDescription.getRequiredBundles();
			TeaBuildElement plugin = chain.getElementFor(pluginName);
			if (plugin == null) {
				// System.out.println("plugin not found:" + pluginName);
			} else {
				if (!(plugin instanceof TeaUnhandledElement)) {
					for (BundleSpecification requiredBundleSpec : requiredBundles) {
						String requiredBundleName = requiredBundleSpec.getName();
						TeaBuildElement requiredPlugin = chain.getElementFor(requiredBundleName);
						if (requiredPlugin != null && requiredPlugin != plugin
								&& !(requiredPlugin instanceof TeaUnhandledElement)) {
							plugin.addDependencyWire(requiredPlugin.createWire());
						}
					}
				}
			}
		}
	}

}
