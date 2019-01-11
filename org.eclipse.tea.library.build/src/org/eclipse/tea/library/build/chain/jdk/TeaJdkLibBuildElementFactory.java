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

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.tea.library.build.chain.TeaBuildChain;
import org.eclipse.tea.library.build.chain.TeaBuildElement;
import org.eclipse.tea.library.build.model.ParameterValue;
import org.eclipse.tea.library.build.model.PluginData;
import org.eclipse.tea.library.build.services.TeaBuildElementFactory;
import org.osgi.service.component.annotations.Component;

@Component
public class TeaJdkLibBuildElementFactory implements TeaBuildElementFactory {

	@Override
	public Collection<TeaBuildElement> createElements(TeaBuildChain chain, IProject prj) {
		PluginData data = new PluginData(prj);
		ParameterValue pv = data.getManifestHeader("Build-JdkLib");
		if (pv != null) {
			String libName = pv.getValue();
			String path = pv.getStringParameter("path");

			return Collections.singletonList(new TeaJdkLibBuildElement(prj, libName, path));
		}
		return null;
	}

}
