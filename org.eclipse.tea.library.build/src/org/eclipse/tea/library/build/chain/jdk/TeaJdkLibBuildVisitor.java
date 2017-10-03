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
package org.eclipse.tea.library.build.chain.jdk;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.chain.TeaBuildElement;
import org.eclipse.tea.library.build.services.TeaBuildVisitor;
import org.osgi.service.component.annotations.Component;

@Component
public class TeaJdkLibBuildVisitor implements TeaBuildVisitor {

	private TaskingLog log;

	@Execute
	public void prepare(TaskingLog log) {
		this.log = log;
	}
	
	@Override
	public Map<TeaBuildElement, IStatus> visit(List<TeaBuildElement> elements) {
		return visitEach(elements, TeaJdkLibBuildElement.class, (e) -> {
			try {
				e.execute(log);
			} catch (Exception e1) {
				log.error("error while copying JDK library", e);
			}
			return Status.OK_STATUS;
		});
	}

}
