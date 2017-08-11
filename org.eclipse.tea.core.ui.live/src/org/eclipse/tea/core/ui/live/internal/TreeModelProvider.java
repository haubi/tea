package org.eclipse.tea.core.ui.live.internal;

import java.util.Deque;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationRootNode;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationStatusNode;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationTaskNode;

public class TreeModelProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof Deque<?>) {
			return ((Deque<?>) inputElement).toArray();
		}
		return null;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof VisualizationRootNode) {
			return ((VisualizationRootNode) parentElement).getNodes().toArray();
		} else if (parentElement instanceof VisualizationTaskNode) {
			return ((VisualizationTaskNode) parentElement).getStatusNodes().toArray();
		} else if (parentElement instanceof VisualizationStatusNode) {
			return ((VisualizationStatusNode) parentElement).getChildren().toArray();
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof VisualizationRootNode || ((element instanceof VisualizationTaskNode
				&& !((VisualizationTaskNode) element).getStatusNodes().isEmpty())
				|| ((element instanceof VisualizationStatusNode)
						&& !((VisualizationStatusNode) element).getChildren().isEmpty()));
	}

}
