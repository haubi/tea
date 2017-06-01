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
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.tea.core.annotations.TaskChainMenuEntry;
import org.eclipse.tea.core.internal.model.TaskingModel;
import org.eclipse.tea.core.internal.model.iface.TaskingContainer;
import org.eclipse.tea.core.internal.model.iface.TaskingItem;
import org.eclipse.tea.core.internal.service.Service;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskingAdditionalMenuEntryProvider;
import org.eclipse.tea.core.services.TaskingAdditionalMenuEntryProvider.TaskingAdditionalMenuEntry;
import org.eclipse.tea.core.services.TaskingMenuDecoration;
import org.eclipse.tea.core.services.TaskingMenuDecoration.TaskingMenuGroupingId;
import org.eclipse.tea.core.services.TaskingMenuDecoration.TaskingMenuPathDecoration;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

/**
 * Aids in creating a {@link TaskingModel}'s root node.
 */
@Creatable
public class TaskingModelCreator {

	@Inject
	@Service
	private List<TaskChain> chains;

	@Inject
	@Service
	private List<TaskingAdditionalMenuEntryProvider> additional;

	@Inject
	@Service
	private List<TaskingMenuDecoration> decorations;

	/**
	 * @return a root {@link TaskingContainer} that can be queried for
	 *         information about all {@link TaskingContainer}s and
	 *         {@link TaskingItem}s in the system.
	 */
	public TaskingContainer createModel() {
		TaskingRootGroupImpl root = new TaskingRootGroupImpl();

		// create all groups with decoration
		for (TaskingMenuDecoration decoration : decorations) {
			for (Field f : decoration.getClass().getDeclaredFields()) {
				TaskingMenuPathDecoration pathDeco = f.getAnnotation(TaskingMenuPathDecoration.class);
				if (pathDeco != null) {
					TaskingContainerImpl impl = root.createGroup(pathDeco.menuPath(), 0);
					impl.setDecoration(f);
				}

				TaskingMenuGroupingId grouping = f.getAnnotation(TaskingMenuGroupingId.class);
				if (grouping != null) {
					TaskingContainerImpl impl = root.createGroup(grouping.menuPath(), 0);
					impl.addGroupingId(f);
				}
			}
		}

		// create all remaining implicit groups and items
		for (TaskChain chain : chains) {
			TaskChainMenuEntry entry = chain.getClass().getAnnotation(TaskChainMenuEntry.class);
			TaskingContainerImpl impl;
			if (entry != null && entry.path().length > 0) {
				impl = root.createGroup(entry.path(), 0);
			} else {
				impl = root;
			}

			impl.addChild(new TaskingItemImpl(chain));
		}

		// create all the additional items from providers
		for (TaskingAdditionalMenuEntryProvider provider : additional) {
			for (TaskingAdditionalMenuEntry entry : provider.getAdditionalEntries()) {
				TaskingContainerImpl impl;
				if (entry.getMenuPath() != null && entry.getMenuPath().length > 0) {
					impl = root.createGroup(entry.getMenuPath(), 0);
				} else {
					impl = root;
				}

				impl.addChild(new TaskingAdditionalItemImpl(entry));
			}
		}

		return root;
	}

	/**
	 * @param serviceClass
	 *            any service class that has a {@link Component} annotation
	 * @return the {@link Constants#SERVICE_RANKING} (parsed to int) of the
	 *         {@link Component} annotation on the given {@link Class}.
	 */
	public static int getServiceRanking(Class<?> serviceClass) {
		Component comp = serviceClass.getAnnotation(Component.class);
		if (comp != null && comp.property().length != 0) {
			for (String prop : comp.property()) {
				if (prop.startsWith(Constants.SERVICE_RANKING)) {
					return parseRanking(prop.substring(prop.indexOf("=")).trim());
				}
			}
		}
		return 0;
	}

	private static int parseRanking(String p) {
		int result = 0;
		try {
			result = Integer.parseInt(p.toString());
		} catch (Exception e) {
			// invalid ranking - ignore - OSGi will warn in this case.
		}
		return result;
	}

}
