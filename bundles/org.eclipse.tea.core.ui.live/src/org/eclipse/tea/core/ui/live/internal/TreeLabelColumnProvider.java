package org.eclipse.tea.core.ui.live.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationNode;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationStatusNode;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationTaskNode;

public class TreeLabelColumnProvider extends ColumnLabelProvider {

	static final Image IMG_ACTIVE = Activator
			.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "resources/state_active.png").createImage();

	static final Image IMG_SKIP = Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "resources/skip.png")
			.createImage();

	private static final Image IMG_OK = Activator
			.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "resources/ok_st_obj.png").createImage();

	private static final Image IMG_ERROR = Activator
			.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "resources/error_obj.png").createImage();

	private static final Image IMG_WARN = Activator
			.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "resources/warning_obj.png").createImage();

	private static final Image IMG_INFO = Activator
			.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "resources/info_obj.png").createImage();

	public static Image getStatusImage(int severity) {
		switch (severity) {
		case IStatus.CANCEL:
		case IStatus.ERROR:
			return IMG_ERROR;
		case IStatus.WARNING:
			return IMG_WARN;
		case IStatus.INFO:
			return IMG_INFO;
		case IStatus.OK:
			return IMG_OK;
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof VisualizationNode) {
			return ((VisualizationNode) element).getName();
		} else if (element instanceof VisualizationStatusNode) {
			return ((VisualizationStatusNode) element).getLabel();
		}
		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof VisualizationNode) {
			if (((VisualizationNode) element).isActive()) {
				return IMG_ACTIVE;
			} else if (((VisualizationNode) element).isDone()) {
				if (element instanceof VisualizationTaskNode && ((VisualizationTaskNode) element).isSkipped()) {
					return IMG_SKIP;
				} else {
					return getStatusImage(((VisualizationNode) element).getStatus().getSeverity());
				}
			}
		} else if (element instanceof VisualizationStatusNode) {
			return getStatusImage(((VisualizationStatusNode) element).getServerity());
		}
		return null;
	}

	@Override
	public String getToolTipText(Object element) {
		return getText(element);
	}
}