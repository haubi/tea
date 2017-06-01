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
package org.eclipse.tea.samples.taskchain;

import org.eclipse.tea.core.annotations.TaskCaptureStdOutput;
import org.eclipse.tea.core.services.TaskProgressTracker.TaskProgressProvider;

@TaskCaptureStdOutput
public class DemoTaskInheritance extends DemoTask {

	@Override
	protected void doSomething() {
		System.err.println("SOMETHING");
	}

	public static final class InnerInheritance extends DemoTaskInheritance {

		@TaskProgressProvider
		public int getWork() {
			return 1;
		}

	}

}
