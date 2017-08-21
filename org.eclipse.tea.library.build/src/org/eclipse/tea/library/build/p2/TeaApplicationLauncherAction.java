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
package org.eclipse.tea.library.build.p2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.equinox.internal.p2.publisher.eclipse.ExecutablesDescriptor;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.eclipse.ApplicationLauncherAction;

@SuppressWarnings("restriction")
public class TeaApplicationLauncherAction extends ApplicationLauncherAction {

	private final String id;
	private final Version version;
	private final String flavor;

	public TeaApplicationLauncherAction(String id, Version version, String flavor, String executableName, File location,
			String[] configSpecs) {
		super(id, version, flavor, executableName, location, configSpecs);
		this.id = id;
		this.version = version;
		this.flavor = flavor;
	}

	@Override
	protected Collection<IPublisherAction> createExecutablesActions(String[] configs) {
		Collection<IPublisherAction> actions = new ArrayList<>(configs.length);
		for (int i = 0; i < configs.length; i++) {
			ExecutablesDescriptor executables = computeExecutables(configs[i]);
			IPublisherAction action = new TeaEquinoxExecutableAction(executables, configs[i], id, version, flavor);
			actions.add(action);
		}
		return actions;
	}

}
