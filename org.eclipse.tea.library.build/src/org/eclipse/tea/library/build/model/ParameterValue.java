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
package org.eclipse.tea.library.build.model;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a string of the form
 *
 * <pre>
 * com.wamas.ide.core;bundle-version="5.1.0"
 * </pre>
 */
public final class ParameterValue {

	private static final class Param {

		private final String value, infix;

		private Param(String value, String infix) {
			this.value = value;
			this.infix = infix;
		}

		private static final Param EMPTY = new Param("", "");
	}

	private String value;
	private final Map<String, Param> parameters;

	ParameterValue(String fullString) {
		String[] elements = fullString.split(";");
		value = elements[0].trim();
		if (elements.length == 1) {
			parameters = Collections.emptyMap();
			return;
		}
		parameters = new TreeMap<>();
		for (int i = 1; i < elements.length; ++i) {
			String element = elements[i];
			String infix = ":=";
			int index = element.indexOf(infix);
			int len = 2;
			if (index < 0) {
				infix = "=";
				index = element.indexOf(infix);
				len = 1;
			}
			if (index < 0) {
				parameters.put(element.trim(), Param.EMPTY);
			} else {
				String key = element.substring(0, index).trim();
				String value = element.substring(index + len).trim();
				parameters.put(key, new Param(value, infix));
			}
		}
	}

	public static ParameterValue[] fromList(String[] list) {
		ParameterValue[] result = new ParameterValue[list.length];
		for (int i = 0; i < list.length; ++i) {
			result[i] = new ParameterValue(list[i]);
		}
		return result;
	}

	public static String[] valuesFromList(ParameterValue[] list) {
		String[] result = new String[list.length];
		for (int i = 0; i < list.length; ++i) {
			result[i] = list[i].value;
		}
		return result;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(\"" + value + "\", " + parameters + ')';
	}

	/**
	 * Returns the value of a string parameter.
	 * <p>
	 * Example:<br>
	 * {@code com.wamas.ide.core;bundle-version="5.1.0"}<br>
	 * The call {@code getStringParameter("bundle-version")} would return
	 * {@code 5.1.0}
	 */
	public String getStringParameter(String key) {
		Param param = parameters.get(key);
		if (param == null) {
			return null;
		}
		final String paramValue = param.value;
		if (paramValue.isEmpty()) {
			return paramValue;
		}
		int i1 = paramValue.indexOf('"');
		int i2 = paramValue.lastIndexOf('"');
		if (i1 != 0 || i2 != paramValue.length() - 1) {
			return paramValue;
		}
		++i1;
		if (i1 >= i2) {
			return paramValue;
		}
		return paramValue.substring(i1, i2);
	}

	/**
	 * Reverse operation to {@link #getStringParameter(String)}.
	 */
	public void setStringParameter(String key, String value, String infix) {
		if (value == null) {
			parameters.remove(key);
		} else {
			parameters.put(key, new Param("\"" + value + '"', infix));
		}
	}

	public String getEnumParameter(String key) {
		Param param = parameters.get(key);
		if (param == null) {
			return null;
		}
		return param.value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void write(Writer ps) throws IOException {
		ps.write(value);
		for (Map.Entry<String, Param> entry : parameters.entrySet()) {
			ps.write(';');
			ps.write(entry.getKey());
			Param param = entry.getValue();
			ps.write(param.infix);
			ps.write(param.value);
		}
	}

	void writeHtmlListelement(Writer w) throws IOException {
		w.write("<li>");
		w.write(value);
		if (!parameters.isEmpty()) {
			w.write(" <tt>");
			w.write(parameters.toString());
			w.write("</tt>");
		}
		w.write("</li>\n");
	}

}
