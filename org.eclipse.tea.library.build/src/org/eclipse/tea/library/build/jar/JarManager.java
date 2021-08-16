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
package org.eclipse.tea.library.build.jar;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.model.BundleBuild;
import org.eclipse.tea.library.build.model.BundleData;
import org.eclipse.tea.library.build.services.TeaBuildVersionService;
import org.eclipse.tea.library.build.util.StringHelper;

/**
 * Knows how to build JAR files with correct Version, etc.
 */
public final class JarManager {

	/**
	 * defines the replacement string for the date & time in the version
	 * qualifier.
	 */
	public static final String QUALIFIER_REP = "%D";

	private final ZipExecFactory zipExecFactory;
	private String qualifier;

	private final TaskingLog log;
	private final TeaBuildVersionService bvService;

	/**
	 * Creates the JAR manager.
	 */
	public JarManager(TaskingLog log, ZipExecFactory zipExecFactory, TeaBuildVersionService bvService) {
		this.log = log;
		this.zipExecFactory = zipExecFactory;
		this.bvService = bvService;

		reset();
	}

	public void reset() {
		final Date now = new Date();

		// build qualifier: timestamp with an accuracy of one minute
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");

		qualifier = bvService.getQualifierFormat().replace(QUALIFIER_REP, format.format(now));
		log.debug("build qualifier: " + qualifier);
	}

	/**
	 * Returns the unique build qualifier. Use with care!
	 */
	public String getQualifier() {
		return qualifier;
	}

	/**
	 * Returns the fully qualified version of this build. Use with care!
	 */
	public String getBuildVersion() {
		return bvService.getBuildVersion().replace("qualifier", qualifier);
	}

	/**
	 * Return the current full version used during this build for a certain
	 * bundle.
	 */
	public String getBundleVersion(BundleData data) {
		return createNewVersion(data);
	}

	/**
	 * Calculates a new bundle version.
	 */
	private String createNewVersion(BundleData data) {
		String origVersion = data.getBundleVersion();
		String newVersion = StringHelper.replaceQualifier(origVersion, qualifier);
		return newVersion;
	}

	/**
	 * Returns the ZIP execution factory.
	 */
	public ZipExecFactory getZipExecFactory() {
		return zipExecFactory;
	}

	/**
	 * Creates the JAR file for the specified bundle.
	 *
	 * @param bundle
	 *            the bundle
	 * @param destDirectory
	 *            destination directory
	 * @return JAR file
	 */
	public File execJarCommands(BundleBuild<?> bundle, File destDirectory) throws Exception {
		boolean withSource = false;
		return execJarCommands(bundle, destDirectory, withSource);
	}

	public File execJarCommands(BundleBuild<?> bundle, File destDirectory, boolean withSource) throws Exception {
		String version = createNewVersion(bundle.getData());
		return bundle.execJarCommands(zipExecFactory, destDirectory, version, this, withSource);
	}

}
