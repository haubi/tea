/*******************************************************************************
 *  Copyright (c) 2021 SSI Schaefer IT Solutions GmbH and others.
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
 * A method to intercept
 *
 * @see ZipExec#addPart(ZipExecPart)
 **/
public interface ZipExecInterceptor {

	List<ZipExecPart> convert(ZipExecPart part);

	/** default Interceptor **/
	static ZipExecInterceptor neutralInterceptor() {
		return new ZipExecInterceptor() {
			@Override
			public List<ZipExecPart> convert(ZipExecPart part) {
				return Collections.singletonList(part);
			}
		};
	}

	/** replace a file with another **/
	static ZipExecInterceptor replaceFileInterceptor(String relativePathToReplace, File replacementSourceDirectory,
			String replacementRelativePath) {
		return new ZipExecInterceptor() {
			@Override
			public List<ZipExecPart> convert(ZipExecPart part) {
				List<ZipExecPart> parts = new ArrayList<>();
				ZipExecPart converted = new ZipExecPart(part);
				converted.relativePaths.remove(relativePathToReplace);
				parts.add(converted);

				ZipExecPart additionalPart = new ZipExecPart(part);
				additionalPart.relativePaths.clear();
				additionalPart.sourceDirectory = replacementSourceDirectory;
				additionalPart.relativePaths.add(replacementRelativePath);
				parts.add(additionalPart);
				return parts;
			}
		};
	}

}
