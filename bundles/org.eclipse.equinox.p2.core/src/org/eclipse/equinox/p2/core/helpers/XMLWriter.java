/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.core.helpers;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import org.osgi.framework.Version;

public class XMLWriter implements XMLConstants {

	// Miscellaneous
	public static final String EMPTY_ATTR = ""; //$NON-NLS-1$

	public static class ProcessingInstruction {

		private String target;
		private String[] data;

		// The standard UTF-8 processing instruction
		public static final String XML_UTF8 = "<?xml version='1.0' encoding='UTF-8'?>"; //$NON-NLS-1$

		public ProcessingInstruction(String target, String data) {
			this.target = target;
			this.data = new String[] {data};
		}

		public ProcessingInstruction(String target) {
			this.target = target;
			this.data = new String[0];
		}

		public ProcessingInstruction(String target, String[] data) {
			this.target = target;
			this.data = data;
		}

		public ProcessingInstruction(String target, String[] attrs, String[] values) {
			// Lengths of attributes and values must be the same
			this.target = target;
			this.data = new String[attrs.length];
			for (int i = 0; i < attrs.length; i++) {
				data[i] = attributeImage(attrs[i], values[i]);
			}
		}

		public static ProcessingInstruction makeClassVersionInstruction(String target, Class clazz, Version version) {
			return new ProcessingInstruction(target, new String[] {PI_CLASS_ATTRIBUTE, PI_VERSION_ATTRIBUTE}, new String[] {clazz.getName(), version.toString()});
		}

		public String toString() {
			StringBuffer sb = new StringBuffer("<?"); //$NON-NLS-1$
			sb.append(this.target).append(' ');
			for (int i = 0; i < data.length; i++) {
				sb.append(this.data[i]);
				if (i < data.length - 1) {
					sb.append(' ');
				}
			}
			sb.append("?>"); //$NON-NLS-1$
			return sb.toString();
		}
	}

	private Stack elements; // XML elements that have not yet been closed
	private boolean open; // Can attributes be added to the current element?
	private String indent; // used for each level of indentation

	private PrintWriter pw;

	private XMLWriter(OutputStream output, boolean writeXMLProcessingInstruction) throws UnsupportedEncodingException {
		this.pw = new PrintWriter(new OutputStreamWriter(output, "UTF8"), true); //$NON-NLS-1$
		if (writeXMLProcessingInstruction) {
			println(ProcessingInstruction.XML_UTF8);
		}
		this.elements = new Stack();
		this.open = false;
		this.indent = "  "; //$NON-NLS-1$
	}

	public XMLWriter(OutputStream output, ProcessingInstruction piElement, boolean writeXMLProcessingInstruction) throws UnsupportedEncodingException {
		this(output, new ProcessingInstruction[] {piElement}, writeXMLProcessingInstruction);
	}

	public XMLWriter(OutputStream output, ProcessingInstruction piElement) throws UnsupportedEncodingException {
		this(output, new ProcessingInstruction[] {piElement}, true /* writeXMLProcessingInstruction */);
	}

	public XMLWriter(OutputStream output, ProcessingInstruction[] piElements, boolean writeXMLProcessingInstruction) throws UnsupportedEncodingException {
		this(output, writeXMLProcessingInstruction);
		if (piElements != null) {
			for (int i = 0; i < piElements.length; i++) {
				println(piElements[i].toString());
			}
		}
	}

	public XMLWriter(OutputStream output, ProcessingInstruction[] piElements) throws UnsupportedEncodingException {
		this(output, piElements, /* writeXMLProcessingInstruction */
		true);
	}

	// String used for each level of indentation; default is two spaces.
	public void setIndent(String indent) {
		this.indent = indent;
	}

	// start a new element
	public void start(String name) {
		if (this.open) {
			println('>');
		}
		indent();
		print('<');
		print(name);
		this.elements.push(name);
		this.open = true;
	}

	// end the most recent element with this name
	public void end(String name) {
		if (this.elements.empty()) {
			throw new EndWithoutStartError();
		}
		int index = this.elements.search(name);
		if (index == -1) {
			throw new EndWithoutStartError(name);
		}
		for (int i = 0; i < index; i += 1) {
			end();
		}
	}

	// end the current element
	public void end() {
		if (this.elements.empty()) {
			throw new EndWithoutStartError();
		}
		String name = (String) this.elements.pop();
		if (this.open) {
			println("/>"); //$NON-NLS-1$
		} else {
			printlnIndented("</" + name + '>', false); //$NON-NLS-1$
		}
		this.open = false;
	}

	// write a boolean attribute if it doesn't have the default value
	public void attribute(String name, boolean value, boolean defaultValue) {
		if (value != defaultValue) {
			attribute(name, value);
		}
	}

	public void attribute(String name, boolean value) {
		attribute(name, Boolean.toString(value));
	}

	public void attribute(String name, int value) {
		attribute(name, Integer.toString(value));
	}

	public void attributeOptional(String name, String value) {
		if (value.length() > 0) {
			attribute(name, value);
		}
	}

	public void attribute(String name, Object value) {
		if (!this.open) {
			throw new AttributeAfterNestedContentError();
		}
		if (value == null) {
			return; // optional attribute with no value
		}
		print(' ');
		print(name);
		print("='"); //$NON-NLS-1$
		print(XMLUtils.escape(value.toString()));
		print('\'');
	}

	public void cdata(String data) {
		cdata(data, true);
	}

	public void cdata(String data, boolean escape) {
		if (this.open) {
			println('>');
			this.open = false;
		}
		if (data != null) {
			printlnIndented(data, escape);
		}
	}

	public void cdataLines(String data, boolean escape) {
		if (this.open) {
			println('>');
			this.open = false;
		}
		if (data.indexOf('\n') == -1) {
			// simple case: one line
			printlnIndented(data, escape);
		} else {
			String[] lines = Pattern.compile("\\s*\\n").split(data, 0); //$NON-NLS-1$
			for (int i = 0; i < lines.length; i += 1) {
				printlnIndented(lines[i].trim(), escape);
			}
		}
	}

	public void flush() {
		while (!this.elements.empty()) {
			try {
				end();
			} catch (EndWithoutStartError e) {
				// can't happen
			}
		}
		this.pw.flush();
	}

	public void close() {
		flush();
		this.pw.close();
	}

	public void writeProperties(Map properties) {
		writeProperties(PROPERTIES_ELEMENT, properties);
	}

	public void writeProperties(String propertiesElement, Map properties) {
		if (properties != null && properties.size() > 0) {
			start(propertiesElement);
			attribute(COLLECTION_SIZE_ATTRIBUTE, properties.size());
			for (Iterator iter = properties.keySet().iterator(); iter.hasNext();) {
				String name = (String) iter.next();
				writeProperty(name, (String) properties.get(name));
			}
			end(propertiesElement);
		}
	}

	// Support writing properties with an auxiliary map
	// of keys to localized values
	public void writeProperties(Map properties, Map localizedValues) {
		writeProperties(PROPERTIES_ELEMENT, properties, localizedValues);
	}

	// Support writing properties with an auxiliary map
	// of keys to localized values
	public void writeProperties(String propertiesElement, Map properties, Map localizedValues) {
		if (properties != null && properties.size() > 0 && localizedValues != null) {
			start(propertiesElement);
			for (Iterator I = properties.entrySet().iterator(); I.hasNext();) {
				Map.Entry property = (Map.Entry) I.next();
				String key = (String) property.getKey();
				String val = (String) property.getValue();
				if (localizedValues.containsKey(key)) {
					val = (String) localizedValues.get(key);
				}
				writeProperty(key, val);
			}
			end(propertiesElement);
		}
	}

	public void writeProperty(String name, String value) {
		start(PROPERTY_ELEMENT);
		attribute(PROPERTY_NAME_ATTRIBUTE, name);
		attribute(PROPERTY_VALUE_ATTRIBUTE, value);
		end();
	}

	protected static String attributeImage(String name, String value) {
		if (value == null) {
			return EMPTY_ATTR; // optional attribute with no value
		}
		return name + "='" + XMLUtils.escape(value) + '\''; //$NON-NLS-1$
	}

	private void println(char c) {
		this.pw.println(c);
	}

	private void println(String s) {
		this.pw.println(s);
	}

	private void println() {
		this.pw.println();
	}

	private void print(char c) {
		this.pw.print(c);
	}

	private void print(String s) {
		this.pw.print(s);
	}

	private void printlnIndented(String s, boolean escape) {
		if (s.length() == 0) {
			println();
		} else {
			indent();
			println(escape ? XMLUtils.escape(s) : s);
		}
	}

	private void indent() {
		for (int i = this.elements.size(); i > 0; i -= 1) {
			print(this.indent);
		}
	}

	public static class AttributeAfterNestedContentError extends Error {
		private static final long serialVersionUID = 1L; // not serialized
	}

	public static class EndWithoutStartError extends Error {
		private static final long serialVersionUID = 1L; // not serialized
		private String name;

		public EndWithoutStartError() {
			super();
		}

		public EndWithoutStartError(String name) {
			super();
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}

}
