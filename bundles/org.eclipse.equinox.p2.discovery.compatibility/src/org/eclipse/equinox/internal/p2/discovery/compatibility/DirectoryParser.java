/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.discovery.compatibility;

import java.io.IOException;
import java.io.Reader;
import org.eclipse.equinox.internal.p2.discovery.compatibility.Directory.Entry;
import org.eclipse.equinox.internal.p2.discovery.compatibility.util.DefaultSaxErrorHandler;
import org.eclipse.equinox.internal.p2.discovery.compatibility.util.IOWithCauseException;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A parser for {@link Directory directories}.
 * 
 * @author David Green
 */
public class DirectoryParser {
	/**
	 * parse the contents of a directory. The caller must close the given reader.
	 * 
	 * @param directoryContents
	 *            the contents of the directory
	 * @return a directory with 0 or more entries
	 * @throws IOException
	 *             if the directory cannot be read.
	 */
	public Directory parse(Reader directoryContents) throws IOException {
		XMLReader xmlReader;
		try {
			xmlReader = XMLReaderFactory.createXMLReader();
		} catch (SAXException e) {
			throw new IOWithCauseException(e.getMessage(), e);
		}
		xmlReader.setErrorHandler(new DefaultSaxErrorHandler());

		DirectoryContentHandler contentHandler = new DirectoryContentHandler();
		xmlReader.setContentHandler(contentHandler);

		try {
			xmlReader.parse(new InputSource(directoryContents));
		} catch (SAXException e) {
			throw new IOWithCauseException(e.getMessage(), e);
		}

		if (contentHandler.directory == null) {
			throw new IOException(Messages.DirectoryParser_no_directory);
		}

		return contentHandler.directory;
	}

	private class DirectoryContentHandler implements ContentHandler {

		Directory directory;

		/**
		 * @throws SAXException - required by contract, not really thrown 
		 */
		public void characters(char[] ch, int start, int length) throws SAXException {
			// ignore
		}

		/**
		 * @throws SAXException - required by contract, not really thrown 
		 */
		public void endDocument() throws SAXException {
			// ignore
		}

		/**
		 * @throws SAXException - required by contract, not really thrown 
		 */
		public void endElement(String uri, String localName, String qName) throws SAXException {
			// ignore
		}

		/**
		 * @throws SAXException - required by contract, not really thrown 
		 */
		public void endPrefixMapping(String prefix) throws SAXException {
			// ignore
		}

		/**
		 * @throws SAXException - required by contract, not really thrown 
		 */
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
			// ignore
		}

		/**
		 * @throws SAXException - required by contract, not really thrown 
		 */
		public void processingInstruction(String target, String data) throws SAXException {
			// ignore
		}

		public void setDocumentLocator(Locator locator) {
			// ignore
		}

		/**
		 * @throws SAXException - required by contract, not really thrown 
		 */
		public void skippedEntity(String name) throws SAXException {
			// ignore
		}

		/**
		 * @throws SAXException - required by contract, not really thrown 
		 */
		public void startDocument() throws SAXException {
			// ignore
		}

		public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
			if ("directory".equals(localName)) { //$NON-NLS-1$
				if (directory != null) {
					unexpectedElement(localName);
				}
				directory = new Directory();
			} else if (directory != null && "entry".equals(localName)) { //$NON-NLS-1$
				String url = atts.getValue("", "url"); //$NON-NLS-1$ //$NON-NLS-2$
				if (url != null && url.length() > 0) {
					Entry entry = new Entry();
					entry.setLocation(url);
					entry.setPermitCategories(Boolean.parseBoolean(atts.getValue("permitCategories"))); //$NON-NLS-1$
					directory.getEntries().add(entry);
				}
			}
			// else ignore
		}

		private void unexpectedElement(String localName) throws SAXException {
			throw new SAXException(NLS.bind(Messages.DirectoryParser_unexpected_element, localName));
		}

		/**
		 * @throws SAXException - required by contract, not really thrown 
		 */
		public void startPrefixMapping(String prefix, String uri) throws SAXException {
			// ignore
		}
	}
}
