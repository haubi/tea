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
package org.eclipse.tea.library.build.p2;

import java.io.File;

import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.tea.library.build.model.FeatureBuild;

/**
 * Represents a product file.
 * <p>
 * Note:The version will be taken from the feature instead and not from the
 * product file.
 * </p>
 */
@SuppressWarnings("restriction")
public class TeaProductDescription extends ProductFile {

	private final FeatureBuild feature;

	/**
	 * Creates a new product file parser
	 *
	 * @param productFile
	 *            the product file to parse
	 * @param feature
	 *            the product feature providing the version
	 */
	public TeaProductDescription(File productFile, FeatureBuild feature) throws Exception {
		super(productFile.getAbsolutePath());
		this.feature = feature;
	}

	@Override
	public String getVersion() {
		try {
			return feature.getData().getBundleVersion();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
