package org.eclipse.tea.core.internal;

import org.eclipse.tea.core.services.TaskProgressTracker;

public interface TaskProgressExtendedTracker extends TaskProgressTracker {

	public Object getTask();
	
	public void addListener(ProgressListener listener);
	
	public void removeListener(ProgressListener listener);
	
    public interface ProgressListener {
    	
    	public void progressChanged(int currentProgress);
    	
    }
			
}
