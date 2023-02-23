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
package org.eclipse.tea.library.build.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IProject;
import org.eclipse.tea.library.build.services.TeaBuildVersionService;
import org.eclipse.tea.library.build.util.FileUtils;
import org.eclipse.tea.library.build.util.StreamHelper;
import org.eclipse.tea.library.build.util.StringHelper;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Stores useful information about a RCP feature.
 */
public class FeatureData extends BundleData {

	/**
	 * content of the feature XML file
	 */
	private Document document;
	/**
	 * A version override that is valid as long as there is no document set.
	 * This is to be able to set the version of a feature that is to be
	 * generated in the future but does not yet exist.
	 */
	private String version;

	private final IProject project;
	private final TeaBuildVersionService bvService;

	/**
	 * Creates the feature data for a source distribution.
	 *
	 * @param project
	 *            Eclipse project
	 * @param bvService
	 */
	public FeatureData(IProject project, TeaBuildVersionService bvService) {
		super(project.getName(), project.getLocation().toFile(), true, null);
		this.project = project;
		this.bvService = bvService;
		this.document = readFeatureXml();
	}

	@Override
	public IProject getProject() {
		return project;
	}

	private static String trim(String text) {
		if (text == null || text.isEmpty()) {
			return null;
		}
		text = text.trim();
		if (text.isEmpty()) {
			return null;
		}
		return text;
	}

	@Override
	public boolean isMetadataOK() {
		return document != null;
	}

	@Override
	public final String getBundleVersion() {
		if (document != null) {
			final Element rootElement = document.getDocumentElement();
			return trim(rootElement.getAttribute("version"));
		} else if (version != null) {
			return version;
		}
		// can happen when building FeatureData from a workspace project without
		// manifest.
		return bvService.getBuildVersion();
	}

	@Override
	public String getBundleName() {
		if (document != null) {
			final Element rootElement = document.getDocumentElement();
			return trim(rootElement.getAttribute("id"));
		}
		// can happen when building FeatureData from a workspace project without
		// manifest.
		return super.getBundleName();
	}

	public String getBundleLabel() {
		if (document != null) {
			final Element rootElement = document.getDocumentElement();
			String lbl = trim(rootElement.getAttribute("label"));
			if (!StringHelper.isNullOrEmpty(lbl)) {
				return lbl;
			}
		}
		return getBundleName();
	}

	@Override
	public final void setBundleVersion(String value) {
		if (document != null) {
			final Element rootElement = document.getDocumentElement();
			rootElement.setAttribute("version", value);
		} else {
			version = value;
		}
	}

	public final String getBundleVendor() {
		if (document != null) {
			final Element rootElement = document.getDocumentElement();
			return trim(rootElement.getAttribute("provider-name"));
		}
		// we're just creating it, no feature.xml there yet. so it's ours. i'm
		// sure. period.
		return bvService.getDefaultVendor();
	}

	public final void setBundleVendor(String value) {
		final Element rootElement = document.getDocumentElement();
		rootElement.setAttribute("provider-name", value);
	}

	/**
	 * Returns the list of plugin IDs, which defines the content of this
	 * feature.
	 */
	public final List<PluginInfo> getPluginInfos() {
		List<PluginInfo> pluginInfos = new ArrayList<>();
		final Element rootElement = document.getDocumentElement();
		final NodeList rootNodes = rootElement.getChildNodes();
		for (int i = 0; i < rootNodes.getLength(); i++) {
			Node node = rootNodes.item(i);
			if (node instanceof Element) {
				Element child = (Element) node;
				if ("plugin".equals(child.getTagName())) {
					PluginInfo info = new PluginInfo();
					info.id = child.getAttribute("id");
					info.version = child.getAttribute("version");

					pluginInfos.add(info);
				}
			}
		}
		return pluginInfos;
	}

	/**
	 * reloads feature XML file
	 */
	final void reloadFeatureXml() {
		document = readFeatureXml();
	}

	File getFeatureXmlFile() {
		return new File(bundleDir, "feature.xml");
	}

	/**
	 * Reads the feature XML file.
	 */
	Document readFeatureXml() {
		try {
			return FileUtils.readXml(getFeatureXmlFile());
		} catch (Exception ex) {
			throw new IllegalStateException("cannot read " + jarFile, ex);
		}
	}

	/**
	 * Re-writes the manifest file; only possible for source distributions.
	 *
	 * @return {@code true} if we could write the manifest file
	 */
	public final boolean writeFeatureXml() {
		if (document == null) {
			return false;
		}
		final File xmlFile = new File(bundleDir, "feature.xml");
		try {
			writeXml(document, xmlFile);
			return true;
		} catch (Exception ex) {
			throw new IllegalStateException("cannot write " + xmlFile, ex);
		}
	}

	/**
	 * Writes a DOM into a XML file; the formatting looks like an Eclipse
	 * feature XML.
	 *
	 * @param document
	 *            DOM
	 * @param xmlFile
	 *            output file
	 * @throws IOException
	 *             if an IO error occurred writing the xmlFile
	 */
	public static void writeXml(Document document, File xmlFile) throws IOException {
		Writer w = null;
		try {
			w = new OutputStreamWriter(new FileOutputStream(xmlFile), "UTF-8");
			w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			Element rootElement = document.getDocumentElement();
			writeXml(rootElement, w, "");
		} finally {
			StreamHelper.closeQuietly(w);
		}
	}

	private static void writeXml(Element elem, Writer w, final String rootIndent) throws IOException {
		final String tagName = elem.getTagName();
		w.write(rootIndent);
		w.write('<');
		w.write(tagName);

		final String childIndent = rootIndent + "   ";
		final String attrIndent = childIndent + "   ";

		final boolean isMain = tagName.equals("feature") || tagName.equals("plugin") || tagName.equals("includes");

		NamedNodeMap attrs = elem.getAttributes();
		int len = attrs.getLength();
		Map<String, String> sortedAttributes = new TreeMap<>(FeatureAttributeComparator.fromTag(tagName));
		for (int i = 0; i < len; ++i) {
			Attr attr = (Attr) attrs.item(i);
			sortedAttributes.put(attr.getName(), attr.getValue());
		}
		for (Map.Entry<String, String> entry : sortedAttributes.entrySet()) {
			if (isMain) {
				w.write('\n');
				w.write(attrIndent);
			} else {
				w.write(' ');
			}
			w.write(entry.getKey());
			w.write("=\"");
			w.write(entry.getValue());
			w.write('"');
		}

		NodeList children = elem.getChildNodes();
		len = children.getLength();
		if (len == 0) {
			w.write("/>\n");
		} else {
			w.write(">\n");
			for (int i = 0; i < len; ++i) {
				Node child = children.item(i);
				if (child instanceof Element) {
					if (isMain) {
						w.write('\n');
					}
					writeXml((Element) child, w, childIndent);
					continue;
				}
				if (child instanceof Text) {
					String text = ((Text) child).getWholeText().trim();
					if (!text.isEmpty()) {
						w.write(childIndent);
						w.write(text);
						w.write('\n');
					}
					continue;
				}
				w.write("<!-- " + child.getClass().getSimpleName() + " -->\n");
			}

			if (rootIndent.isEmpty()) {
				w.write('\n');
			} else {
				w.write(rootIndent);
			}
			w.write("</");
			w.write(tagName);
			w.write(">\n");
		}
	}

	public Document generateFeatureHeader() throws ParserConfigurationException {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		Document doc = docBuilder.newDocument();

		// create the root element and add it to the document
		Element root = doc.createElement("feature");
		root.setAttribute("id", getBundleName());
		root.setAttribute("label", getBundleLabel());
		root.setAttribute("version", getBundleVersion());
		root.setAttribute("provider-name", getBundleVendor());
		doc.appendChild(root);

		return doc;
	}

}
