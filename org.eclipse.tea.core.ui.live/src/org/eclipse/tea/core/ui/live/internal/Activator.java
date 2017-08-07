package org.eclipse.tea.core.ui.live.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

	private static BundleContext context;
	public static final String PLUGIN_ID = "org.eclipse.tea.core.ui.live";

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		Activator.context = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);

		Activator.context = null;
	}

	public static BundleContext getBundleContext() {
		return context;
	}

}
