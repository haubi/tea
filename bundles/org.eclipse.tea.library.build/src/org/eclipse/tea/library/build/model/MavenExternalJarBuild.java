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
 * Represents a library that is fetched from an external maven repository.
 */
public class MavenExternalJarBuild implements Comparable<MavenExternalJarBuild> {

	private final String coordinates;

	public MavenExternalJarBuild(String coordinates) {
		this.coordinates = coordinates;
	}

	public String getCoordinates() {
		return coordinates;
	}

	@Override
	public int compareTo(MavenExternalJarBuild o) {
		return coordinates.compareTo(o.coordinates);
	}

	@Override
	public String toString() {
		return "MVN [" + getCoordinates() + "]";
	}

}
