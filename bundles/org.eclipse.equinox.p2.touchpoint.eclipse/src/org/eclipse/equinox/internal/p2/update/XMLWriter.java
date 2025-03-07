/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.update;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A simple XML writer.
 * 
 * Copied from the org.eclipse.core.resources bundle.
 */
public class XMLWriter extends PrintWriter {
	protected int tab;

	static final boolean ignoreWhitespace = Boolean.getBoolean("p2.ignoreWhitespace"); //$NON-NLS-1$

	/* constants */
	protected static final String XML_VERSION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"; //$NON-NLS-1$

	public XMLWriter(OutputStream output) {
		super(new OutputStreamWriter(output, StandardCharsets.UTF_8));
		tab = 0;
		println(XML_VERSION);
	}

	public void endTag(String name) {
		tab--;
		printTag('/' + name, null);
	}

	public void printSimpleTag(String name, Object value) {
		if (value != null) {
			printTag(name, null, true, false);
			print(getEscaped(String.valueOf(value)));
			printTag('/' + name, null, false, true);
		}
	}

	public void printTabulation() {
		for (int i = 0; i < tab; i++) {
			super.print('\t');
		}
	}

	public void printTag(String name, Map<String, String> parameters) {
		printTag(name, parameters, true, true);
	}

	public void printTag(String name, Map<String, String> parameters, boolean shouldTab, boolean newLine) {
		StringBuilder sb = new StringBuilder();
		sb.append("<"); //$NON-NLS-1$
		sb.append(name);
		if (parameters != null) {
			for (String key : parameters.keySet()) {
				sb.append(" "); //$NON-NLS-1$
				sb.append(key);
				sb.append("=\""); //$NON-NLS-1$
				sb.append(getEscaped(String.valueOf(parameters.get(key))));
				sb.append("\""); //$NON-NLS-1$
			}
		}
		sb.append(">"); //$NON-NLS-1$
		if (shouldTab && !ignoreWhitespace) {
			printTabulation();
		}
		if (newLine && !ignoreWhitespace) {
			println(sb.toString());
		} else {
			print(sb.toString());
		}
	}

	public void startTag(String name, Map<String, String> parameters) {
		startTag(name, parameters, true);
	}

	public void startTag(String name, Map<String, String> parameters, boolean newLine) {
		printTag(name, parameters, true, newLine);
		tab++;
	}

	private static void appendEscapedChar(StringBuffer buffer, char c) {
		String replacement = getReplacement(c);
		if (replacement != null) {
			buffer.append('&');
			buffer.append(replacement);
			buffer.append(';');
		} else {
			buffer.append(c);
		}
	}

	public static String getEscaped(String s) {
		StringBuffer result = new StringBuffer(s.length() + 10);
		for (int i = 0; i < s.length(); ++i) {
			appendEscapedChar(result, s.charAt(i));
		}
		return result.toString();
	}

	private static String getReplacement(char c) {
		// Encode special XML characters into the equivalent character references.
		// These five are defined by default for all XML documents.
		switch (c) {
		case '<':
			return "lt"; //$NON-NLS-1$
		case '>':
			return "gt"; //$NON-NLS-1$
		case '"':
			return "quot"; //$NON-NLS-1$
		case '\'':
			return "apos"; //$NON-NLS-1$
		case '&':
			return "amp"; //$NON-NLS-1$
		}
		return null;
	}
}
