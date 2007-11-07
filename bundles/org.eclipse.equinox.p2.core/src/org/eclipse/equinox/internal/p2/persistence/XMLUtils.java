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
package org.eclipse.equinox.internal.p2.persistence;

public class XMLUtils {

	public static String escape(String txt) {
		// Traverse the string from right to left as the length
		// may increase as result of processing
		for (int i = txt.length() - 1; i >= 0; i -= 1) {
			String replace;
			switch (txt.charAt(i)) {
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
				default :
					continue;
			}
			txt = txt.substring(0, i) + replace + txt.substring(i + 1);
		}
		return txt;
	}

	// A string constant for no indentation.
	protected static final String NO_INDENT = ""; //$NON-NLS-1$

	// A constant for maximum line length before break and indent
	protected static final int MAX_LINE = 72;

	/*
	 *  Construct a string for a simple XML element
	 *  with no text or child elements, just attributes
	 *  expressed as name/value pairs.
	 *  
	 *  e.g <elemName attr1='value1' attr2='value2'/>
	 */
	public static String simpleElement(String elementName, String[] attrNameValuePairs) {
		StringBuffer buffer = new StringBuffer();
		simpleElement(elementName, attrNameValuePairs, buffer, NO_INDENT);

		return buffer.toString();
	}

	/*
	 *  Write a string for a simple XML element
	 *  with no text or child elements, just attributes
	 *  expressed as name/value pairs, into the given
	 *  string buffer using the given indentation.
	 *  
	 *  e.g. <elemName attr1='value1' attr2='value2'/>
	 */
	public static void simpleElement(String elementName, String[] attrNameValuePairs, StringBuffer buffer, String indent) {
		element(elementName, attrNameValuePairs, buffer, indent, "/>"); //$NON-NLS-1$
	}

	/*
	 *  Write a string for an open XML element, including
	 *  the attributes expressed as name/value pairs,
	 *  into the given string buffer using the given indentation.
	 *  
	 *  The caller is responsible for adding any child elements
	 *  and closing the element.
	 *  
	 *  Eg. <elemName attr1='value1' attr2='value2'>
	 */
	public static void openElement(String elementName, String[] attrNameValuePairs, StringBuffer buffer, String indent) {
		element(elementName, attrNameValuePairs, buffer, indent, ">"); //$NON-NLS-1$
	}

	/*
	 *  Write a string for an unterminated XML element, including
	 *  the attributes expressed as name/value pairs,
	 *  into the given string buffer using the given indentation.
	 *  
	 *  The caller is responsible for terminating the element.
	 *  
	 *  e.g. <elemName attr1='value1' attr2='value2' 
	 */
	public static void unterminatedElement(String elementName, String[] attrNameValuePairs, StringBuffer buffer, String indent) {
		buffer.append(indent).append('<').append(elementName);

		String attrPrefix = " "; //$NON-NLS-1$
		int totalLen = buffer.length() + 1;
		for (int i = 0; i < attrNameValuePairs.length; i++) {
			String next = attrNameValuePairs[i];
			if (next != null) {
				totalLen += next.length() + 2;
				if (totalLen >= MAX_LINE) {
					attrPrefix = '\n' + indent + "  "; //$NON-NLS-1$
					break;
				}
			}
		}

		for (int i = 0; i < attrNameValuePairs.length;) {
			String name = attrNameValuePairs[i++];
			if (name.length() > 0) {
				String value = (i < attrNameValuePairs.length ? attrNameValuePairs[i++] : null);
				if (value != null) {
					buffer.append(attrPrefix).append(name);
					buffer.append("='").append(escape(value)).append('\''); //$NON-NLS-1$
				}
			}
		}
	}

	/*
	 *  Fill the string buffer for a XML element, including
	 *  the attributes expressed as name/value pairs, and
	 *  terminate with the given termination string.
	 *  
	 *  e.g. <elemName attr1='value1' attr2='value2'>
	 *   or  <elemName attr1='value1' attr2='value2'>
	 */
	private static void element(String elementName, String[] attrNameValuePairs, StringBuffer buffer, String indent, String termination) {
		unterminatedElement(elementName, attrNameValuePairs, buffer, indent);
		buffer.append(termination);
	}

	private XMLUtils() {
		// Private constructor to ensure no instances are created.
	}

}
