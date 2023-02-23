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
package org.eclipse.tea.library.build.model;

import java.util.Comparator;

/**
 * Compares the order of known feature attributes.
 */
final class FeatureAttributeComparator implements Comparator<String> {

	private static final String[] NAMES_FEATURE = { "id", "label", "version", "provider-name" };

	private static final String[] NAMES_PLUGIN = { "id", "os", "ws", "arch", "download-size", "install-size", "version",
			"fragment", "unpack" };

	private static final String[] NAMES_IMPORT = { "plugin", "feature", "version", "match" };

	static FeatureAttributeComparator fromTag(String tagName) {
		if (tagName.equals("feature")) {
			return new FeatureAttributeComparator(NAMES_FEATURE);
		}
		if (tagName.equals("plugin")) {
			return new FeatureAttributeComparator(NAMES_PLUGIN);
		}
		if (tagName.equals("import")) {
			return new FeatureAttributeComparator(NAMES_IMPORT);
		}
		return new FeatureAttributeComparator(null);
	}

	private final String[] names;

	private FeatureAttributeComparator(String[] names) {
		this.names = names;
	}

	private int getOrderNumber(String key) {
		int result = 0;
		for (String name : names) {
			if (name.equals(key)) {
				return result;
			}
			++result;
		}
		return Integer.MAX_VALUE;
	}

	@Override
	public int compare(String o1, String o2) {
		if (names != null) {
			int num1 = getOrderNumber(o1);
			int num2 = getOrderNumber(o2);
			if (num1 < num2) {
				return -1;
			}
			if (num1 > num2) {
				return 1;
			}
		}
		return o1.compareTo(o2);
	}

}
