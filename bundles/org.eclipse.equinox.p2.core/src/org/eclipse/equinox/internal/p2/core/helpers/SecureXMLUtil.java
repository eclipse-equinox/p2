/*******************************************************************************
 * Copyright (c) 2017, 2023 Manumitting Technologies Inc and others.
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
import javax.xml.transform.TransformerFactory;
import org.xml.sax.*;

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
		// completely disable external entities declarations:
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false); //$NON-NLS-1$
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); //$NON-NLS-1$
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
		// ignore DOCTYPE:
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false); //$NON-NLS-1$
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); //$NON-NLS-1$
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
		return factory;
	}

	/**
	 * Create a new {@link XMLReader}.
	 *
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static XMLReader newSecureXMLReader() throws SAXException, ParserConfigurationException {
		SAXParserFactory factory = newSecureSAXParserFactory();
		factory.setNamespaceAware(true);
		return factory.newSAXParser().getXMLReader();
	}

	/**
	 * Creates TransformerFactory which throws TransformerException when detecting
	 * external entities.
	 *
	 * @return javax.xml.transform.TransformerFactory
	 */
	public static TransformerFactory createTransformerFactoryWithErrorOnDOCTYPE() {
		TransformerFactory factory = TransformerFactory.newInstance();
		// prohibit the use of all protocols by external entities:
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); //$NON-NLS-1$
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, ""); //$NON-NLS-1$
		return factory;
	}
}
