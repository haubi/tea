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

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.equinox.internal.p2.metadata.expression.ExpressionFactory;
import org.eclipse.equinox.internal.p2.metadata.expression.parser.LDAPFilterParser;
import org.eclipse.equinox.p2.metadata.expression.IFilterExpression;

/**
 * Contains all the supported platforms. Required for Feature generation and
 * Product build.
 */
@SuppressWarnings("restriction")
public final class PlatformTriple {

	/**
	 * the operating system value
	 */
	public final String os;

	/**
	 * the window system value
	 */
	public final String ws;

	/**
	 * the processor architecture value
	 */
	public final String arch;

	/**
	 * the name of the locale on which Eclipse platform will run; maybe an empty
	 * string
	 */
	public final String nl;

	public static final PlatformTriple WIN32 = new PlatformTriple("win32", "win32", "x86", "");
	public static final PlatformTriple WIN64 = new PlatformTriple("win32", "win32", "x86_64", "");
	public static final PlatformTriple LINUX32 = new PlatformTriple("linux", "gtk", "x86", "");
	public static final PlatformTriple LINUX64 = new PlatformTriple("linux", "gtk", "x86_64", "");

	/**
	 * Returns all platforms which are currently supported by WAMAS.
	 */
	public static PlatformTriple[] getAllPlatforms() {
		return new PlatformTriple[] { WIN32, WIN64, LINUX32, LINUX64 };
	}

	private final Map<String, String> map = new TreeMap<>();

	private PlatformTriple(String os, String ws, String arch, String nl) {
		this.os = os;
		this.ws = ws;
		this.arch = arch;
		this.nl = nl;

		map.put("osgi.os", os);
		map.put("osgi.ws", ws);
		map.put("osgi.arch", arch);
		map.put("osgi.nl", nl);
	}

	/**
	 * Returns a valid fragment name suffix, which contains operating system and
	 * architecture.
	 */
	public String getFragmentArchSuffix() {
		return "." + os + '.' + arch;
	}

	/**
	 * Like {@link #getFragmentArchSuffix()} but also contains the windowing
	 * system.
	 */
	public String getFragmentWsSuffix() {
		return "." + os + '.' + ws + '.' + arch;
	}

	/**
	 * Returns the platform triple of the current platform, extended by the name
	 * of the locale.
	 */
	public String[] getTarget() {
		return new String[] { os, ws, arch, nl };
	}

	/**
	 * Returns a String containing osgi.os, osgi.ws, osgi.arch and osgi.nl
	 */
	@Override
	public String toString() {
		return "[osgi.os:" + os + "] [osgi.ws:" + ws + "] [osgi.arch:" + arch + "] [osgi.nl:" + nl + "]";
	}

	/**
	 * returns the platform triple for build.properties
	 *
	 * @return String containing osgi.os, osgi.ws, and osgi.arch
	 */
	public String toStringBuildPropStyle() {
		return os + "," + ws + "," + arch;
	}

	/**
	 * returns the platform triple for commandline
	 *
	 * @return String containing osgi.ws, osgi.os, and osgi.arch
	 */
	public String toStringCmdLine() {
		return ws + "." + os + "." + arch;
	}

	/**
	 * returns all supported targets in build.properties syntax
	 *
	 * @return String of all supported targets. We need this syntax for further
	 *         use in some setup scripts.
	 */
	public static String getAllTargetsBuildPropStyle() {
		return WIN32.toStringBuildPropStyle() + "&" + WIN64.toStringBuildPropStyle() + "&"
				+ LINUX32.toStringBuildPropStyle() + "&" + LINUX64.toStringBuildPropStyle();
	}

	/**
	 * returns all supported targets in commandline syntax
	 *
	 * @return String of all supported targets. We need this syntax for creating
	 *         the correct update site.
	 */
	public static String getAllTargetsCommandLineStyle() {
		return WIN32.toStringCmdLine() + "," + WIN64.toStringCmdLine() + "," + LINUX32.toStringCmdLine() + ","
				+ LINUX64.toStringCmdLine();
	}

	/**
	 * Checks if this platform triple matches the given platform filter.
	 */
	public boolean matchFilter(String filter) {
		final LDAPFilterParser parser = new LDAPFilterParser(ExpressionFactory.INSTANCE);
		final IFilterExpression root = parser.parse(filter);

		boolean match = root.match(map);

		return match;
	}
}
