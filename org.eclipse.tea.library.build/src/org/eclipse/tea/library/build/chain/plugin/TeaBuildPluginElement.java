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
package org.eclipse.tea.library.build.chain.plugin;

import org.eclipse.tea.library.build.chain.TeaBuildElement;
import org.eclipse.tea.library.build.chain.TeaBuildProjectElement;
import org.eclipse.tea.library.build.chain.TeaDependencyWire;
import org.eclipse.tea.library.build.model.PluginBuild;

/**
 * Represents a {@link TeaBuildElement} for plugins.
 */
public class TeaBuildPluginElement extends TeaBuildProjectElement {

	/** The state of a build element. */
	private enum State {
		NEW, OK, ERROR
	};

	/** state of this build element */
	private State state = State.NEW;

	/** the underlying plugin to be handled */
	private final PluginBuild plugin;

	public TeaBuildPluginElement(PluginBuild pb) {
		super(pb.getData().getProject());
		this.plugin = pb;
	}

	/**
	 * @return the underlying {@link PluginBuild} handled by this
	 *         {@link TeaBuildPluginElement}
	 */
	public PluginBuild getPlugin() {
		return plugin;
	}

	/**
	 * @return determines whether all direct and indirect dependencies of this
	 *         {@link TeaBuildPluginElement} have been compiled successfully.
	 */
	public boolean isAllDependenciesBuilt() {
		// If i have been built, all my dependencies must have been.
		if (state == State.OK) {
			return true;
		}

		for (TeaDependencyWire wire : getDependencyWires()) {
			TeaBuildElement e = wire.getTarget();
			if (e instanceof TeaBuildPluginElement) {
				if (((TeaBuildPluginElement) e).state != State.OK) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Marks this {@link TeaBuildPluginElement} as successfully built.
	 */
	public void done() {
		state = State.OK;
	}

	/**
	 * Marks this {@link TeaBuildPluginElement} as having error(s).
	 */
	public void error() {
		state = State.ERROR;
	}

	@Override
	public String getName() {
		return plugin.getPluginName();
	}

}
