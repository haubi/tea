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
package org.eclipse.tea.library.build.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides static helpers to work with streams
 */
public class StreamHelper {

	/**
	 * Unconditionally close a <code>Closeable</code> without throwing any
	 * exception.
	 *
	 * @param closeable
	 *            the object to close, may be null or already closed
	 */
	public static void closeQuietly(Closeable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (Exception e) {
			// ignore
		}
	}
	
	/**
	 * Copies an input stream to an output stream fully.
	 * 
	 * @throws IOException
	 */
	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[102400]; // 100K buffer
		int size;
		while ((size = in.read(buffer)) >= 0) {
			out.write(buffer, 0, size);
		}
	}
	

}
