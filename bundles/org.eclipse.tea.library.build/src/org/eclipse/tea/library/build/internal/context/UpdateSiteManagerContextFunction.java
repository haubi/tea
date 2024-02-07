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
package org.eclipse.tea.library.build.internal.context;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.library.build.config.BuildDirectories;
import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.p2.UpdateSiteManager;
import org.osgi.service.component.annotations.Component;

/**
 * Assures that there is exactly one {@link UpdateSiteManager} per
 * {@link TaskExecutionContext}.
 */
@Component(service = IContextFunction.class, property = {
		"service.context.key=org.eclipse.tea.library.build.p2.UpdateSiteManager" })
public class UpdateSiteManagerContextFunction extends ContextFunction {

	@Override
	public Object compute(IEclipseContext context, String contextKey) {
		JarManager jm = context.get(JarManager.class);
		if (jm == null) {
			return IInjector.NOT_A_VALUE;
		}

		BuildDirectories dirs = context.get(BuildDirectories.class);

		UpdateSiteManager um = new UpdateSiteManager(dirs.getSiteDirectory(), jm);
		TaskingInjectionHelper.findExecutionContext(context).set(UpdateSiteManager.class, um);

		return um;
	}

}
