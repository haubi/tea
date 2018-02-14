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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.util.FileUtils;

import com.google.common.base.Splitter;

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
				Map<ZipEntry, File> entries = new TreeMap<>((a, b) -> a.getName().compareTo(b.getName()));
				for (ZipExecPart part : parts) {
					addZip(part, entries);
				}
				write(zos, entries);
			} finally {
				zos.close();
			}
		} catch (IOException e) {
			throw new IllegalStateException("cannot create " + zipFile, e);
		}
	}

	private void addZip(ZipExecPart part, Map<ZipEntry, File> entries) {
		for (String relPath : part.relativePaths) {
			if (".".equals(relPath)) {
				addEntry(part, entries, part.sourceDirectory, null);
			} else {
				File source = new File(part.sourceDirectory, relPath);
				addEntry(part, entries, source, relPath);
			}
		}
	}

	private void addEntry(ZipExecPart part, Map<ZipEntry, File> entries, File source, String entryName) {
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
					addEntry(part, entries, child, childName);
				} else {
					addEntry(part, entries, child, entryName + '/' + childName);
				}
			}
			return;
		}

		// add file
		ZipEntry ze = new ZipEntry(entryName);
		ze.setTime(source.lastModified());
		entries.put(ze, source);
	}

	private void write(ZipOutputStream zos, Map<ZipEntry, File> entries) {
		// sort entries, create entries for directories
		entries.keySet().stream().sorted((a, b) -> a.getName().compareTo(b.getName())).forEach((e) -> {
			List<String> segments = Splitter.on('/').splitToList(e.getName());
			if (segments.size() > 1) {
				String rel = null;
				for (int i = 0; i < segments.size() - 1; ++i) {
					String seg = segments.get(i);
					if (rel == null) {
						rel = seg + "/";
					} else {
						rel = rel + seg + "/";
					}
					ZipEntry de = new ZipEntry(rel);
					de.setTime(System.currentTimeMillis());
					entries.put(de, null);
				}
			}
		});

		for (Entry<ZipEntry, File> ze : entries.entrySet()) {
			try {
				zos.putNextEntry(ze.getKey());
				if (!ze.getKey().isDirectory()) {
					FileInputStream fis = new FileInputStream(ze.getValue());
					try {
						int count;
						while ((count = fis.read(BUFFER)) >= 0) {
							zos.write(BUFFER, 0, count);
						}
					} finally {
						fis.close();
					}
				}
				zos.closeEntry();
			} catch (Exception e) {
				throw new IllegalStateException("cannot add " + ze.getValue(), e);
			}
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
