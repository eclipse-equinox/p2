/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.xml.parsers.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureManifestParser;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.osgi.util.NLS;
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
	private final List<Feature> features = new ArrayList<Feature>();
	private final FeatureManifestParser featureHandler = new FeatureManifestParser(false);

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

	public Feature[] parse(File localFile, URI location) {
		if (!localFile.exists())
			return null;

		if (location == null)
			location = localFile.toURI();

		JarFile jar = null;
		InputStream is = null;
		try {
			jar = new JarFile(localFile);
			JarEntry entry = jar.getJarEntry("digest.xml"); //$NON-NLS-1$
			if (entry == null)
				return null;
			is = new BufferedInputStream(jar.getInputStream(entry));
			parser.parse(new InputSource(is), this);
			return features.toArray(new Feature[features.size()]);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingDigest, location), e));
		} catch (SAXException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.ErrorReadingDigest, location), e));
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e1) {
				//
			}
			try {
				if (jar != null)
					jar.close();
			} catch (IOException e) {
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
