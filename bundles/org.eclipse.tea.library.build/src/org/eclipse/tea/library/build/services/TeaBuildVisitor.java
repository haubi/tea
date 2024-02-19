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
package org.eclipse.tea.library.build.services;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.library.build.chain.TeaBuildChain;
import org.eclipse.tea.library.build.chain.TeaBuildElement;

/**
 * A visitor that will process certain elements in the {@link TeaBuildChain}. A
 * visitor may have a method annotated with an {@link Execute} annotation which
 * will be executed prior to actual visitor execution. This allows injection and
 * access to all available TEA constructs.
 */
public interface TeaBuildVisitor {

	/**
	 * Processes a group of elements. The given elements are allowed to be
	 * processed in parallel if possible.
	 * <p>
	 * This method's implementations may never throw. Instead a {@link IStatus}
	 * with the according {@link Exception} must be returned.
	 *
	 * @param elements
	 *            all elements to process
	 * @return the result(s) for those elements. Results must only be present
	 *         for elements the visitor processed.
	 */
	public Map<TeaBuildElement, IStatus> visit(List<TeaBuildElement> elements);

	/**
	 * Helper for implementors allowing to visit a certain sub-type of
	 * {@link TeaBuildElement}, processing each individually.
	 */
	default <T> Map<TeaBuildElement, IStatus> visitEach(List<TeaBuildElement> elements, Class<T> clazz,
			Function<T, IStatus> function) {
		Map<TeaBuildElement, IStatus> results = new TreeMap<>();
		for (TeaBuildElement e : elements) {
			if (clazz.isInstance(e)) {
				IStatus s = function.apply(clazz.cast(e));
				if (s != null) {
					results.put(e, s);
				}
			}
		}
		return results;
	}
}
