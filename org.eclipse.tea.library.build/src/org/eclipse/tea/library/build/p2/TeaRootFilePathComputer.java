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
package org.eclipse.tea.library.build.p2;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.internal.p2.publisher.FileSetDescriptor;

/** Holds the root files for a given configuration to publish */
@SuppressWarnings("restriction")
public class TeaRootFilePathComputer implements IPathComputer {
	private final Map<File, IPath> files = new HashMap<>();
	private final FileSetDescriptor descriptor;

	public TeaRootFilePathComputer(String config) {
		descriptor = new FileSetDescriptor("root." + config, config);
	}

	@Override
	public IPath computePath(File source) {
		return files.get(source).append(source.getName());
	}

	@Override
	public void reset() {
	}

	/**
	 * Adds the given file to this path computer
	 * 
	 * @param sourceFile
	 *            the file to add
	 * @param destDir
	 *            the relative destination path
	 */
	public void addRootfile(File sourceFile, IPath destDir) {
		descriptor.addFiles(new File[] { sourceFile });
		files.put(sourceFile, destDir);
	}

	/**
	 * Returns the the files added to this computer
	 */
	public FileSetDescriptor getDescriptor() {
		return descriptor;
	}

}