/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package org.eclipse.tea.core.ui.live.internal.model;

import org.eclipse.core.runtime.IStatus;

public class VisualizationStatusNode {

	private final IStatus status;

	public VisualizationStatusNode(IStatus status) {
		this.status = status;
	}

	public String getLabel() {
		return status.getMessage();
	}

	public int getServerity() {
		return status.getSeverity();
	}

}
