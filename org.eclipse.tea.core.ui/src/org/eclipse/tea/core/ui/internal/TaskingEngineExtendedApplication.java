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
package org.eclipse.tea.core.ui.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import org.eclipse.tea.core.internal.TaskingConfigurationStore;
import org.eclipse.tea.core.internal.TaskingEngineApplication;
import org.eclipse.tea.core.internal.config.PropertyConfigurationStore;
import org.eclipse.tea.core.ui.config.TaskingEclipsePreferenceStore;

public class TaskingEngineExtendedApplication extends TaskingEngineApplication {

	@Override
	protected TaskingConfigurationStore getPreferenceStore(File propFile) throws IOException {
		return new PropertyConfigurationStore(propFile) {
			TaskingEclipsePreferenceStore eStore = new TaskingEclipsePreferenceStore();

			@Override
			public boolean loadValue(Field property, Object target) throws IllegalAccessException {
				if (!super.loadValue(property, target)) {
					return eStore.loadValue(property, target);
				}
				return true;
			}

			@Override
			public String toString() {
				return "Compound preference store (properties & preferences)";
			}
		};
	}

}
