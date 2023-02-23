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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tea.core.MarkerStatus;
import org.eclipse.tea.library.build.internal.Activator;

/**
 * Base class for all {@link TeaBuildElement}s that directly correlate to a
 * single {@link IProject}.
 */
public abstract class TeaBuildProjectElement extends TeaBuildElement {

	private final IProject project;

	/**
	 * @param project
	 *            the {@link IProject} to associate with this
	 *            {@link TeaBuildElement}.
	 */
	public TeaBuildProjectElement(IProject project) {
		this.project = project;
	}

	/**
	 * @return the associated {@link IProject}.
	 */
	public IProject getProject() {
		return project;
	}

	/**
	 * @return a {@link List} of all {@link IMarker}s of the associated
	 *         {@link IProject} of type {@link IMarker#PROBLEM} converted to
	 *         {@link IStatus} for easier integration into TEA.
	 */
	public List<IStatus> getStatus() {
		List<IStatus> markers = new ArrayList<>();
		if (project != null) {
			try {
				for (IMarker marker : project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)) {
					markers.add(new MarkerStatus(mapSeverity(marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR)),
							Activator.PLUGIN_ID,
							marker.getAttribute(IMarker.LOCATION,
									marker.getResource().getProjectRelativePath().toOSString()) + ":"
									+ marker.getAttribute(IMarker.LINE_NUMBER, 0) + ": "
									+ marker.getAttribute(IMarker.MESSAGE, ""), marker));
				}
			} catch (CoreException e) {
				markers.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						"cannot retrieve marker for " + project.getName()));
			}
		}
		return markers;
	}

	/**
	 * Converts {@link IMarker} severity levels to {@link IStatus} severity
	 * levels.
	 */
	private static int mapSeverity(int severity) {
		switch (severity) {
		case IMarker.SEVERITY_ERROR:
			return IStatus.ERROR;
		case IMarker.SEVERITY_WARNING:
			return IStatus.WARNING;
		case IMarker.SEVERITY_INFO:
			return IStatus.INFO;
		}
		return IMarker.SEVERITY_ERROR;
	}

}
