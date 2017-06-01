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
package org.eclipse.tea.samples.stat;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.tea.core.services.TaskingStatisticsContribution;
import org.osgi.service.component.annotations.Component;

@Component
public class SampleStatContribution implements TaskingStatisticsContribution {

	@TaskingStatisticProvider(qualifier = "important")
	public Map<String, String> getAdditionalInfo() {
		Map<String, String> test = new TreeMap<>();
		test.put("Some", "Value");
		test.put("To", "Remember");
		return test;
	}

}
