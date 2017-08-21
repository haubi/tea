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
package org.eclipse.tea.library.build.chain;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tea.library.build.services.TeaElementFailurePolicy;
import org.eclipse.tea.library.build.services.TeaElementFailurePolicy.FailurePolicy;

/**
 * Base class for a single build element of a {@link TeaBuildChain}.
 */
@TeaElementFailurePolicy(FailurePolicy.USE_THRESHOLD)
public abstract class TeaBuildElement implements Comparable<TeaBuildElement> {

	/** wire to this element, currently can be the same for all callers */
	private final TeaDependencyWire selfWire = new TeaDependencyWire(this);

	/** all dependencies to other elements */
	private final Set<TeaDependencyWire> dependencyWires = new TreeSet<>();

	/** build order; a negative value if not yet calculated */
	private int buildOrder = -1;

	/**
	 * Returns the internal name of this element. This name must be unique
	 * throughout the system!
	 */
	public abstract String getName();

	/**
	 * Returns the build order value for this element.
	 *
	 * @returns the build order; the builds must run in ascending order, cycles
	 *          are forbidden
	 */
	protected final int getBuildOrder() {
		if (buildOrder >= 0) {
			return buildOrder;
		}

		if (buildOrder == -2) {
			// already in calculation!
			throw new IllegalStateException("circular dependency detected in " + this);
		}

		// set to -2 to detect circular dependencies
		buildOrder = -2;

		// calculate the build order
		buildOrder = calcBuildOrder(1);

		return buildOrder;
	}

	/**
	 * Calculates the build order value for this element.
	 *
	 * @param minValue
	 *            minimal order value
	 * @returns the build order (>=minValue)
	 */
	protected int calcBuildOrder(int minValue) {
		int result = minValue;

		for (TeaDependencyWire wire : dependencyWires) {
			TeaBuildElement target = wire.getTarget();

			if (target instanceof TeaUnhandledElement) {
				throw new RuntimeException("depenendecy from " + getName() + " to " + target.getName()
						+ " not possible. Target unhandled.");
			}

			int depOrder = target.getBuildOrder();
			if (depOrder >= result) {
				result = depOrder + 1;
			}
		}

		return result;
	}

	/**
	 * Add a dependency from this {@link TeaBuildElement} to another one.
	 *
	 * @param wire
	 *            the wire to the opposite {@link TeaBuildElement}.
	 */
	public void addDependencyWire(TeaDependencyWire wire) {
		Assert.isTrue(buildOrder == -1, "may add dependencies only before order calculation");
		dependencyWires.add(wire);
	}

	/**
	 * @return all {@link TeaDependencyWire}s to other {@link TeaBuildElement}s.
	 */
	public Set<TeaDependencyWire> getDependencyWires() {
		return dependencyWires;
	}

	/**
	 * @return a {@link TeaDependencyWire} to this {@link TeaBuildElement} that
	 *         can be added to another {@link TeaBuildElement}.
	 */
	public TeaDependencyWire createWire() {
		// once more contextual information is required on dependency wires, we
		// can resort to creating wires on the fly here.
		return selfWire;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ", name=" + getName();
	}

	@Override
	public int compareTo(TeaBuildElement o) {
		return getName().compareTo(o.getName());
	}

}
