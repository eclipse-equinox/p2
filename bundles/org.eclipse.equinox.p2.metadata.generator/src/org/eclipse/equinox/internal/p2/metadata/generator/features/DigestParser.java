/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.generator.features;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.xml.parsers.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.Feature;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Default feature parser.
 * Parses the feature manifest file as defined by the platform.
 * 
 * @since 3.0
 */
public class DigestParser extends DefaultHandler {

	private final static SAXParserFactory parserFactory = SAXParserFactory.newInstance();
	private SAXParser parser;
	private final List features = new ArrayList();
	private final FeatureParser featureHandler = new FeatureParser(false);

	public DigestParser() {
		super();
		try {
			parserFactory.setNamespaceAware(true);
			this.parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			System.out.println(e);
		} catch (SAXException e) {
			System.out.println(e);
		}
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		featureHandler.characters(ch, start, length);
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("digest".equals(localName)) { //$NON-NLS-1$
			return;
		}
		if ("feature".equals(localName)) { //$NON-NLS-1$
			Feature feature = featureHandler.getResult();
			features.add(feature);
		} else
			featureHandler.endElement(uri, localName, qName);
	}

	public Feature[] parse(File location) {
		if (!location.exists())
			return null;

		InputStream is = null;
		try {
			JarFile jar = new JarFile(location);
			JarEntry entry = jar.getJarEntry("digest.xml"); //$NON-NLS-1$
			if (entry == null)
				return null;
			is = new BufferedInputStream(jar.getInputStream(entry));
			parser.parse(new InputSource(is), this);
			return (Feature[]) features.toArray(new Feature[features.size()]);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e1) {
				//
			}
		}
		return null;
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if ("digest".equals(localName)) { //$NON-NLS-1$
			return;
		}
		featureHandler.startElement(uri, localName, qName, attributes);
	}

}
