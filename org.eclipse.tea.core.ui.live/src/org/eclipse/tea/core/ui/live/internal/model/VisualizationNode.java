package org.eclipse.tea.core.ui.live.internal.model;

import org.eclipse.core.runtime.IStatus;

public interface VisualizationNode {

	public String getName();

	public int getTotalProgress();

	public int getCurrentProgress();

	public IStatus getStatus();

	public long getDuration();

	public boolean isActive();

	public boolean isDone();

}
