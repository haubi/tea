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
package org.eclipse.tea.library.build.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

/**
 * Helper concerning files and directories;
 */
public final class FileUtils {

	/**
	 * Deletes a directory recursively.
	 *
	 * @param directory
	 *            directory to delete
	 */
	public static void deleteDirectory(File directory) {
		if (!directory.exists()) {
			return;
		}
		if (!directory.isDirectory()) {
			throw new IllegalStateException("not a directory: " + directory);
		}
		List<String> ignoredFiles = Collections.emptyList();
		deleteDirectoryContent(directory, ignoredFiles);
		if (!directory.delete()) {
			throw new IllegalStateException("cannot delete " + directory);
		}
	}

	/**
	 * Delete the content of the directory
	 *
	 * @param directory
	 *            {@link File} representing the directory, which content should
	 *            be deleted
	 * @param ignoredFileNames
	 *            a list of filenames, which should not be deleted
	 */
	public static void deleteDirectoryContent(File directory, List<String> ignoredFileNames) {
		for (File child : directory.listFiles()) {
			if (child.isDirectory()) {
				deleteDirectoryContent(child, ignoredFileNames);
			}
			if (ignoredFileNames.contains(child.getName())) {
				continue;
			}
			if (!child.delete()) {
				throw new IllegalStateException("cannot delete " + child);
			}
		}
	}

	/**
	 * Copies a file to a directory preserving the file date.
	 * <p>
	 * This method copies the contents of the specified source file to a file of
	 * the same name in the specified destination directory. The destination
	 * directory is created if it does not exist. If the destination file
	 * exists, then this method will overwrite it. The last modified time of the
	 * resulting file will be set to that of the source file.
	 *
	 * @param srcFile
	 *            an existing file to copy
	 * @param destDir
	 *            the directory to place the copy in
	 * @throws IOException
	 *             if an IO error occurs during copying
	 */
	public static void copyFileToDirectory(File srcFile, File destDir) throws IOException {
		if (!destDir.isDirectory()) {
			if (!destDir.mkdirs()) {
				throw new IOException("cannot create directory " + destDir);
			}
		}
		File destFile = new File(destDir, srcFile.getName());
		copyFile(srcFile, destFile);
	}

	/**
	 * Convenience method to copy a file from a source to a destination. If the
	 * file already exists, it will be overwritten. The last modified time of
	 * the resulting file will be set to that of the source file.
	 *
	 * @param srcFile
	 *            the file to copy from. Must not be <code>null</code>.
	 * @param destFile
	 *            the file to copy to. Must not be <code>null</code>.
	 * @throws IOException
	 *             if the copying fails.
	 */
	public static void copyFile(File srcFile, File destFile) throws IOException {
		if (!srcFile.isFile()) {
			throw new IOException("not a file: " + srcFile);
		}

		try (FileInputStream fis = new FileInputStream(srcFile);
				FileOutputStream out = new FileOutputStream(destFile)) {
			StreamHelper.copyStream(fis, out);
		}

		long time = srcFile.lastModified();
		if (!destFile.setLastModified(time)) {
			throw new IOException("cannot set last modified time: " + destFile);
		}
	}

	/**
	 * Tries to create a hardlink at destFile, falls back to copying in case
	 * something happens.
	 *
	 * @param srcFile
	 * @param destFile
	 * @throws IOException
	 */
	public static void hardLinkOrCopy(File srcFile, File destFile) throws IOException {
		try {
			Files.createLink(destFile.toPath(), srcFile.toPath());
		} catch (Exception e) {
			copyFile(srcFile, destFile);
		}
	}

	/**
	 * Opens a stream to a remote file and downloads it to the given target.
	 *
	 * @param uri
	 *            the URI of the file
	 * @param target
	 *            the target to place the file to.
	 */
	public static void download(String uri, File target) throws Exception {
		URL url = new URI(uri).toURL();
		try (InputStream is = url.openStream(); FileOutputStream out = new FileOutputStream(target)) {
			ReadableByteChannel rbc = Channels.newChannel(is);
			out.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		}
	}

	/**
	 * Copies files from a source to a destination directory. All existing files
	 * in the target directory will be replaced. The last modified time of the
	 * resulting file will be set to that of the source file.
	 *
	 * @param srcDir
	 *            the directory to copy from. Must not be <code>null</code>.
	 * @param destDir
	 *            the directory to copy to. Must not be <code>null</code>.
	 * @throws IOException
	 *             if the copying fails.
	 */
	public static void copyDirectory(File srcDir, File destDir) throws IOException {
		copyDirectory(srcDir, destDir, Collections.emptyList());
	}

	/**
	 * Copies files from a source to a destination directory. All existing files
	 * in the target directory will be replaced. The last modified time of the
	 * resulting file will be set to that of the source file.
	 *
	 * @param srcDir
	 *            the directory to copy from. Must not be <code>null</code>.
	 * @param destDir
	 *            the directory to copy to. Must not be <code>null</code>.
	 * @throws IOException
	 *             if the copying fails.
	 */
	public static void copyDirectory(File srcDir, File destDir, List<String> exclude) throws IOException {
		if (!srcDir.isDirectory()) {
			throw new IOException("not a directory: " + srcDir);
		}
		FileUtils.mkdirs(destDir);
		for (File file : srcDir.listFiles()) {
			if (exclude.contains(file.getName())) {
				continue;
			}
			if (file.isFile()) {
				copyFileToDirectory(file, destDir);
			} else {
				copyDirectory(file, new File(destDir, file.getName()), exclude);
			}
		}
	}

	/**
	 * Deletes a file or directory; throws an exception if it failed.
	 */
	public static void delete(File file) {
		if (file.exists()) {
			if (!file.delete()) {
				throw new IllegalStateException("cannot delete " + file);
			}
		}
	}

	/**
	 * Creates a directory, including any necessary but nonexistent parent
	 * directories, in a thread-safe manner. Note that if this operation fails
	 * it may have succeeded in creating some of the necessary parent
	 * directories. Throws an exception if the creation failed.
	 */
	public static void mkdirs(File dir) {
		// retry in case some other thread simultaneously creates the same
		// directory too
		long tried = 0;
		while (++tried <= 20 && !dir.exists()) {
			if (dir.mkdirs()) {
				return;
			}
			try {
				Thread.sleep(tried);
			} catch (InterruptedException e) {
				// nothing to do
			}
		}
		if (dir.exists()) {
			return;
		}
		throw new IllegalStateException("cannot create " + dir);
	}

	/**
	 * Writes a XML file without any special formatting.
	 *
	 * @param document
	 *            DOM document
	 * @param file
	 *            target file (UTF-8)
	 */
	public static void writeXml(Document document, File file) throws Exception {
		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(file);
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.setOutputProperty(OutputKeys.INDENT, "yes");
		xformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		xformer.transform(source, result);
	}

	/**
	 * Reads the given file and returns the resulting document.
	 *
	 * @param file
	 *            the XML file to read
	 * @return DOM document or {@code null} if the file is not existing
	 */
	public static Document readXml(File file) throws Exception {
		if (!file.exists() || !file.canRead()) {
			return null;
		}
		FileInputStream is = null;
		try {
			is = new FileInputStream(file);
			return readXml(is);
		} finally {
			StreamHelper.closeQuietly(is);
		}
	}

	/**
	 * Reads the given stream and returns the resulting document.
	 *
	 * @param stream
	 *            the input stream to read
	 * @return DOM document
	 */
	public static Document readXml(InputStream stream) throws Exception {
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		return builder.parse(stream);
	}

	/**
	 * Reads a given properties file and returns the result.
	 *
	 * @param file
	 *            the file to read
	 * @return the properties
	 */
	public static Properties readProperties(File file) {
		try {
			Properties p = new Properties();
			try (FileInputStream fis = new FileInputStream(file)) {
				p.load(fis);
			}
			return p;
		} catch (IOException e) {
			throw new IllegalStateException("cannot read " + file, e);
		}
	}

	/**
	 * Writes properties to a file.
	 *
	 * @param props
	 *            the properties
	 * @param file
	 *            the file to write
	 */
	public static void writeProperties(Properties props, File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		try {
			props.store(fos, null);
		} finally {
			StreamHelper.closeQuietly(fos);
		}
	}

	/**
	 * Moves a {@link File} from one location to another. Tries to rename the
	 * file (which fails in case the rename crosses partitions), and otherwise
	 * falls back to a copy/delete operation. If the target file already exists
	 * it will be deleted before moving.
	 *
	 * @param source
	 *            the source filename
	 * @param target
	 *            the target filename
	 * @throws IOException
	 *             in case an error happens.
	 */
	public static void moveFile(File source, File target) throws IOException {
		delete(target);

		if (!source.renameTo(target)) {
			copyFile(source, target);
			delete(source);
		}
	}

	/**
	 * Same as {@link #moveFile(File, File)} but for directories.
	 */
	public static void moveDirectory(File source, File target) throws IOException {
		deleteDirectory(target);

		if (!source.renameTo(target)) {
			copyDirectory(source, target);
			deleteDirectory(source);
		}
	}

	public static void touchFile(File f) throws IOException {
		if (f.exists()) {
			if (!f.setLastModified(System.currentTimeMillis())) {
				throw new IOException("setting last-modified time failed");
			}
		} else {
			if (!f.createNewFile()) {
				throw new IOException("cannot create " + f);
			}
		}
	}

	/**
	 * Reads a given {@link File} with the given {@link Charset} fully into a
	 * {@link String}
	 *
	 * @param file
	 *            the {@link File} to read from
	 * @param cs
	 *            the {@link Charset} used for conversion
	 * @return the {@link String} representing the complete contents of the
	 *         {@link File}
	 * @throws IOException
	 *             in case of an error.
	 */
	public static String readStringFromFile(File file, Charset cs) throws IOException {
		if (!file.isFile()) {
			throw new IOException("not a file: " + file);
		}
		if (file.length() > Integer.MAX_VALUE) {
			throw new IOException("file is too big");
		}
		int len1 = (int) file.length();
		char[] cbuf = new char[len1];

		try (InputStreamReader r = new InputStreamReader(new FileInputStream(file), cs)) {
			int len2 = r.read(cbuf, 0, len1);
			return new String(cbuf, 0, len2);
		}
	}

	/**
	 * Writes a given {@link String} to a given {@link File} in the given
	 * {@link Charset}, replacing any existing {@link File} at that location.
	 *
	 * @param file
	 *            the {@link File} to write
	 * @param cs
	 *            the {@link Charset} used for conversion
	 * @param contents
	 *            the target contents of the {@link File}
	 * @throws IOException
	 *             in case of an error.
	 */
	public static void writeFileFromString(File file, Charset cs, String contents) throws IOException {
		try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(file), cs)) {
			w.write(contents);
		}
	}

	/**
	 * Reads all non-empty lines of a file. Line comments start with a hash
	 * mark.
	 */
	public static List<String> readListFile(File file) throws IOException {
		if (!file.isFile()) {
			throw new IOException("not a file: " + file);
		}
		Reader reader = new FileReader(file);
		try {
			return readListFile(reader);
		} finally {
			reader.close();
		}
	}

	/**
	 * Reads all non-empty lines from a {@link Reader}. Line comments start with
	 * a hash mark. Doesn't close the reader.
	 */
	public static List<String> readListFile(Reader reader) throws IOException {
		List<String> result = new ArrayList<>();
		LineNumberReader r = new LineNumberReader(reader);
		String line = r.readLine();
		while (line != null) {
			String element = line.trim();
			if (!element.isEmpty() && !element.startsWith("#")) {
				result.add(element);
			}
			line = r.readLine();
		}
		return result;
	}

	/**
	 * A {@link File} if f is not <code>null</code> and exists.
	 */
	public static File safeFile(String f) {
		if (f == null) {
			return null;
		}

		File r = new File(f);
		if (!r.exists()) {
			return null;
		}

		return r;
	}

	public static boolean equals(File f1, File f2) {
		try {
			byte[] b1 = java.nio.file.Files.readAllBytes(f1.toPath());
			byte[] b2 = java.nio.file.Files.readAllBytes(f2.toPath());
			return Arrays.equals(b1, b2);
		} catch (IOException e) {
			return false;
		}
	}

}
