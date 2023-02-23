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
package org.eclipse.tea.library.build.p2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.equinox.internal.p2.publisher.eclipse.ExecutablesDescriptor;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.eclipse.ApplicationLauncherAction;

@SuppressWarnings("restriction")
public class TeaApplicationLauncherAction extends ApplicationLauncherAction {

	private final String id;
	private final Version version;
	private final String flavor;
	private final File platformLocation;
	private final File overrideLocation;

	public TeaApplicationLauncherAction(String id, Version version, String flavor, String executableName, File location,
			String[] configSpecs, File overrideLocation) {
		super(id, version, flavor, executableName, location, configSpecs);
		this.id = id;
		this.version = version;
		this.flavor = flavor;
		this.platformLocation = location;
		this.overrideLocation = overrideLocation;
	}

	@Override
	protected Collection<IPublisherAction> createExecutablesActions(String[] configs) {
		Collection<IPublisherAction> actions = new ArrayList<>(configs.length);
		for (int i = 0; i < configs.length; i++) {
			boolean hasOverridden = true;
			ExecutablesDescriptor executables = createExecutablesFromLocation(overrideLocation, configs[i]);
			if (executables == null) {
				hasOverridden = false;
				executables = createExecutablesFromLocation(new File(platformLocation, "bin"), configs[i]);

				if (executables == null) {
					// also no executables in the target, fall back to original
					// code...
					executables = computeExecutables(configs[i]);
				}
			}
			IPublisherAction action = new TeaEquinoxExecutableAction(executables, configs[i], id, version, flavor,
					!hasOverridden);
			actions.add(action);
		}
		return actions;
	}

	public static ExecutablesDescriptor createExecutablesFromLocation(File executablesFeatureLocation,
			String configSpec) {
		if (executablesFeatureLocation == null || !executablesFeatureLocation.exists()) {
			return null;
		}
		String[] config = AbstractPublisherAction.parseConfigSpec(configSpec);
		File result = new File(executablesFeatureLocation, config[0] + "/" + config[1] + "/" + config[2]);
		if (!result.exists() || !result.isDirectory()) {
			return null;
		}

		String launcherName = null;
		for (File file : result.listFiles()) {
			if (file.getName().toLowerCase().endsWith("c.exe") || file.getName().toLowerCase().endsWith("c")) {
				// ignore for compat. eclipsec.exe (older versions), myprodc.exe
				// (newer versions). also catch unix files which just end with c
				continue;
			}

			if (file.getName().toLowerCase().endsWith(".exe")) {
				launcherName = file.getName().substring(0, file.getName().length() - 4);
			} else {
				launcherName = file.getName();
			}

			break;
		}

		return new ExecutablesDescriptor(config[1], launcherName == null ? "launcher" : launcherName, result,
				new File[] { result });
	}

}
