/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.core.services;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tea.core.internal.model.TaskingModel;

import com.google.common.base.Joiner;

/**
 * Allows to contribute to the TEA menu by providing additional entries.
 * <p>
 * Menu paths that do not exist will be created on the fly.
 */
public interface TaskingAdditionalMenuEntryProvider {

	/**
	 * @return all additional entries to put into the TEA Menu
	 */
	public List<TaskingAdditionalMenuEntry> getAdditionalEntries();

	/**
	 * Represents an additional entry in the menu.
	 */
	public static class TaskingAdditionalMenuEntry implements Comparable<TaskingAdditionalMenuEntry> {

		private final TaskChain chain;
		private final String[] path;
		private final String label;
		private final String iconBundle;
		private final String iconPath;
		private final String groupingId;
		private final boolean developerOnly;

		public TaskingAdditionalMenuEntry(TaskChain chain, String[] path, String iconUri, String groupingId,
				boolean dev) {
			this(chain, TaskingModel.getTaskChainName(chain), path, getIconBundle(iconUri), getIconPath(iconUri),
					groupingId, dev);
		}

		protected static String getIconBundle(String iconUri) {
			if (iconUri == null) {
				return null;
			}
			Assert.isLegal(iconUri.startsWith("platform:/plugin"), "URI must be of platform:/plugin scheme");
			String sub = iconUri.substring("platform:/plugin/".length());
			return sub.substring(0, sub.indexOf('/'));
		}

		protected static String getIconPath(String iconUri) {
			if (iconUri == null) {
				return null;
			}
			Assert.isLegal(iconUri.startsWith("platform:/plugin"), "URI must be of platform:/plugin scheme");
			String sub = iconUri.substring("platform:/plugin/".length());
			return sub.substring(sub.indexOf('/') + 1);
		}

		public TaskingAdditionalMenuEntry(TaskChain chain, String label, String[] path, String bundle, String icon,
				String groupingId, boolean developerOnly) {
			this.chain = chain;
			this.label = label;
			this.path = path;
			this.iconBundle = bundle;
			this.iconPath = icon;
			this.groupingId = groupingId != null ? groupingId : TaskingMenuDecoration.NO_GROUPING;
			this.developerOnly = developerOnly;
		}

		public TaskChain getTaskChain() {
			return chain;
		}

		public String[] getMenuPath() {
			return path;
		}

		public String getLabel() {
			return label;
		}

		public String getIconBundleName() {
			return iconBundle;
		}

		public String getIconPath() {
			return iconPath;
		}

		public String getGroupingId() {
			return groupingId;
		}

		public boolean isDeveloperOnly() {
			return developerOnly;
		}

		public String getMenuId() {
			if (path == null) {
				return label;
			}
			return Joiner.on('/').join(path) + "/" + label;
		}

		@Override
		public int compareTo(TaskingAdditionalMenuEntry o) {
			return getMenuId().compareTo(o.getMenuId());
		}

	}

}
