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
package org.eclipse.tea.library.build.model;

import org.osgi.framework.Version;

/**
 * Small DTO to hold information about a plugin
 */
public class PluginInfo {

	public String id;
	public String version;

	/** only used as cache to speed subsequent calls to {@link #getVersion()} */
	private Version ver;

	/**
	 * Returns the {@link Version} of the denoted plugin
	 */
	public Version getVersion() {
		/*
		 * don't synchronize - it's not worth it. if really two call it from two
		 * threads at the same time, then both parse it, and one wins. as the
		 * result is stable, this is only a very negligible hit.
		 */
		if (ver == null) {
			ver = Version.parseVersion(version);
		}

		return ver;
	}

	@Override
	public String toString() {
		return "PluginInfo (id=" + id + ", version=" + version + ")";
	}

}
