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
package org.eclipse.tea.core.ui.internal.context;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.internal.workbench.E4Workbench;
import org.osgi.service.component.annotations.Component;

/**
 * {@link ContextFunction} that looks up a task chain by the name stored in the
 * context.
 */
@SuppressWarnings("restriction")
@Component(service = IContextFunction.class, property = { "service.context.key=E4Context" })
public class E4WorkbenchContextFunction extends ContextFunction {

	public static final String E4_CONTEXT_ID = "E4Context";

	@Override
	public Object compute(IEclipseContext context, String contextKey) {
		return E4Workbench.getServiceContext();
	}

}
