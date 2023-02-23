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
package org.eclipse.tea.core.internal.model;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.tea.core.internal.model.iface.TaskingContainer;
import org.eclipse.tea.core.internal.model.impl.TaskingModelCreator;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;

import com.google.common.base.Strings;

/**
 * Holds information about all (currently) available {@link TaskChain}s.
 */
@Creatable
public class TaskingModel {

	private final TaskingModelCreator creator;

	@Inject
	public TaskingModel(TaskingModelCreator creator) {
		this.creator = creator;
	}

	/**
	 * @return the root of the Tasking model. The root is re-created on every
	 *         request.
	 */
	public TaskingContainer getRootGroup() {
		return creator.createModel();
	}

	/**
	 * @param task
	 *            the task to calculate a nice name for
	 * @return the best human readable name computable for the task
	 */
	public static String getTaskName(Object task) {
		return toTaskingObjectName(task);
	}

	/**
	 * @return the description of the {@link TaskChain} to be executed
	 */
	public static String getTaskChainName(TaskChain chain) {
		TaskChainId id = chain.getClass().getAnnotation(TaskChainId.class);
		if (id == null) {
			return TaskingModel.toTaskingObjectName(chain);
		}

		return id.description();
	}

	private static String toTaskingObjectName(Object obj) {
		// 1: @Named annotation
		Named named = obj.getClass().getAnnotation(Named.class);
		if (named != null) {
			return named.value();
		}
		
		// 2: toString if it is a real instance
		if (Arrays.asList(obj.getClass().getMethods()).stream()
				.filter((m) -> m.getName().equals("toString") && !m.getDeclaringClass().equals(Object.class)).findAny()
				.isPresent()) {
			return obj.toString();
		}

		// 3: class name
		String name = obj.getClass().getSimpleName();
		if (Strings.isNullOrEmpty(name)) {
			return obj.getClass().getName();
		}
		return name;
	}

}
