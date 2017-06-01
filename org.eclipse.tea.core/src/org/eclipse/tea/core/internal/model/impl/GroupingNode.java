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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Internal helper that keeps track of the grouping within a single menu tier.
 * Grouping is represented as a tree of nodes which allows for automatic sorting
 * of groupings.
 */
class GroupingNode {

	/**
	 * Nodes that compete for a slot are sorted by their id lexically
	 * (attention: NOT by label(s) or anything else).
	 */
	private static final Comparator<GroupingNode> NODE_COMP = (a, b) -> {
		return a.groupingId.compareTo(b.groupingId);
	};

	private final String groupingId;

	final Set<GroupingNode> before = new TreeSet<>(NODE_COMP);
	final Set<GroupingNode> after = new TreeSet<>(NODE_COMP);

	public GroupingNode(String groupingId) {
		this.groupingId = groupingId;
	}

	/**
	 * @return a flattened and sorted list of all nodes that are related to this
	 *         node (directly and indirectly).
	 */
	public List<String> flatListSorted() {
		List<String> target = new ArrayList<>();
		flatListRecursive(target);
		return target;
	}

	private void flatListRecursive(List<String> target) {
		// all befores
		before.forEach(n -> n.flatListRecursive(target));

		// self
		target.add(groupingId);

		// all afters
		after.forEach(n -> n.flatListRecursive(target));
	}

}
