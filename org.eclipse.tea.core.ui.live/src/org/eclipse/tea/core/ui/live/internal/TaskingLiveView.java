
package org.eclipse.tea.core.ui.live.internal;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectToolItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.internal.TaskProgressExtendedTracker;
import org.eclipse.tea.core.internal.TimeHelper;
import org.eclipse.tea.core.internal.listeners.TaskingStatusTracker;
import org.eclipse.tea.core.ui.internal.listeners.EventBrokerBridge;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationNode;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationRootNode;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationTaskNode;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class TaskingLiveView implements Refreshable, EventHandler {

	private TreeViewer tree;
	private final List<VisualizationRootNode> nodes = new ArrayList<>();

	@PostConstruct
	public void postConstruct(Composite parent, IEventBroker broker, MPart part) {
		tree = new TreeViewer(parent);
		tree.setContentProvider(new TreeModelProvider());
		tree.getTree().setHeaderVisible(true);
		tree.setInput(nodes);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree.getControl());

		TreeViewerColumn name = new TreeViewerColumn(tree, SWT.LEFT);
		name.getColumn().setText("Task");
		name.getColumn().setWidth(350);
		name.setLabelProvider(new TreeLabelColumnProvider());

		TreeViewerColumn duration = new TreeViewerColumn(tree, SWT.LEFT);
		duration.getColumn().setText("Duration");
		duration.getColumn().setWidth(100);
		duration.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof VisualizationNode) {
					if (element instanceof VisualizationTaskNode && ((VisualizationTaskNode) element).isSkipped()) {
						return null;
					} else if (((VisualizationNode) element).isDone()) {
						return TimeHelper.formatDuration(((VisualizationNode) element).getDuration());
					}
				}
				return null;
			}
		});

		TreeViewerColumn progressText = new TreeViewerColumn(tree, SWT.RIGHT);
		progressText.getColumn().setText("Status");
		progressText.getColumn().setWidth(75);
		progressText.setLabelProvider(new TreeProgressRenderer());

		// add toolbar entries
		MDirectToolItem clear = MMenuFactory.INSTANCE.createDirectToolItem();
		clear.setLabel("Clear");
		clear.setObject(new Object() {
			@Execute
			public void clear() {
				nodes.clear();
				refresh();
			}
		});
		clear.setIconURI(getIconUri("clear.png"));

		part.setToolbar(MMenuFactory.INSTANCE.createToolBar());
		part.getToolbar().getChildren().add(clear);

		broker.subscribe(EventBrokerBridge.EVENT_TOPIC_BASE + "*", null, this, true);
	}

	private String getIconUri(String icon) {
		return "platform:/plugin/" + Activator.PLUGIN_ID + "/resources/" + icon;
	}

	private VisualizationRootNode nodeFor(TaskExecutionContext tec) {
		return nodes.stream().filter(o -> o.getTaskExecutionContext() == tec).findFirst().orElse(null);
	}

	@Override
	public void handleEvent(Event event) {
		Object data = event.getProperty(IEventBroker.DATA);
		if (!(data instanceof IEclipseContext)) {
			return;
		}
		IEclipseContext eventContext = (IEclipseContext) data;

		switch (event.getTopic()) {
		case EventBrokerBridge.EVENT_CHAIN_BEGIN:
			// need to smudge the real context as task contexts rely on it.
			eventContext.set(Refreshable.class, this);
			nodes.add(ContextInjectionFactory.make(VisualizationRootNode.class, eventContext));
			break;
		case EventBrokerBridge.EVENT_CHAIN_FINISH:
			nodeFor(eventContext.get(TaskExecutionContext.class)).done();
			break;
		case EventBrokerBridge.EVENT_TASK_BEGIN:
			nodeFor(eventContext.get(TaskExecutionContext.class))
					.getNodeFor(eventContext.get(TaskingInjectionHelper.CTX_TASK))
					.begin(eventContext.get(TaskProgressExtendedTracker.class));
			break;
		case EventBrokerBridge.EVENT_TASK_FINISH:
			nodeFor(eventContext.get(TaskExecutionContext.class))
					.getNodeFor(eventContext.get(TaskingInjectionHelper.CTX_TASK))
					.done(eventContext.get(TaskProgressExtendedTracker.class),
							eventContext.get(TaskExecutionContext.class), eventContext.get(TaskingStatusTracker.class));
			break;
		}

		refresh();
	}

	@Override
	public void refresh() {
		tree.getControl().getDisplay().asyncExec(() -> tree.refresh(true));
	}

	@Focus
	public void onFocus() {
		tree.getControl().setFocus();
	}

}