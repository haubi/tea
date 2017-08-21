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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.tea.library.build.util.FileUtils;

/**
 * ZipExec implementation which calls an external ZIP application.
 */
public class ExternalZipExec extends BaseZipExec {

	final File zipExe;

	public ExternalZipExec(File zipExe) {
		this.zipExe = zipExe;
	}

	@Override
	protected void doCreateZip() {
		for (ZipExecPart part : parts) {
			addZip(part);
		}
	}

	private void addZip(ZipExecPart part) {
		List<String> cmdTokens = new ArrayList<>();
		cmdTokens.add(zipExe.getAbsolutePath());
		cmdTokens.add("-rq");
		cmdTokens.add(zipFile.getAbsolutePath());

		final Collection<String> elements = part.relativePaths;
		if (elements.isEmpty()) {
			throw new IllegalStateException("no ZIP elements set");
		}
		for (String element : elements) {
			cmdTokens.add(element);
		}

		if (part.excludeGit) {
			cmdTokens.add("-x");
			cmdTokens.add("*/" + GITIGNORE);
		}

		final File directory = part.sourceDirectory;
		final String[] cmdArray = cmdTokens.toArray(new String[cmdTokens.size()]);
		if (execCmd(directory, cmdArray) != 0) {
			throw new IllegalStateException("command failed, dir=" + directory + ", cmd=" + Arrays.toString(cmdArray));
		}
	}

	private int execCmd(final File directory, final String... cmdArray) {
		try {
			Process proc = Runtime.getRuntime().exec(cmdArray, null, directory);
			try {
				return proc.waitFor();
			} finally {
				proc.destroy();
			}
		} catch (Exception ex) {
			throw new IllegalStateException("command failed, dir=" + directory + ", cmd=" + Arrays.toString(cmdArray),
					ex);
		}
	}

	@Override
	public void unzip(File zip, File destDir) throws IOException {
		FileUtils.mkdirs(destDir);

		File unzip = new File(zipExe.getParentFile(), "unzip");
		List<String> cmds = new ArrayList<>();
		cmds.add(unzip.getAbsolutePath());
		cmds.add("-q");
		cmds.add(zip.getAbsolutePath());

		String[] cmdArray = cmds.toArray(new String[cmds.size()]);

		if (execCmd(destDir, cmdArray) != 0) {
			throw new IllegalStateException("command failed, dir=" + destDir + ", cmd=" + Arrays.toString(cmdArray));
		}
	}

}
