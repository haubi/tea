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
package org.eclipse.tea.library.build.jar;

import java.util.Comparator;

/**
 * compares the root elements of a JAR file, to move meta-data at first position
 */
final class JarComparator implements Comparator<String> {

	static final JarComparator instance = new JarComparator();

	@Override
	public int compare(String o1, String o2) {
		int result;

		// check for 'META-INF'
		result = check(o1, o2, "META-INF");
		if (result != 0) {
			return result;
		}

		// check for 'plugin.xml'
		result = check(o1, o2, "plugin.xml");
		if (result != 0) {
			return result;
		}

		// OK, nothing special to do
		return o1.compareTo(o2);
	}

	private static int check(String o1, String o2, String token) {
		boolean meta1 = o1.startsWith(token);
		boolean meta2 = o2.startsWith(token);
		if (meta1 && !meta2) {
			return -1;
		}
		if (meta2 && !meta1) {
			return 1;
		}
		return 0;
	}

}
