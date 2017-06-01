/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.core.internal.model.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tea.core.internal.model.iface.TaskingContainer;
import org.eclipse.tea.core.internal.model.iface.TaskingElement;
import org.eclipse.tea.core.internal.model.iface.TaskingItem;
import org.eclipse.tea.core.services.TaskingMenuDecoration;
import org.eclipse.tea.core.services.TaskingMenuDecoration.TaskingMenuGroupingId;
import org.eclipse.tea.core.services.TaskingMenuDecoration.TaskingMenuPathDecoration;
import org.osgi.framework.FrameworkUtil;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

public class TaskingContainerImpl implements TaskingContainer {

	private final String[] path;
	private Field decoration;

	private final Map<String, TaskingMenuGroupingId> definedGroupingIds = new TreeMap<>();

	private final Set<TaskingElement> children = new HashSet<>();

	/**
	 * Dummy to simplify code in this class (if there is no decoration
	 * available).
	 */
	@SuppressWarnings("unused")
	private final String dummy = null;

	public TaskingContainerImpl(String[] path) {
		this.path = path;
	}

	public void setDecoration(Field decoration) {
		this.decoration = decoration;
	}

	public void addGroupingId(Field groupingId) {
		TaskingMenuGroupingId annotation = groupingId.getAnnotation(TaskingMenuGroupingId.class);
		try {
			definedGroupingIds.put(groupingId.get(null).toString(), annotation);
		} catch (Exception e) {
			throw new IllegalStateException("Unexpected Error", e);
		}
	}

	@Override
	public String getIconBundle() {
		if (decoration == null) {
			return null;
		}

		return FrameworkUtil.getBundle(decoration.getDeclaringClass()).getSymbolicName();
	}

	@Override
	public String getIconPath() {
		if (decoration == null) {
			return null;
		}

		try {
			return decoration.get(null).toString();
		} catch (Exception e) {
			throw new IllegalStateException("Unexpected Error", e);
		}
	}

	@Override
	public String getLabel() {
		return path[path.length - 1];
	}

	@Override
	public Set<TaskingElement> getChildren() {
		if (children.isEmpty()) {
			return children;
		}

		// need to sort here, as there is no guaranteed order in which items and
		// groupingIds appear in the container. the sorting algorithm needs to
		// see all available groups in the container already...

		Map<String, GroupingNode> allNodes = new TreeMap<>();

		// request all groups to lazily build up the structural information
		// required. allNodes serves as a cache and synchronization for creation
		// and lookup (recursive algorithm).
		children.stream().map((c) -> c.getGroupingId()).forEach(id -> getGroupingNode(id, allNodes));

		// now that the tree is built. we can ask the NO_GROUPING node for a
		// full map of all before's and after's. EVERY grouping node is directly
		// or indirectly related to the NO_GROUPING node.
		GroupingNode rootNode = allNodes.get(TaskingMenuDecoration.NO_GROUPING);
		Assert.isNotNull(rootNode, "No root grouping node");
		List<String> sortedGroupingIds = rootNode.flatListSorted();

		Set<TaskingElement> sortedElements = new TreeSet<>(new Comparator<TaskingElement>() {
			@Override
			public int compare(TaskingElement a, TaskingElement b) {
				int groupingIndexA = sortedGroupingIds.indexOf(a.getGroupingId());
				int groupingIndexB = sortedGroupingIds.indexOf(b.getGroupingId());

				// group by groupingId
				int x = Integer.compare(groupingIndexA, groupingIndexB);
				if (x != 0) {
					return x;
				}

				// sort by label
				return a.getLabel().compareTo(b.getLabel());
			}
		});
		sortedElements.addAll(children);
		return sortedElements;
	}

	private GroupingNode getGroupingNode(String id, Map<String, GroupingNode> allNodes) {
		return allNodes.computeIfAbsent(id, (key) -> {
			GroupingNode node = new GroupingNode(key);

			// prevent endless recursion.
			if (key.equals(TaskingMenuDecoration.NO_GROUPING)) {
				return node;
			}

			// if the grouping is defined, use metadata, otherwise we assume it
			// is after the NO_GROUPING node.
			TaskingMenuGroupingId defined = definedGroupingIds.get(key);
			if (defined == null) {
				getGroupingNode(TaskingMenuDecoration.NO_GROUPING, allNodes).after.add(node);
			} else if (!Strings.isNullOrEmpty(defined.afterGroupingId())) {
				getGroupingNode(defined.afterGroupingId(), allNodes).after.add(node);
			} else {
				getGroupingNode(defined.beforeGroupingId(), allNodes).before.add(node);
			}

			return node;
		});
	}

	@Override
	public TaskingContainer getContainer(String name) {
		for (TaskingElement e : children) {
			if (e instanceof TaskingContainer && e.getLabel().equals(name)) {
				return (TaskingContainer) e;
			}
		}
		return null;
	}

	@Override
	public boolean isDevelopment() {
		boolean dev = true;
		for (TaskingElement e : children) {
			if (!e.isDevelopment()) {
				dev = false;
			}
		}
		return dev;
	}

	@Override
	public String getGroupingId() {
		if (decoration == null) {
			return TaskingMenuDecoration.NO_GROUPING;
		}

		// annotation must be present, otherwise decoration is null
		return decoration.getAnnotation(TaskingMenuPathDecoration.class).groupingId();
	}

	@Override
	public TaskingItem getItem(String id) {
		List<TaskingItem> matches = new ArrayList<>();
		for (TaskingElement ele : children) {
			if (ele instanceof TaskingItem && ((TaskingItem) ele).matchesId(id)) {
				matches.add((TaskingItem) ele);
			}

			if (ele instanceof TaskingContainer) {
				TaskingItem result = ((TaskingContainer) ele).getItem(id);
				if (result != null) {
					matches.add(result);
				}
			}
		}

		switch (matches.size()) {
		case 0:
			return null;
		case 1:
			return matches.get(0);
		default:
			throw new IllegalStateException("Ambiguous TaskChain ID: " + id + ". Matches " + Joiner.on(", ")
					.join(matches.stream().map(c -> c.getChain().getClass().getName()).collect(Collectors.toList())));
		}
	}

	TaskingContainerImpl createGroup(String[] path, int lvl) {
		// can be the case if no path is specified in the annotation that
		// requests a group. in this case, this is the root group.
		if (path.length <= lvl) {
			return this;
		}

		String toLookup = path[lvl];
		TaskingContainerImpl group = (TaskingContainerImpl) getContainer(toLookup);
		if (group == null) {
			String[] relPath = new String[lvl + 1];
			System.arraycopy(path, 0, relPath, 0, lvl + 1);
			group = new TaskingContainerImpl(relPath);
			addChild(group);
		}

		if (path.length - 1 == lvl) {
			return group;
		} else {
			return group.createGroup(path, lvl + 1);
		}
	}

	void addChild(TaskingElement e) {
		children.add(e);
	}

	void debugString(StringBuilder target, String indent) {
		target.append(indent).append("Group '").append(getLabel()).append("'\n");
		indent = indent + "  ";
		for (TaskingElement element : children) {
			if (element instanceof TaskingContainerImpl) {
				((TaskingContainerImpl) element).debugString(target, indent);
			} else {
				target.append(indent).append("Item '").append(element).append("'\n");
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		debugString(builder, "");
		return builder.toString();
	}

}
