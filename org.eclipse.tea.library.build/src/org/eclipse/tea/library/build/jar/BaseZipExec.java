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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Base for common ZIP executors.
 */
abstract class BaseZipExec implements ZipExec {

	protected static final String GITIGNORE = ".gitignore";

	protected File zipFile;
	protected final List<ZipExecPart> parts = new ArrayList<>();
	protected boolean isJar;

	@Override
	public void setZipFile(File zipFile) {
		this.zipFile = zipFile;
	}

	@Override
	public void setJarMode(boolean isJar) {
		this.isJar = isJar;
	}

	@Override
	public void addPart(ZipExecPart part) {
		parts.add(part);
	}

	@Override
	public final void createZip() {
		if (isJar) {
			// move meta-data at first position
			Collections.sort(parts, new Comparator<ZipExecPart>() {

				@Override
				public int compare(ZipExecPart o1, ZipExecPart o2) {
					String key1 = o1.getJarKey();
					String key2 = o2.getJarKey();
					return JarComparator.instance.compare(key1, key2);
				}
			});
		}
		doCreateZip();
	}

	/**
	 * processes the ZipExecPart list
	 */
	protected abstract void doCreateZip();

}
