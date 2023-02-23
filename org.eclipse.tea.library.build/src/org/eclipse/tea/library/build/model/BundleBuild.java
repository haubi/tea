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

import java.io.File;

import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.jar.ZipExecFactory;
import org.eclipse.tea.library.build.jar.ZipExecInterceptor;

/**
 * Common operations of build elements.
 */
public abstract class BundleBuild<T extends BundleData> {

	/**
	 * associated BundleData element
	 */
	protected final T data;

	protected BundleBuild(T data) {
		this.data = data;
	}

	public final T getData() {
		return data;
	}

	/**
	 * Creates the JAR file for this bundle.
	 */
	public abstract File execJarCommands(ZipExecFactory zip, File distDirectory, String buildVersion,
			JarManager jarManager) throws Exception;

	public abstract File execJarCommands(ZipExecFactory zip, File distDirectory, String buildVersion,
			JarManager jarManager, boolean withSources, ZipExecInterceptor zipExecInterceptor) throws Exception;

	/**
	 * Returns the JAR filename for the specified build version
	 *
	 * @param buildVersion
	 *            build version
	 * @return JAR filename
	 */
	public abstract String getJarFileName(String buildVersion);

}
