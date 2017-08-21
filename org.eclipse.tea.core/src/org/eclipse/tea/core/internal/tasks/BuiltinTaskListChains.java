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
package org.eclipse.tea.core.internal.tasks;

import java.util.List;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.Service;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.core.services.TaskingLog;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Simple task that lists all registered task chain services
 */
public class BuiltinTaskListChains {

	@Execute
	public void listChains(@Service List<TaskChain> allChains, TaskingLog log) {
		log.info(String.format("%1$40s | %2$s", "Task Chain Name", "Known Aliases"));
		log.info(
				"-----------------------------------------|----------------------------------------------------------");
		for (TaskChain c : allChains) {
			TaskChainId annotation = c.getClass().getAnnotation(TaskChainId.class);
			if (annotation != null) {
				log.info(String.format("%1$40s | %2$s", annotation.description(),
						Joiner.on(", ").join(Lists.asList(c.getClass().getName(), annotation.alias()))));
			} else {
				log.info(String.format("%1$40s | %2$s", "<no name>", c.getClass().getName()));
			}
		}
	}

}
