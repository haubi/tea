
package org.eclipse.tea.core.ui.live.internal;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.PostConstruct;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectToolItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.model.application.ui.menu.MPopupMenu;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.TaskingEngine;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.internal.TaskProgressExtendedTracker;
import org.eclipse.tea.core.internal.TimeHelper;
import org.eclipse.tea.core.internal.listeners.TaskingStatusTracker;
import org.eclipse.tea.core.ui.TaskingEngineJob;
import org.eclipse.tea.core.ui.config.TaskingEclipsePreferenceStore;
import org.eclipse.tea.core.ui.internal.listeners.EventBrokerBridge;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationNode;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationRootNode;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationTaskNode;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class TaskingLiveView implements Refreshable, EventHandler {

	private static final int MAX_ELEMENTS = 10;
	private static final String CONTEXT_MENU_ID = "TaskingLiveViewContextMenu";

	private TreeViewer tree;
	private final Deque<VisualizationRootNode> nodes = new ArrayDeque<>();

	@PostConstruct
	public void postConstruct(Composite parent, IEventBroker broker, MPart part, EMenuService menuService) {
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

		// menus and stuff is persistent, but we want dynamic.
		part.getMenus().clear();

		// tree context menu
		MPopupMenu ctxMenu = MMenuFactory.INSTANCE.createPopupMenu();
		ctxMenu.setElementId(CONTEXT_MENU_ID);

		MDirectMenuItem relaunch = MMenuFactory.INSTANCE.createDirectMenuItem();
		relaunch.setLabel("Re-Launch...");
		relaunch.setObject(new Object() {
			@CanExecute
			public boolean can() {
				IStructuredSelection sel = tree.getStructuredSelection();
				if (sel.isEmpty() || sel.size() > 1) {
					return false;
				}
				return sel.getFirstElement() instanceof VisualizationRootNode;
			}

			@Execute
			public void rerun() {
				TaskingEngine engine = TaskingEngine.withConfiguration(new TaskingEclipsePreferenceStore());
				Job j = new TaskingEngineJob(engine,
						((VisualizationRootNode) tree.getStructuredSelection().getFirstElement()).getChain());
				j.schedule();
			}
		});

		relaunch.setIconURI(getIconUri("skip.png"));
		ctxMenu.getChildren().add(relaunch);

		part.getMenus().add(ctxMenu);
		menuService.registerContextMenu(tree.getControl(), CONTEXT_MENU_ID);

		// initialize event handling
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

		VisualizationRootNode node = nodeFor(eventContext.get(TaskExecutionContext.class));

		if (!event.getTopic().equals(EventBrokerBridge.EVENT_CHAIN_BEGIN) && node == null) {
			return; // ignore events if attaching listener whilst something is
					// running already.
		}

		switch (event.getTopic()) {
		case EventBrokerBridge.EVENT_CHAIN_BEGIN:
			// need to smudge the real context as task contexts rely on it.
			if (node != null) {
				// retry ...
				node.prepareRetry();
			}
			eventContext.set(Refreshable.class, this);
			nodes.addFirst(ContextInjectionFactory.make(VisualizationRootNode.class, eventContext));

			while (nodes.size() > MAX_ELEMENTS) {
				nodes.removeLast();
			}
			break;
		case EventBrokerBridge.EVENT_CHAIN_FINISH:
			node.done();
			break;
		case EventBrokerBridge.EVENT_TASK_BEGIN:
			node.getNodeFor(eventContext.get(TaskingInjectionHelper.CTX_TASK))
					.begin(eventContext.get(TaskProgressExtendedTracker.class));
			break;
		case EventBrokerBridge.EVENT_TASK_FINISH:
			node.getNodeFor(eventContext.get(TaskingInjectionHelper.CTX_TASK)).done(
					eventContext.get(TaskProgressExtendedTracker.class), eventContext.get(TaskExecutionContext.class),
					eventContext.get(TaskingStatusTracker.class));
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