/*******************************************************************************
 * Copyright (c) 2008, 2022 IBM Corporation and others.
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
 *     Christoph Läubrich - Issue #20 - XMLParser should not require a bundle context but a Parser in the constructor
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.persistence;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.*;

/*
 * Class used to read a composite repository.
 */
public class CompositeParser extends XMLParser implements XMLConstants {

	private static final Version CURRENT_VERSION = Version.createOSGi(1, 0, 0);
	static final VersionRange XML_TOLERANCE = new VersionRange(CURRENT_VERSION, true, Version.createOSGi(2, 0, 0), false);
	private static final String REQUIRED_CAPABILITY_ELEMENT = "required"; //$NON-NLS-1$
	private static final String REPOSITORY_ELEMENT = "repository"; //$NON-NLS-1$
	String repositoryType;
	private CompositeRepositoryState theState;

	protected class ChildrenHandler extends AbstractHandler {
		private ArrayList<URI> children;

		public ChildrenHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, CHILDREN_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			children = (size != null ? new ArrayList<>(Integer.parseInt(size)) : new ArrayList<>(4));
		}

		public URI[] getChildren() {
			return children.toArray(new URI[children.size()]);
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(CHILD_ELEMENT)) {
				new ChildHandler(this, attributes, children);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class ChildHandler extends AbstractHandler {
		private final String[] required = new String[] {LOCATION_ELEMENT};
		private final String[] optional = new String[] {};

		URI currentRepo = null;

		private List<URI> repos;

		public ChildHandler(AbstractHandler parentHandler, Attributes attributes, List<URI> repos) {
			super(parentHandler, CHILD_ELEMENT);
			String[] values = parseAttributes(attributes, required, optional);
			this.repos = repos;
			//skip entire subrepository if the location is missing
			if (values[0] == null)
				return;
			currentRepo = checkURI(REQUIRED_CAPABILITY_ELEMENT, URI_ATTRIBUTE, values[0]);

		}

		@Override
		public void startElement(String name, Attributes attributes) {
			checkCancel();
		}

		@Override
		protected void finished() {
			if (currentRepo != null)
				repos.add(currentRepo);
		}
	}

	private final class RepositoryDocHandler extends DocHandler {

		public RepositoryDocHandler(String rootName, RootHandler rootHandler) {
			super(rootName, rootHandler);
		}

		@Override
		public void processingInstruction(String target, String data) throws SAXException {
			if (repositoryType.equals(target)) {
				Version repositoryVersion = extractPIVersion(target, data);
				if (!XML_TOLERANCE.isIncluded(repositoryVersion)) {
					throw new SAXException(NLS.bind(Messages.io_IncompatibleVersion, repositoryVersion, XML_TOLERANCE));
				}
			}
		}
	}

	/*
	 * Handler for the "repository" attribute.
	 */
	private final class RepositoryHandler extends RootHandler {

		private final String[] required = new String[] {NAME_ATTRIBUTE, TYPE_ATTRIBUTE, VERSION_ATTRIBUTE};
		private final String[] optional = new String[] {DESCRIPTION_ATTRIBUTE, PROVIDER_ATTRIBUTE};
		private PropertiesHandler propertiesHandler = null;
		private ChildrenHandler childrenHandler = null;
		private CompositeRepositoryState state;
		private String[] attrValues = new String[required.length + optional.length];

		public RepositoryHandler() {
			super();
		}

		public CompositeRepositoryState getRepository() {
			return state;
		}

		@Override
		protected void handleRootAttributes(Attributes attributes) {
			attrValues = parseAttributes(attributes, required, optional);
			attrValues[2] = checkVersion(REPOSITORY_ELEMENT, VERSION_ATTRIBUTE, attrValues[2]).toString();
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name==null) {
				invalidElement(name, attributes);
			} else switch (name) {
				case PROPERTIES_ELEMENT:
					if (propertiesHandler == null) {
						propertiesHandler = new PropertiesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
					break;
				case CHILDREN_ELEMENT:
					if (childrenHandler == null) {
						childrenHandler = new ChildrenHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
					break;
				default:
					invalidElement(name, attributes);
					break;
			}
		}

		/*
		 * If we parsed valid XML then fill in our repository state object with the parsed data.
		 */
		@Override
		protected void finished() {
			if (isValidXML()) {
				state = new CompositeRepositoryState();
				state.setName(attrValues[0]);
				state.setType(attrValues[1]);
				state.setVersion(attrValues[2]);
				state.setDescription(attrValues[3]);
				state.setProvider(attrValues[4]);
				state.setProperties((propertiesHandler == null ? new OrderedProperties(0) //
						: propertiesHandler.getProperties()));
				state.setChildren((childrenHandler == null ? new URI[0] //
						: childrenHandler.getChildren()));
			}
		}
	}

	public CompositeParser(String bundleId, String type) {
		super(bundleId);
		this.repositoryType = type;
	}

	public void parse(File file) throws IOException {
		// don't overwrite if we already have a filename/location
		if (errorContext == null)
			setErrorContext(file.getAbsolutePath());
		parse(new FileInputStream(file));
	}

	public synchronized void parse(InputStream stream) throws IOException {
		this.status = null;
		try {
			// TODO: currently not caching the parser since we make no assumptions
			//		 or restrictions on concurrent parsing
			XMLReader reader = getParser().getXMLReader();
			RepositoryHandler repositoryHandler = new RepositoryHandler();
			reader.setContentHandler(new RepositoryDocHandler(REPOSITORY_ELEMENT, repositoryHandler));
			reader.parse(new InputSource(stream));
			if (isValidXML()) {
				theState = repositoryHandler.getRepository();
			}
		} catch (SAXException e) {
			IOException ioException = new IOException(e.getMessage());
			ioException.initCause(e);
			throw ioException;
		} catch (ParserConfigurationException e) {
			IOException ioException = new IOException(e.getMessage());
			ioException.initCause(e);
			throw ioException;
		} finally {
			stream.close();
		}
	}

	public CompositeRepositoryState getRepositoryState() {
		return theState;
	}

	//TODO what?
	@Override
	protected Object getRootObject() {
		return null;
	}

	@Override
	protected String getErrorMessage() {
		return Messages.io_parseError;
	}

}
