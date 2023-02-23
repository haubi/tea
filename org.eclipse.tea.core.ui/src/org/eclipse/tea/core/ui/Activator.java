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
package org.eclipse.tea.core.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.eclipse.tea.core.ui";

	private static Activator instance;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Activator.instance = this;
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.instance = null;
	}

	public static Activator getInstance() {
		return instance;
	}

}
