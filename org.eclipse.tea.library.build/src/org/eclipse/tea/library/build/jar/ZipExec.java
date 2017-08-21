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
import java.io.IOException;

/**
 * ZIP executor.
 */
public interface ZipExec {

	/**
	 * Defines path and name of the ZIP file.
	 */
	public void setZipFile(File zipFile);

	/**
	 * Adds an element for the ZIP operation.
	 *
	 * @param part
	 *            one part of the operation
	 */
	public void addPart(ZipExecPart part);

	/**
	 * Defines if we need special JAR handling.
	 */
	public void setJarMode(boolean isJar);

	/**
	 * Creates the ZIP file.
	 */
	public void createZip();

	/**
	 * Extracts the given ZIP file
	 *
	 * @param zip
	 *            the ZIP to extract
	 * @param destDir
	 *            the directory to extract to.
	 */
	public void unzip(File zip, File destDir) throws IOException;

}
