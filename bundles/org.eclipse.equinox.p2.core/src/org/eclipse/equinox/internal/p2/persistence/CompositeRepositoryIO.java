/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.persistence;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.ICompositeRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.xml.sax.*;

/**
 * This class reads and writes composite repository metadata
 * (e.g. table of contents files);
 * 
 * This class is not used for reading or writing the actual composite repositories.
 */
public class CompositeRepositoryIO {

	public static class CompositeRepositoryState {
		public String Name;
		public String Type;
		public String Version;
		public String Provider;
		public String Description;
		public URI Location;
		public Map Properties;
		public URI[] Children;
	}

	/**
	 * Writes the given repository to the stream.
	 * This method performs buffering, and closes the stream when finished.
	 */
	public void write(ICompositeRepository repository, OutputStream output) {
		OutputStream bufferedOutput = null;
		try {
			try {
				bufferedOutput = new BufferedOutputStream(output);
				Writer repositoryWriter = new Writer(bufferedOutput);
				repositoryWriter.write(repository);
			} finally {
				if (bufferedOutput != null) {
					bufferedOutput.close();
				}
			}
		} catch (IOException ioe) {
			// TODO shouldn't this throw a core exception?
			ioe.printStackTrace();
		}
	}

	/**
	 * Reads the composite repository from the given stream,
	 * and returns the contained array of abstract composite repositories.
	 * 
	 * This method performs buffering, and closes the stream when finished.
	 */
	public CompositeRepositoryState read(URL location, InputStream input, IProgressMonitor monitor) throws ProvisionException {
		BufferedInputStream bufferedInput = null;
		try {
			try {
				bufferedInput = new BufferedInputStream(input);
				Parser repositoryParser = new Parser(Activator.getContext(), Activator.ID);
				repositoryParser.parse(input);
				IStatus result = repositoryParser.getStatus();
				switch (result.getSeverity()) {
					case IStatus.CANCEL :
						throw new OperationCanceledException();
					case IStatus.ERROR :
						throw new ProvisionException(result);
					case IStatus.WARNING :
					case IStatus.INFO :
						LogHelper.log(result);
				}
				CompositeRepositoryState repositoryState = repositoryParser.getRepositoryState();
				if (repositoryState == null)
					throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, Messages.io_parseError, null));
				return repositoryState;
			} finally {
				if (bufferedInput != null)
					bufferedInput.close();
			}
		} catch (IOException ioe) {
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, ioe));
		}
	}

	private interface XMLConstants extends org.eclipse.equinox.internal.p2.persistence.XMLConstants {

		// Constants defining the structure of the XML for a ICompositeRepository

		// A format version number for composite repository XML.
		public static final Version CURRENT_VERSION = new Version(1, 0, 0);
		public static final VersionRange XML_TOLERANCE = new VersionRange(CURRENT_VERSION, true, new Version(2, 0, 0), false);

		// Constants for processing instructions
		public static final String PI_REPOSITORY_TARGET = "artifactRepository"; //$NON-NLS-1$
		public static XMLWriter.ProcessingInstruction[] PI_DEFAULTS = new XMLWriter.ProcessingInstruction[] {XMLWriter.ProcessingInstruction.makeClassVersionInstruction(PI_REPOSITORY_TARGET, ICompositeRepository.class, CURRENT_VERSION)};

		// Constants for artifact repository elements
		public static final String REPOSITORY_ELEMENT = "repository"; //$NON-NLS-1$
	}

	// XML writer for a ICompositeRepository
	protected class Writer extends CompositeWriter implements XMLConstants {

		public Writer(OutputStream output) throws IOException {
			super(output, PI_DEFAULTS);
		}

		/**
		 * Write the given composite repository to the output stream.
		 */
		public void write(IRepository repository) {
			start(REPOSITORY_ELEMENT);
			attribute(NAME_ATTRIBUTE, repository.getName());
			attribute(TYPE_ATTRIBUTE, repository.getType());
			attribute(VERSION_ATTRIBUTE, repository.getVersion());
			attributeOptional(PROVIDER_ATTRIBUTE, repository.getProvider());
			attributeOptional(DESCRIPTION_ATTRIBUTE, repository.getDescription()); // TODO: could be cdata?

			writeProperties(repository.getProperties());

			ArrayList children = ((ICompositeRepository) repository).getChildren();
			writeChildren(children.iterator(), children.size());

			end(REPOSITORY_ELEMENT);
			flush();
		}
	}

	/*
	 * Parser for the contents of a ICompositeRepository,
	 * as written by the Writer class.
	 */
	private class Parser extends CompositeParser implements XMLConstants {

		private CompositeRepositoryState theState;

		public Parser(BundleContext context, String bundleId) {
			super(context, bundleId);
		}

		public void parse(File file) throws IOException {
			parse(new FileInputStream(file));
		}

		public synchronized void parse(InputStream stream) throws IOException {
			this.status = null;
			try {
				// TODO: currently not caching the parser since we make no assumptions
				//		 or restrictions on concurrent parsing
				getParser();
				RepositoryHandler repositoryHandler = new RepositoryHandler();
				xmlReader.setContentHandler(new RepositoryDocHandler(REPOSITORY_ELEMENT, repositoryHandler));
				xmlReader.parse(new InputSource(stream));
				if (isValidXML()) {
					theState = repositoryHandler.getRepository();
				}
			} catch (SAXException e) {
				throw new IOException(e.getMessage());
			} catch (ParserConfigurationException e) {
				throw new IOException(e.getMessage());
			} finally {
				stream.close();
			}
		}

		public CompositeRepositoryState getRepositoryState() {
			return theState;
		}

		//TODO what?
		protected Object getRootObject() {
			return null;
		}

		protected String getErrorMessage() {
			return Messages.io_parseError;
		}

		private final class RepositoryDocHandler extends DocHandler {

			public RepositoryDocHandler(String rootName, RootHandler rootHandler) {
				super(rootName, rootHandler);
			}

			public void processingInstruction(String target, String data) throws SAXException {
				if (PI_REPOSITORY_TARGET.equals(target)) {
					// TODO: should the root handler be constructed based on class
					// 		 via an extension registry mechanism?
					// String clazz = extractPIClass(data);
					// TODO: version tolerance by extension
					Version repositoryVersion = extractPIVersion(target, data);
					if (!XML_TOLERANCE.isIncluded(repositoryVersion)) {
						throw new SAXException(NLS.bind(Messages.io_IncompatibleVersion, repositoryVersion, XML_TOLERANCE));
					}
				}
			}
		}

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

			protected void handleRootAttributes(Attributes attributes) {
				attrValues = parseAttributes(attributes, required, optional);
				attrValues[2] = checkVersion(REPOSITORY_ELEMENT, VERSION_ATTRIBUTE, attrValues[2]).toString();
			}

			public void startElement(String name, Attributes attributes) {
				if (PROPERTIES_ELEMENT.equals(name)) {
					if (propertiesHandler == null) {
						propertiesHandler = new PropertiesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (CHILDREN_ELEMENT.equals(name)) {
					if (childrenHandler == null) {
						childrenHandler = new ChildrenHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else {
					invalidElement(name, attributes);
				}
			}

			protected void finished() {
				if (isValidXML()) {
					state = new CompositeRepositoryState();
					state.Name = attrValues[0];
					state.Type = attrValues[1];
					state.Version = attrValues[2];
					state.Description = attrValues[3];
					state.Provider = attrValues[4];
					state.Properties = (propertiesHandler == null ? new OrderedProperties(0) //
							: propertiesHandler.getProperties());
					state.Children = (childrenHandler == null ? new URI[0] //
							: childrenHandler.getChildren());
				}
			}
		}
	}
}
