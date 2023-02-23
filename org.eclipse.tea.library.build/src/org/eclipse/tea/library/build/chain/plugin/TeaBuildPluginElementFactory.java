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

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.library.build.chain.TeaBuildChain;
import org.eclipse.tea.library.build.chain.TeaBuildElement;
import org.eclipse.tea.library.build.model.PluginBuild;
import org.eclipse.tea.library.build.model.WorkspaceBuild;
import org.eclipse.tea.library.build.services.TeaBuildElementFactory;
import org.osgi.service.component.annotations.Component;

/**
 * Creates {@link TeaBuildPluginElement}s for all {@link IProject}s in the
 * workspace that are plugins.
 */
@Component
public class TeaBuildPluginElementFactory implements TeaBuildElementFactory {

	private WorkspaceBuild wb;

	@Execute
	public void init(WorkspaceBuild wb) {
		this.wb = wb;
	}

	@Override
	public Collection<TeaBuildElement> createElements(TeaBuildChain chain, IProject project) {
		PluginBuild plugin = wb.getSourcePlugin(project.getName());
		if (plugin != null) {
			return Collections.singleton(new TeaBuildPluginElement(plugin));
		}
		return null;
	}

}
