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
		String message = status.getMessage();
		if (status.getException() != null) {
			return message + " (" + status.getException() + ")";
		}
		if (message == null || message.isEmpty()) {
			return status.toString(); // aehm. prevent empty string (e.g.
										// Status.CANCEL_STATUS).
		}
		return message;
	}

	public int getServerity() {
		return status.getSeverity();
	}

}
