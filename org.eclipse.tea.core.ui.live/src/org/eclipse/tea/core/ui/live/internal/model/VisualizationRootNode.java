package org.eclipse.tea.core.ui.live.internal.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.internal.model.TaskingModel;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.ui.live.internal.Activator;

public class VisualizationRootNode implements VisualizationNode {

	private String name;
	private final List<VisualizationTaskNode> nodes = new ArrayList<>();
	private final TaskChain chain;
	private final long totalProgress;
	private boolean active = true;
	private TaskExecutionContext tec;

	@Inject
	public VisualizationRootNode(TaskExecutionContext tec, TaskChain chain,
			@Named(TaskingInjectionHelper.CTX_PREPARED_TASKS) List<?> tasks,
			@Named(TaskingInjectionHelper.CTX_TASK_CONTEXTS) Map<?, IEclipseContext> taskContexts) {
		this.name = TaskingModel.getTaskChainName(chain) + " [" + SimpleDateFormat.getTimeInstance().format(new Date())
				+ "]";

		this.chain = chain;
		this.tec = tec;

		for (Object t : tasks) {
			nodes.add(ContextInjectionFactory.make(VisualizationTaskNode.class, taskContexts.get(t)));
		}

		totalProgress = nodes.stream().collect(Collectors.summarizingInt(VisualizationNode::getTotalProgress)).getSum();
	}

	public List<? extends VisualizationNode> getNodes() {
		return nodes;
	}

	public TaskChain getChain() {
		return chain;
	}

	public TaskExecutionContext getTaskExecutionContext() {
		return tec;
	}

	public VisualizationTaskNode getNodeFor(Object task) {
		return nodes.stream().filter(o -> o.getTask() == task).findFirst().orElse(null);
	}

	public void done() {
		// skip not run tasks
		for (VisualizationTaskNode n : nodes) {
			if (!n.isDone()) {
				n.skip();
			}
		}

		active = false;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getTotalProgress() {
		return (int) totalProgress;
	}

	@Override
	public int getCurrentProgress() {
		if (!active) {
			return getTotalProgress();
		}

		int total = 0;
		for (VisualizationTaskNode node : nodes) {
			total += node.getCurrentProgress();
		}
		return total;
	}

	@Override
	public IStatus getStatus() {
		MultiStatus ms = new MultiStatus(Activator.PLUGIN_ID, 0, name, null);

		for (VisualizationTaskNode node : nodes) {
			ms.add(node.getStatus());
		}

		return ms;
	}

	@Override
	public long getDuration() {
		return nodes.stream().collect(Collectors.summarizingLong(VisualizationNode::getDuration)).getSum();
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public boolean isDone() {
		return !active;
	}

	public void prepareRetry() {
		tec = null; // prevent this node from being found
		active = false; // no matter if it completed.
		name = name + " (retrying)"; // mark for user.
	}

}
