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
package org.eclipse.tea.library.build.model;

/**
 * Represents a single attribute in a MANIFEST.MF file.
 */
final class ManifestAttribute {

	final String name;
	final ParameterValue[] values;
	int order = Integer.MAX_VALUE;

	ManifestAttribute(String name, String[] values) {
		this(name, ParameterValue.fromList(values));
	}

	ManifestAttribute(String name, String value) {
		this(name, new ParameterValue[] { new ParameterValue(value) });
	}

	ManifestAttribute(String name, ParameterValue[] values) {
		this.name = name;
		this.values = values;
	}

}
