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
package org.eclipse.tea.core;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * An {@link IStatus} which has an associated {@link IMarker}. Used to back-link
 * to the location which caused a certain sub-status.
 */
public class MarkerStatus extends Status {

	private IMarker marker;

	public MarkerStatus(int severity, String pluginId, String message, IMarker marker) {
		this(severity, pluginId, message, marker, null);
	}

	public MarkerStatus(int severity, String pluginId, String message, IMarker marker, Throwable t) {
		super(severity, pluginId, message, t);
		this.marker = marker;
	}

	/**
	 * @return the associated {@link IMarker} which can be used to track back to the
	 *         originating location of this {@link IStatus}.
	 */
	public IMarker getMarker() {
		return marker;
	}

}
