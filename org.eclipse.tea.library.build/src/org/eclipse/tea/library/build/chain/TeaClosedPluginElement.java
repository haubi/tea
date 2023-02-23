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
package org.eclipse.tea.library.build.chain;

import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * Represents a {@link TeaBuildElement} for plugins in the target platform.
 */
public class TeaClosedPluginElement extends TeaBuildElement {

	private final String name;

	public TeaClosedPluginElement(IPluginModelBase model) {
		this.name = model.getPluginBase().getId();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isBuilder() {
		return false;
	}
}
