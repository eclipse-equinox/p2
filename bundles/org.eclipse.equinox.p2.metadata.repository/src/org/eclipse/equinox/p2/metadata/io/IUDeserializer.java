/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sonatype, Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import javax.xml.parsers.*;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataParser;
import org.eclipse.equinox.internal.p2.persistence.Messages;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.xml.sax.*;

/**
 * This class allows to deserialize {@link IInstallableUnit}s that have been serialized with {@link IUSerializer}.
 * The deserializer is able to read data that have been serialized with previous versions of the serializer.
 * @since 1.2
 *
 */
public class IUDeserializer {
	private IUDeserializerParser deserializer;

	/**
	 * Construct a new instance of the deserializer.
	 */
	public IUDeserializer() {
		deserializer = new IUDeserializerParser(SAXParserFactory.newInstance());
	}

	/**
	 * Deserialize a set of {@link IInstallableUnit} from the input stream.
	 * @param input the input stream to deserialize {@link IInstallableUnit}s from. 
	 * @return the collection of {@link IInstallableUnit}s read from the input stream.
	 * @throws IOException
	 */
	public Collection<IInstallableUnit> read(InputStream input) throws IOException {
		return deserializer.parse(input);
	}

	private class IUDeserializerParser extends MetadataParser {
		private IUOnlyHandler iusHandler;
		private SAXParserFactory parserFactory;

		public IUDeserializerParser(SAXParserFactory factory) {
			super(null, null);
			this.parserFactory = factory;
		}

		public Collection<IInstallableUnit> parse(InputStream stream) throws IOException {
			try {
				getParser();
				iusHandler = new IUOnlyHandler();
				xmlReader.setContentHandler(iusHandler);
				xmlReader.parse(new InputSource(stream));
				if (isValidXML()) {
					return Arrays.asList(iusHandler.getInstallableUnits());
				}
				throw new IOException(status.toString());
			} catch (ParserConfigurationException configException) {
				IOException ioException = new IOException(configException.getMessage());
				ioException.initCause(configException);
				throw ioException;
			} catch (SAXException saxException) {
				IOException ioException = new IOException(saxException.getMessage());
				ioException.initCause(saxException);
				throw ioException;
			}
		}

		protected SAXParser getParser() throws ParserConfigurationException, SAXException {
			if (parserFactory == null) {
				throw new SAXException(Messages.XMLParser_No_SAX_Parser);
			}
			parserFactory.setNamespaceAware(true);
			parserFactory.setValidating(false);
			try {
				parserFactory.setFeature("http://xml.org/sax/features/string-interning", true); //$NON-NLS-1$
			} catch (SAXException se) {
				// some parsers may not support string interning
			}
			SAXParser theParser = parserFactory.newSAXParser();
			if (theParser == null) {
				throw new SAXException(Messages.XMLParser_No_SAX_Parser);
			}
			xmlReader = theParser.getXMLReader();
			return theParser;
		}

		class IUOnlyHandler extends RootHandler {

			private InstallableUnitsHandler unitsHandler;

			public IUOnlyHandler() {
				// default
			}

			protected void handleRootAttributes(Attributes attributes) {
				//Nothing to do
			}

			public void startElement(String name, Attributes attributes) {
				if (INSTALLABLE_UNITS_ELEMENT.equals(name)) {
					if (unitsHandler == null) {
						unitsHandler = new InstallableUnitsHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else {
					invalidElement(name, attributes);
				}
			}

			public IInstallableUnit[] getInstallableUnits() {
				if (unitsHandler == null)
					return null;
				return unitsHandler.getUnits();
			}
		}

		@Override
		protected Object getRootObject() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected String getErrorMessage() {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
