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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * contains the elements for one part of an add/create operation
 */
public class ZipExecPart {

	/**
	 * base directory for all relative paths (mandatory)
	 */
	public File sourceDirectory;

	/**
	 * the relative paths of all elements
	 */
	public final List<String> relativePaths = new ArrayList<>();

	/**
	 * exclude Git files
	 */
	public boolean excludeGit;

	private String jarKey;

	public ZipExecPart() {
		// public
	}

	/** copy constructor **/
	public ZipExecPart(ZipExecPart source) {
		sourceDirectory = source.sourceDirectory;
		relativePaths.addAll(source.relativePaths);
		excludeGit = source.excludeGit;
	}

	/**
	 * sort all elements and return the path of the first element
	 */
	String getJarKey() {
		if (jarKey == null) {
			if (relativePaths.isEmpty()) {
				jarKey = "";
			} else {
				Collections.sort(relativePaths, JarComparator.instance);
				jarKey = relativePaths.get(0);
			}
		}
		return jarKey;
	}

}
