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
package org.eclipse.tea.library.build.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator implements BundleActivator {

	public static final String PLUGIN_ID = "org.eclipse.tea.library.build";
	private static BundleContext ctx;

	@Override
	public void start(BundleContext context) throws Exception {
		ctx = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		ctx = null;
	}

	public static BundleContext getContext() {
		return ctx;
	}

	public static void log(int severity, String message, Throwable exception) {
		Status status = new Status(severity, PLUGIN_ID, message, exception);
		StatusManager.getManager().handle(status, StatusManager.LOG);

		if (severity == IStatus.ERROR) {
			StatusManager.getManager().handle(status, StatusManager.SHOW);
		}
	}

	/** Returns the service described by the given name */
	public static Object getService(String name) {
		ServiceReference<?> reference = ctx.getServiceReference(name);
		if (reference == null) {
			return null;
		}
		Object result = ctx.getService(reference);
		ctx.ungetService(reference);
		return result;
	}

}
