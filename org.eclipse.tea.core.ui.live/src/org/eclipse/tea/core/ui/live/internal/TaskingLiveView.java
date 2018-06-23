
package org.eclipse.tea.core.ui.live.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
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
import org.eclipse.tea.core.ui.live.internal.model.VisualizationStatusNode;
import org.eclipse.tea.core.ui.live.internal.model.VisualizationTaskNode;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.ResourceUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.google.common.base.Joiner;

public class TaskingLiveView implements Refreshable, EventHandler {

	private static final int MAX_ELEMENTS = 10;
	private static final String CONTEXT_MENU_ID = "TaskingLiveViewContextMenu";

	private TreeViewer tree;
	private final Deque<VisualizationRootNode> nodes = new ArrayDeque<>();
	private Clipboard clipboard;

	@PostConstruct
	public void postConstruct(Composite parent, IEventBroker broker, MPart part, EMenuService menuService) {
		tree = new TreeViewer(parent);
		tree.setContentProvider(new TreeModelProvider());
		tree.getTree().setHeaderVisible(true);
		tree.setInput(nodes);
		ColumnViewerToolTipSupport.enableFor(tree);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree.getControl());

		TreeViewerColumn name = new TreeViewerColumn(tree, SWT.LEFT);
		name.getColumn().setText("Task");
		name.getColumn().setWidth(300);
		name.setLabelProvider(new TreeLabelColumnProvider());

		TreeViewerColumn duration = new TreeViewerColumn(tree, SWT.LEFT);
		duration.getColumn().setText("Duration");
		duration.getColumn().setWidth(80);
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
		progressText.setLabelProvider(new TreeProgressRenderer(tree.getControl().getDisplay()));

		clipboard = new Clipboard(tree.getControl().getDisplay());

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
		clear.setIconURI(getIconUri("removea_exc.png"));

		part.setToolbar(MMenuFactory.INSTANCE.createToolBar());
		part.getToolbar().getChildren().add(clear);

		// on double click, check whether we can jump somewhere
		tree.addDoubleClickListener(this::doubleClick);

		// also on double click, check whether an item can and should be expanded...
		tree.addDoubleClickListener(event -> {
			ISelection selection = event.getSelection();
			if (selection instanceof ITreeSelection) {
				ITreeSelection ss = (ITreeSelection) selection;
				if (ss.size() == 1) {
					Object obj = ss.getFirstElement();
					if (tree.isExpandable(obj)) {
						tree.setExpandedState(obj, !tree.getExpandedState(obj));
					}
				}
			}
		});

		// menus and stuff is persistent, but we want dynamic.
		part.getMenus().clear();

		// tree context menu
		MPopupMenu ctxMenu = MMenuFactory.INSTANCE.createPopupMenu();
		ctxMenu.setElementId(CONTEXT_MENU_ID);

		MDirectMenuItem relaunch = MMenuFactory.INSTANCE.createDirectMenuItem();
		relaunch.setLabel("Re-Launch...");
		relaunch.setIconURI(getIconUri("skip.png"));
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

		MDirectMenuItem remove = MMenuFactory.INSTANCE.createDirectMenuItem();
		remove.setLabel("Remove selected");
		remove.setIconURI(getIconUri("clear.png"));
		remove.setObject(new Object() {
			@CanExecute
			public boolean can() {
				IStructuredSelection sel = tree.getStructuredSelection();
				if (sel.isEmpty()) {
					return false;
				}
				return sel.getFirstElement() instanceof VisualizationRootNode;
			}

			@Execute
			public void remove() {
				for (Object root : tree.getStructuredSelection().toList()) {
					nodes.remove(root);
				}
				refresh();
			}
		});

		MDirectMenuItem copy = MMenuFactory.INSTANCE.createDirectMenuItem();
		copy.setLabel("Copy");
		copy.setIconURI(getIconUri("copy_edit_co.png"));
		copy.setObject(new Object() {
			@Execute
			public void copy() {
				List<String> labels = new ArrayList<>();

				// copy labels to clipboard
				for (Object element : tree.getStructuredSelection().toList()) {
					if (element instanceof VisualizationNode) {
						labels.add(((VisualizationNode) element).getName());
					} else if (element instanceof VisualizationStatusNode) {
						labels.add(((VisualizationStatusNode) element).getLabel());
					}
				}

				clipboard.setContents(new Object[] { Joiner.on('\n').join(labels) },
						new Transfer[] { TextTransfer.getInstance() });
			}
		});

		ctxMenu.getChildren().add(relaunch);
		ctxMenu.getChildren().add(remove);
		ctxMenu.getChildren().add(copy);

		part.getMenus().add(ctxMenu);
		menuService.registerContextMenu(tree.getControl(), CONTEXT_MENU_ID);

		// initialize event handling
		broker.subscribe(EventBrokerBridge.EVENT_TOPIC_BASE + "*", null, this, true);
	}

	private void doubleClick(DoubleClickEvent event) {
		IStructuredSelection sel = (IStructuredSelection) event.getSelection();
		if (sel.isEmpty() || sel.size() > 1) {
			return; // only handle single item double click.
		}

		Object clicked = sel.getFirstElement();
		if (clicked instanceof VisualizationStatusNode) {
			VisualizationStatusNode sn = (VisualizationStatusNode) clicked;
			if (sn.getMarker() != null) {
				openMarkerInEditor(sn.getMarker());
			}
		}
	}

	/**
	 * Open the supplied marker in an editor in page borrowed mostly from
	 * org.eclipse.ui.internal.views.markers.ExtendedMarkersView.
	 */
	public static void openMarkerInEditor(IMarker marker) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		IEditorPart editor = page.getActiveEditor();
		if (editor != null) {
			IEditorInput input = editor.getEditorInput();
			IFile file = ResourceUtil.getFile(input);
			if (file != null) {
				if (marker.getResource().equals(file) && OpenStrategy.activateOnOpen()) {
					page.activate(editor);
				}
			}
		}

		if (marker != null && marker.getResource() instanceof IFile) {
			try {
				IDE.openEditor(page, marker, OpenStrategy.activateOnOpen());
			} catch (PartInitException e) {
				Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Cannot open Editor for marker", e));
			}
		}
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

		switch (event.getTopic()) {
		case EventBrokerBridge.EVENT_CHAIN_BEGIN:
		case EventBrokerBridge.EVENT_CHAIN_FINISH:
			tree.getControl().getDisplay().asyncExec(() -> {
				tree.collapseAll();
				tree.setExpandedElements(nodes.stream().filter(e -> e.isActive()).toArray());
			});
		}
	}

	@Override
	public void refresh() {
		tree.getControl().getDisplay().asyncExec(() -> tree.refresh(true));
	}

	@Focus
	public void onFocus() {
		tree.getControl().setFocus();
	}

	@PreDestroy
	public void dispose() {
		clipboard.dispose();
	}

}