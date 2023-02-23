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
package org.eclipse.tea.library.build.util;

/**
 * Helper to ease handling of strings
 */
public class StringHelper {

	private static final String QUALI = "qualifier";

	/**
	 * Compares two string objects. {@code null} is treated as smaller than any
	 * not-null value.
	 *
	 * @param s1
	 *            the first string, or {@code null}
	 * @param s2
	 *            the second string, or {@code null}
	 * @return see {@link String#compareTo(String)}
	 */
	public static int compare(String s1, String s2) {
		if (s1 == s2) {
			return 0;
		}
		if (s1 == null) {
			return -1;
		}
		if (s2 == null) {
			return 1;
		}
		return s1.compareTo(s2);
	}

	/**
	 * Updates a version string and replaces 'qualifier' with the give string
	 *
	 * @param input
	 *            the input string
	 * @param qualifier
	 *            qualifier to use
	 * @return the updated string
	 */
	public static String replaceQualifier(String input, String qualifier) {
		if (input.contains(QUALI)) {
			return input.replace(QUALI, qualifier);
		}
		return input;
	}

	/**
	 * Checks whether a given string is null or empty
	 *
	 * @param s
	 *            string to check
	 * @return <code>true</code> if the string is null or empty
	 */
	public static boolean isNullOrEmpty(String s) {
		return s == null || s.isEmpty();
	}

}
