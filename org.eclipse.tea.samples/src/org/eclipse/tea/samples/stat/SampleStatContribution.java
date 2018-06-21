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

	@TaskingStatisticProvider(qualifier = "weather")
	public Map<String, Object> getAdditionalInfo() {
		Map<String, Object> test = new TreeMap<>();
		Map<String, String> temp = new TreeMap<>();
		temp.put("celsius", "34");
		temp.put("fahrenh", "93");
		
		test.put("temps", temp);
		test.put("hot", "true");
		return test;
	}

}
