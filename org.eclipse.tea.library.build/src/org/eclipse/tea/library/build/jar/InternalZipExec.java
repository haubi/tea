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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.util.FileUtils;

/**
 * ZipExec implementation which calls the JRE ZIP library.
 */
public class InternalZipExec extends BaseZipExec {

	private final byte[] BUFFER = new byte[10240];
	private final TaskingLog log;

	public InternalZipExec(TaskingLog log) {
		this.log = log;
	}

	@Override
	protected void doCreateZip() {
		try {
			FileOutputStream fos = new FileOutputStream(zipFile);
			ZipOutputStream zos = new ZipOutputStream(fos);
			try {
				for (ZipExecPart part : parts) {
					addZip(part, zos);
				}
			} finally {
				zos.close();
			}
		} catch (IOException e) {
			throw new IllegalStateException("cannot create " + zipFile, e);
		}
	}

	private void addZip(ZipExecPart part, ZipOutputStream zos) {
		for (String relPath : part.relativePaths) {
			if (".".equals(relPath)) {
				addEntry(part, zos, part.sourceDirectory, null);
			} else {
				File source = new File(part.sourceDirectory, relPath);
				addEntry(part, zos, source, relPath);
			}
		}
	}

	private void addEntry(ZipExecPart part, ZipOutputStream zos, File source, String entryName) {
		if (!source.exists()) {
			// requested input does not exist. this is worth a warning only.
			// command line zip ignores it completely.
			log.warn("ZIP input " + source + " does not exist");
			return;
		}

		if (source.isDirectory()) {
			if (entryName != null) {
				int lastIndex = entryName.length() - 1;
				if (entryName.charAt(lastIndex) == '/') {
					entryName = entryName.substring(0, lastIndex);
				}
			}

			String[] children = source.list();
			Arrays.sort(children);

			for (String childName : children) {
				if (part.excludeGit) {
					if (GITIGNORE.equals(childName)) {
						continue;
					}
				}
				File child = new File(source, childName);
				if (entryName == null) {
					addEntry(part, zos, child, childName);
				} else {
					addEntry(part, zos, child, entryName + '/' + childName);
				}
			}
			return;
		}

		// add file
		try {
			ZipEntry ze = new ZipEntry(entryName);
			ze.setTime(source.lastModified());
			// TODO what about file permissions?

			zos.putNextEntry(ze);
			FileInputStream fis = new FileInputStream(source);
			try {
				int count;
				while ((count = fis.read(BUFFER)) >= 0) {
					zos.write(BUFFER, 0, count);
				}
			} finally {
				fis.close();
			}
			zos.closeEntry();
		} catch (Exception e) {
			throw new IllegalStateException("cannot add " + entryName, e);
		}
	}

	/**
	 * Extracts a ZIP file
	 *
	 * @param zipFile
	 *            the ZIP file
	 * @param destDir
	 *            the destination directory
	 */
	@Override
	public void unzip(File zipFile, File destDir) throws IOException {
		try (ZipFile file = new ZipFile(zipFile)) {
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File realFile = new File(destDir, entry.getName());
				if (entry.isDirectory()) {
					FileUtils.mkdirs(realFile);
					continue;
				}
				FileUtils.mkdirs(realFile.getParentFile());
				try (InputStream is = file.getInputStream(entry);
						FileOutputStream os = new FileOutputStream(realFile)) {
					int i;
					while ((i = is.read(BUFFER)) != -1) {
						os.write(BUFFER, 0, i);
					}
				}
			}
		}
	}

}
