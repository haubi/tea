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
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.jar.ZipExecFactory;
import org.eclipse.tea.library.build.services.TeaBuildVersionService;
import org.osgi.service.component.annotations.Component;

/**
 * Assures that there is exactly one {@link JarManager} per
 * {@link TaskExecutionContext}.
 */
@Component(service = IContextFunction.class, property = {
		"service.context.key=org.eclipse.tea.library.build.jar.JarManager" })
public class JarManagerContextFunction extends ContextFunction {

	@Override
	public Object compute(IEclipseContext context, String contextKey) {
		TaskingLog log = context.get(TaskingLog.class);
		ZipExecFactory zip = ContextInjectionFactory.make(ZipExecFactory.class, context);

		JarManager jm = new JarManager(log, zip, context.get(TeaBuildVersionService.class));
		TaskingInjectionHelper.findExecutionContext(context).set(JarManager.class, jm);

		return jm;
	}

}
