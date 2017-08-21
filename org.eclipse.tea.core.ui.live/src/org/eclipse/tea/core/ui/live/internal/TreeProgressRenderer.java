/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package org.eclipse.tea.core.ui.live.internal;

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationNode;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationTaskNode;

public class TreeProgressRenderer extends OwnerDrawLabelProvider {

	private static final Image IMG_WAIT = Activator
			.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "resources/waiting.png").createImage();
	private final Color gradColorTop;
	private final Color gradColorBottom;
	private final Color textColor;

	public TreeProgressRenderer(Display display) {
		gradColorTop = display.getSystemColor(SWT.COLOR_DARK_BLUE);
		gradColorBottom = display.getSystemColor(SWT.COLOR_BLUE);
		textColor = display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
	}

	@Override
	protected void measure(Event event, Object element) {
	}

	@Override
	protected void paint(Event event, Object element) {
		if (element instanceof VisualizationNode) {
			drawVisNode(event, (VisualizationNode) element);
		}
	}

	private void drawVisNode(Event event, VisualizationNode node) {
		int percentage = (node.getCurrentProgress() * 100 / node.getTotalProgress());

		GC gc = event.gc;
		Color foreground = gc.getForeground();
		Color background = gc.getBackground();

		Rectangle bounds = ((TreeItem) event.item).getBounds(event.index);

		if (!node.isDone()) {
			if (node.getCurrentProgress() == 0 && !node.isActive()) {
				drawImage(event, bounds, IMG_WAIT);
			} else {
				drawProgress(event, percentage, gc, bounds, 100);
				drawPercentageText(event, percentage, gc, bounds);
			}
		} else if (!(node instanceof VisualizationTaskNode && ((VisualizationTaskNode) node).isSkipped())) {
			drawProgress(event, percentage, gc, bounds, 20);

			Image image = TreeLabelColumnProvider.getStatusImage(node.getStatus().getSeverity());
			drawImage(event, bounds, image);
		}

		gc.setForeground(background);
		gc.setBackground(foreground);
	}

	private void drawImage(Event event, Rectangle bounds, Image image) {
		Rectangle rect2 = image.getBounds();
		int xOff = Math.max(0, (bounds.width - rect2.width) / 2);
		int yOff = Math.max(0, (bounds.height - rect2.height) / 2);
		event.gc.drawImage(image, event.x + xOff, event.y + yOff);
	}

	private void drawProgress(Event event, int percentage, GC gc, Rectangle bounds, int alpha) {

		gc.setForeground(gradColorTop);
		gc.setBackground(gradColorBottom);

		int width = (bounds.width - 1) * percentage / 100;
		Rectangle boundingBox = new Rectangle(event.x, event.y, width - 1, event.height - 1);

		gc.setAlpha(alpha);

		gc.fillGradientRectangle(event.x, event.y, width, event.height, true);
		gc.drawRectangle(boundingBox);

		gc.setAlpha(255);
	}

	private void drawPercentageText(Event event, int percentage, GC gc, Rectangle bounds) {
		String text = percentage + "%";
		Point size = event.gc.textExtent(text);
		int offset = Math.max(0, (bounds.height - size.y) / 2);
		int center = Math.max(0, (bounds.width - size.x) / 2);

		gc.setForeground(textColor);
		gc.drawText(text, event.x + center, event.y + offset, true);
	}

}
