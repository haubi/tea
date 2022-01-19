/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package org.eclipse.tea.core.ui.live.internal.model;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.tea.core.MarkerStatus;

public class VisualizationStatusNode {

	private final IStatus status;
	private final List<VisualizationStatusNode> children = new ArrayList<>();

	public VisualizationStatusNode(IStatus status) {
		this.status = status;

		if (status.isMultiStatus()) {
			for (IStatus s : status.getChildren()) {
				if (s.getSeverity() > IStatus.OK) {
					children.add(new VisualizationStatusNode(s));
				}
			}

			children.sort((a, b) -> {
				int x = b.getServerity() - a.getServerity();

				if (x != 0) {
					return x;
				}

				return a.getLabel().compareTo(b.getLabel());
			});
		}
	}

	public String getLabel() {
		String message = status.getMessage();
		if (status.getException() != null) {
			Throwable unwrapped = status.getException();
			// A RuntimException is typically wrapped in two
			// InjectionException. unwrap them:
			while (unwrapped instanceof InjectionException && unwrapped.getCause() != null) {
				unwrapped = unwrapped.getCause();
			}
			if (message != null
					&& message.startsWith(org.eclipse.tea.core.TaskExecutionContext.FATAL_FAILURE_WHILE_EXECUTING)) {
				return "" + unwrapped;
			}
			return message + " (" + unwrapped + ")";
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

	public List<VisualizationStatusNode> getChildren() {
		return children;
	}

	public IMarker getMarker() {
		if (status instanceof MarkerStatus) {
			return ((MarkerStatus) status).getMarker();
		}
		return null;
	}

}
