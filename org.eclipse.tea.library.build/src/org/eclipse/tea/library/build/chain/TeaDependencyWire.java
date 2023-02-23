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
package org.eclipse.tea.library.build.chain;

/**
 * Represents a dependency to a certain {@link TeaBuildElement} target.
 */
public class TeaDependencyWire implements Comparable<TeaDependencyWire> {

	private final TeaBuildElement target;

	/**
	 * Create a new wire to the given {@link TeaBuildElement}. This wire can be
	 * added to another {@link TeaBuildElement} to create a dependency between
	 * the two.
	 */
	public TeaDependencyWire(TeaBuildElement target) {
		this.target = target;
	}

	/**
	 * @return the opposite side of the dependency.
	 */
	public TeaBuildElement getTarget() {
		return target;
	}

	@Override
	public int compareTo(TeaDependencyWire o) {
		return target.compareTo(o.target);
	}

}
