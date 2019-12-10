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

/**
 * Service that provides version information to various build components.
 */
public interface TeaBuildVersionService {

	/**
	 * Provides a version to use when creating versioned artifacts on the fly.
	 *
	 * @return a version string in the format 'x.x.x.qualifier' - the
	 *         'qualifier' is a plain text string which should be as is. It will
	 *         be replaced with the real qualifier later.
	 */
	public String getBuildVersion();

	/**
	 * @return a version string used for "official" display purposes. E.g. a
	 *         branded project specific version of a main deliverable.
	 */
	public String getDisplayVersion();

	/**
	 * Provides a format for qualifiers which are used to replace the
	 * 'qualifier' in version strings
	 *
	 * @return the qualifier format for this build. The {@link String} '%D' will
	 *         be replaced with current date and time.
	 */
	public String getQualifierFormat();

	/**
	 * @return the vendor to use for artifacts created on the fly during builds.
	 */
	public String getDefaultVendor();

	public static class DefaultBuildVersionService implements TeaBuildVersionService {

		@Override
		public String getBuildVersion() {
			return "1.0.0.qualifier";
		}

		@Override
		public String getDisplayVersion() {
			return getBuildVersion();
		}

		@Override
		public String getQualifierFormat() {
			return "%D";
		}

		@Override
		public String getDefaultVendor() {
			return "Default Vendor";
		}

	}

}
