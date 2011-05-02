/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.persistence;

import org.eclipse.equinox.p2.metadata.Version;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class XMLWriter implements XMLConstants {

	public static class ProcessingInstruction {

		private String target;
		private String[] data;

		// The standard UTF-8 processing instruction
		public static final String XML_UTF8 = "<?xml version='1.0' encoding='UTF-8'?>"; //$NON-NLS-1$

		public ProcessingInstruction(String target, String[] attrs, String[] values) {
			// Lengths of attributes and values must be the same
			this.target = target;
			this.data = new String[attrs.length];
			for (int i = 0; i < attrs.length; i++) {
				data[i] = attributeImage(attrs[i], values[i]);
			}
		}

		public static ProcessingInstruction makeTargetVersionInstruction(String target, Version version) {
			return new ProcessingInstruction(target, new String[] {PI_VERSION_ATTRIBUTE}, new String[] {version.toString()});
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

	private Stack<String> elements; // XML elements that have not yet been closed
	private boolean open; // Can attributes be added to the current element?
	private String indent; // used for each level of indentation

	private PrintWriter pw;

	public XMLWriter(OutputStream output, ProcessingInstruction[] piElements) throws UnsupportedEncodingException {
		this.pw = new PrintWriter(new OutputStreamWriter(output, "UTF8"), false); //$NON-NLS-1$
		println(ProcessingInstruction.XML_UTF8);
		this.elements = new Stack<String>();
		this.open = false;
		this.indent = "  "; //$NON-NLS-1$
		if (piElements != null) {
			for (int i = 0; i < piElements.length; i++) {
				println(piElements[i].toString());
			}
		}
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
		String name = this.elements.pop();
		if (this.open) {
			println("/>"); //$NON-NLS-1$
		} else {
			printlnIndented("</" + name + '>', false); //$NON-NLS-1$
		}
		this.open = false;
	}

	public static String escape(String txt) {
		StringBuffer buffer = null;
		for (int i = 0; i < txt.length(); ++i) {
			String replace;
			char c = txt.charAt(i);
			switch (c) {
				case '<' :
					replace = "&lt;"; //$NON-NLS-1$
					break;
				case '>' :
					replace = "&gt;"; //$NON-NLS-1$
					break;
				case '"' :
					replace = "&quot;"; //$NON-NLS-1$
					break;
				case '\'' :
					replace = "&apos;"; //$NON-NLS-1$
					break;
				case '&' :
					replace = "&amp;"; //$NON-NLS-1$
					break;
				case '\t' :
					replace = "&#x9;"; //$NON-NLS-1$
					break;
				case '\n' :
					replace = "&#xA;"; //$NON-NLS-1$
					break;
				case '\r' :
					replace = "&#xD;"; //$NON-NLS-1$
					break;
				default :
					// this is the set of legal xml scharacters in unicode excluding high surrogates since they cannot be represented with a char
					// see http://www.w3.org/TR/REC-xml/#charsets
					if ((c >= '\u0020' && c <= '\uD7FF') || (c >= '\uE000' && c <= '\uFFFD')) {
						if (buffer != null)
							buffer.append(c);
						continue;
					}
					replace = Character.isWhitespace(c) ? " " : null; //$NON-NLS-1$
			}
			if (buffer == null) {
				buffer = new StringBuffer(txt.length() + 16);
				buffer.append(txt.substring(0, i));
			}
			if (replace != null)
				buffer.append(replace);
		}

		if (buffer == null)
			return txt;

		return buffer.toString();
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
		if (value != null && value.length() > 0) {
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
		print(escape(value.toString()));
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

	public void flush() {
		this.pw.flush();
	}

	public void writeProperties(Map<String, String> properties) {
		writeProperties(PROPERTIES_ELEMENT, properties);
	}

	public void writeProperties(String propertiesElement, Map<String, String> properties) {
		if (properties != null && properties.size() > 0) {
			start(propertiesElement);
			attribute(COLLECTION_SIZE_ATTRIBUTE, properties.size());
			for (Entry<String, String> entry : properties.entrySet()) {
				writeProperty(entry.getKey(), entry.getValue());
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
			return ""; // optional attribute with no value //$NON-NLS-1$
		}
		return name + "='" + escape(value) + '\''; //$NON-NLS-1$
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
			println(escape ? escape(s) : s);
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
