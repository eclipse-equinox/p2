/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.persistence;

import static java.util.stream.Collectors.joining;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.eclipse.equinox.p2.metadata.Version;

public class XMLWriter implements XMLConstants {

	static final boolean ignoreWhitespace = Boolean.getBoolean("p2.ignoreWhitespace"); //$NON-NLS-1$

	public static class ProcessingInstruction {

		private final String target;
		private final String[] data;

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

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("<?"); //$NON-NLS-1$
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

	private final Stack<String> elements; // XML elements that have not yet been closed
	private boolean open; // Can attributes be added to the current element?
	private final String indent; // used for each level of indentation

	private final PrintWriter pw;

	public XMLWriter(OutputStream output, ProcessingInstruction[] piElements) {
		this.pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8)), false);
		println(ProcessingInstruction.XML_UTF8);
		this.elements = new Stack<>();
		this.open = false;
		this.indent = "  "; //$NON-NLS-1$
		if (piElements != null) {
			for (ProcessingInstruction piElement : piElements) {
				println(piElement.toString());
			}
		}
	}

	// start a new element
	public void start(String name) {
		if (this.open) {
			println('>');
		}
		if (!ignoreWhitespace) {
			indent();
		}
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
		StringBuilder buffer = null;
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
						if (buffer != null) {
							buffer.append(c);
						}
						continue;
					}
					replace = Character.isWhitespace(c) ? " " : null; //$NON-NLS-1$
			}
			if (buffer == null) {
				buffer = new StringBuilder(txt.length() + 16);
				buffer.append(txt.substring(0, i));
			}
			if (replace != null) {
				buffer.append(replace);
			}
		}

		if (buffer == null) {
			return txt;
		}

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

	public void writeProperties(Map<String, ?> properties) {
		writeProperties(PROPERTIES_ELEMENT, properties);
	}

	public void writeProperties(String propertiesElement, Map<String, ?> properties) {
		if (properties == null || properties.isEmpty()) {
			return;
		}

		start(propertiesElement);
		attribute(COLLECTION_SIZE_ATTRIBUTE, properties.size());
		properties.forEach(this::writeProperty);
		end();
	}

	public void writeProperty(String name, Object value) {
		String type;
		String valueStr;

		if (Collection.class.isAssignableFrom(value.getClass())) {
			Collection<?> coll = (Collection<?>) value;

			type = PROPERTY_TYPE_LIST;
			String elType = resolvePropertyType(coll.iterator().next());
			if (elType != null) {
				type += String.format("<%s>", elType); //$NON-NLS-1$
			}

			valueStr = coll.stream().map(Object::toString).collect(joining(",")); //$NON-NLS-1$
		} else {
			type = resolvePropertyType(value);
			valueStr = value.toString();
		}

		start(PROPERTY_ELEMENT);
		attribute(PROPERTY_NAME_ATTRIBUTE, name);
		attribute(PROPERTY_VALUE_ATTRIBUTE, valueStr);
		attributeOptional(PROPERTY_TYPE_ATTRIBUTE, type);
		end();
	}

	private String resolvePropertyType(Object value) {
		if (value instanceof Integer) {
			return PROPERTY_TYPE_INTEGER;
		}
		if (value instanceof Long) {
			return PROPERTY_TYPE_LONG;
		}
		if (value instanceof Float) {
			return PROPERTY_TYPE_FLOAT;
		}
		if (value instanceof Double) {
			return PROPERTY_TYPE_DOUBLE;
		}
		if (value instanceof Byte) {
			return PROPERTY_TYPE_BYTE;
		}
		if (value instanceof Short) {
			return PROPERTY_TYPE_SHORT;
		}
		if (value instanceof Character) {
			return PROPERTY_TYPE_CHARACTER;
		}
		if (value instanceof Boolean) {
			return PROPERTY_TYPE_BOOLEAN;
		}
		if (value instanceof Version) {
			return PROPERTY_TYPE_VERSION;
		}

		// Null is read back as String
		// NOTE: Using string as default is needed for backward compatibility with properties that are always String like
		// the IU properties
		return null;
	}

	protected static String attributeImage(String name, String value) {
		if (value == null) {
			return ""; // optional attribute with no value //$NON-NLS-1$
		}
		return name + "='" + escape(value) + '\''; //$NON-NLS-1$
	}

	private void println(char c) {
		if (ignoreWhitespace) {
			this.pw.print(c);
		} else {
			this.pw.println(c);
		}
	}

	private void println(String s) {
		if (ignoreWhitespace) {
			this.pw.print(s);
		} else {
			this.pw.println(s);
		}
	}

	private void println() {
		if (!ignoreWhitespace) {
			this.pw.println();
		}
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
			if (!ignoreWhitespace) {
				indent();
			}
			println(escape ? escape(s) : s);
		}
	}

	private void indent() {
		if (!ignoreWhitespace) {
			for (int i = this.elements.size(); i > 0; i -= 1) {
				print(this.indent);
			}
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
