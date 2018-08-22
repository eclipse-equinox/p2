/*******************************************************************************
 * Copyright (c) 2017 Manumitting Technologies Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Manumitting Technologies Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core.helpers;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A utility class for creating an XML-related factories suitable for
 * for processing XML data from possibly malicious sources in a secure
 * fashion, avoiding XML Entity Expansion problem.
 */
public class SecureXMLUtil {
	/**
	 * Create a new {@link DocumentBuilderFactory}.
	 *
	 * @throws ParserConfigurationException
	 */
	public static DocumentBuilderFactory newSecureDocumentBuilderFactory() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		return factory;
	}

	/**
	 * Create a new {@link SAXParserFactory}.
	 *
	 * @throws ParserConfigurationException
	 * @throws SAXNotSupportedException
	 * @throws SAXNotRecognizedException
	 */
	public static SAXParserFactory newSecureSAXParserFactory() throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		return factory;
	}

	/**
	 * Create a new {@link XMLReader}.
	 *
	 * @throws SAXException
	 */
	public static XMLReader newSecureXMLReader() throws SAXException {
		XMLReader reader = XMLReaderFactory.createXMLReader();
		reader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		return reader;
	}
}
