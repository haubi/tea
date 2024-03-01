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
package org.eclipse.tea.core.ui.internal.run;

import org.eclipse.core.runtime.Assert;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.annotations.TaskChainMenuEntry;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.core.ui.DevelopmentMenuDecoration;
import org.eclipse.tea.core.ui.SelectTaskChainDialog;
import org.eclipse.tea.core.ui.annotations.TaskChainUiInit;
import org.osgi.service.component.annotations.Component;

@TaskChainId(description = "Run any TaskChain...", alias = "RunAny")
@TaskChainMenuEntry(development = true, path = DevelopmentMenuDecoration.MENU_DEVELOPMENT)
@Component
public class RunTaskChain implements TaskChain {

	private TaskChain realChain;

	@TaskChainUiInit
	public void initFromUI(SelectTaskChainDialog dlg) {
		if (dlg.open() == SelectTaskChainDialog.OK) {
			realChain = dlg.getSelectedChain();

			Assert.isLegal(realChain.getClass() != getClass(), "Cannot run self");
		} else {
			// service is reused. thus the value will still be set.
			realChain = null;
		}
	}

	@TaskChainContextInit
	public void initChain(TaskExecutionContext context) {
		if (realChain != null) {
			ContextInjectionFactory.invoke(realChain, TaskChainContextInit.class, context.getContext());
		}
	}

}
