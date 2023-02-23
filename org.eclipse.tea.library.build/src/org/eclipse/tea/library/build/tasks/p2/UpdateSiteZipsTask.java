/*******************************************************************************
 *  Copyright (c) 2018 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.library.build.tasks.p2;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.p2.UpdateSiteManager;

/**
 * Creates the update site ZIP files for all sites which are known by the global
 * update site manager.
 */
public class UpdateSiteZipsTask {

	@Execute
	public void run(TaskingLog log, UpdateSiteManager um) throws Exception {
		um.createUpdateSiteZips(log);
	}

}
