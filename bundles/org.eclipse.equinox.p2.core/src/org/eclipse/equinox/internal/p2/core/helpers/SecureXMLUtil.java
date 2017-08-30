/*******************************************************************************
 * Copyright (c) 20017 Manumitting Technologies Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Manumitting Technologies Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core.helpers;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.Activator;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A utility class for processing XML data in a secure fashion,
 * avoiding XML Entity Expansion problems
 */
public class SecureXMLUtil {
	/**
	 * Create a new {@link DocumentBuilderFactory} suitable for processing
	 * XML data from possibly malicious sources.  For example, data retrieved
	 * from remote p2 metadata and artifacts repositories.
	 */
	public static DocumentBuilderFactory newSecureDocumentBuilderFactory() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// FEATURE_SECURE_PROCESSING is documented as must be supported by all implementations
		try {
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch (ParserConfigurationException e) {
			LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Feature not supported", e)); //$NON-NLS-1$
		}
		return factory;
	}

	/**
	 * Create a new {@link SAXParserFactory} suitable for processing
	 * XML data from possibly malicious sources.  For example, data retrieved
	 * from remote p2 metadata and artifacts repositories.
	 */
	public static SAXParserFactory newSecureSAXParserFactory() {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		// FEATURE_SECURE_PROCESSING is documented as must be supported by all implementations
		try {
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
			LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Feature not supported", e)); //$NON-NLS-1$
		}
		return factory;
	}

	/**
	 * Create a new {@link XMLReader} suitable for processing
	 * XML data from possibly malicious sources.  For example, data retrieved
	 * from remote p2 metadata and artifacts repositories.
	 */
	public static XMLReader newSecureXMLReader() throws SAXException {
		XMLReader reader = XMLReaderFactory.createXMLReader();
		try {
			reader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch (SAXNotRecognizedException | SAXNotSupportedException e) {
			LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Feature not supported", e)); //$NON-NLS-1$
		}
		return reader;
	}
}
