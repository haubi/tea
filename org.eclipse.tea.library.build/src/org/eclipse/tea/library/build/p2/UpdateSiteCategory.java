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
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.model.FeatureBuild;
import org.eclipse.tea.library.build.model.WorkspaceBuild;
import org.eclipse.tea.library.build.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Helper methods to generate update site categorizations.
 */
public class UpdateSiteCategory {

	/**
	 * Generates the definition for a category into the given category.xml
	 * {@link Document}
	 *
	 * @param categoryXml
	 *            the {@link Document} to append nodes to
	 * @param name
	 *            the id of the category
	 * @param label
	 *            the human readable label for the category
	 */
	public static void generateCategory(Document categoryXml, String name, String label) {
		Element root = categoryXml.getDocumentElement();

		Element cat = categoryXml.createElement("category-def");
		cat.setAttribute("name", name);
		cat.setAttribute("label", label);
		root.appendChild(cat);
	}

	/**
	 * Generates the definition of a feature categorization in the given
	 * category.xml {@link Document}
	 *
	 * @param categoryXml
	 *            the {@link Document} to append nodes to
	 * @param featureRelUrl
	 *            the site-relative path to the feature jar
	 *            ("features/feature_version.jar")
	 * @param featureId
	 *            the feature id.
	 * @param featureVersion
	 *            the feature version.
	 * @param categoryName
	 *            the category id
	 */
	public static void generateCategorization(Document categoryXml, String featureRelUrl, String featureId,
			String featureVersion, String categoryName) {
		Element root = categoryXml.getDocumentElement();

		Element feat = categoryXml.createElement("feature");
		feat.setAttribute("url", featureRelUrl);
		feat.setAttribute("id", featureId);
		feat.setAttribute("version", featureVersion);
		root.appendChild(feat);

		Element cat = categoryXml.createElement("category");
		cat.setAttribute("name", categoryName);
		feat.appendChild(cat);
	}

	/**
	 * Generates a new {@link Document} suitable for categorization of features
	 * in an update site.
	 *
	 * @throws ParserConfigurationException
	 *             if the document could not be created because of a wrong
	 *             configuration
	 */
	public static Document generateCategoryHeader() throws ParserConfigurationException {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		Document doc = docBuilder.newDocument();

		Element root = doc.createElement("site");
		doc.appendChild(root);

		return doc;
	}

	/**
	 * Generates the category.xml for a feature. This a file is required by the
	 * CategoryPublisher.
	 */
	public static void generateCategoryXml(File categoryFile, Map<String, String> featureIdToCategory,
			WorkspaceBuild wb, JarManager jm) throws Exception {
		final Document doc = UpdateSiteCategory.generateCategoryHeader();

		featureIdToCategory.values().stream().distinct().forEach(n -> UpdateSiteCategory.generateCategory(doc, n, n));

		for (Map.Entry<String, String> entry : featureIdToCategory.entrySet()) {
			FeatureBuild feature = wb.getFeature(entry.getKey());

			String version = feature.getData().getBundleVersion();
			String name = feature.getFeatureName();
			UpdateSiteCategory.generateCategorization(doc, "features/" + name + "_" + version + ".jar", name, version,
					entry.getValue());
		}

		Element root = doc.getDocumentElement();
		if (root.getChildNodes().getLength() <= 0) {
			throw new Exception("Malformed " + categoryFile);
		}

		FileUtils.delete(categoryFile);
		FileUtils.writeXml(doc, categoryFile);
	}
}
