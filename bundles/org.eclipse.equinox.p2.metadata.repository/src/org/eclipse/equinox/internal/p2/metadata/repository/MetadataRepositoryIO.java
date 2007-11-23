/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Prashant Deva - Bug 194674 [prov] Provide write access to metadata repository
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.io.*;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataParser;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataWriter;
import org.eclipse.equinox.internal.p2.persistence.XMLWriter;
import org.eclipse.equinox.p2.core.repository.RepositoryCreationException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.eclipse.equinox.spi.p2.metadata.repository.AbstractMetadataRepository.RepositoryState;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.xml.sax.*;

/**
 * This class reads and writes provisioning metadata.
 */
public class MetadataRepositoryIO {

	/**
	 * Reads metadata from the given stream, and returns the contained array
	 * of abstract metadata repositories.
	 * This method performs buffering, and closes the stream when finished.
	 */
	public IMetadataRepository read(InputStream input) throws RepositoryCreationException {
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
						throw new RepositoryCreationException(new CoreException(result));
					case IStatus.WARNING :
					case IStatus.INFO :
						LogHelper.log(result);
				}
				return repositoryParser.getRepository();
			} finally {
				if (bufferedInput != null)
					bufferedInput.close();
			}
		} catch (IOException ioe) {
			throw new RepositoryCreationException(ioe);
		}
	}

	/**
	 *
	 */
	public void write(IMetadataRepository repository, OutputStream output) {
		OutputStream bufferedOutput = null;
		try {
			try {
				bufferedOutput = new BufferedOutputStream(output);
				Writer repositoryWriter = new Writer(bufferedOutput, repository.getClass());
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

	private interface XMLConstants extends org.eclipse.equinox.internal.p2.metadata.repository.io.XMLConstants {

		// Constants defining the structure of the XML for a MetadataRepository

		// A format version number for metadata repository XML.
		public static final String XML_VERSION = "0.0.1"; //$NON-NLS-1$
		public static final Version CURRENT_VERSION = new Version(XML_VERSION);
		public static final VersionRange XML_TOLERANCE = new VersionRange(CURRENT_VERSION, true, new Version(CURRENT_VERSION.getMajor() + 1, 0,0), true);

		// Constants for processing Instructions
		public static final String PI_REPOSITORY_TARGET = "metadataRepository"; //$NON-NLS-1$

		// Constants for metadata repository elements
		public static final String REPOSITORY_ELEMENT = "repository"; //$NON-NLS-1$

	}

	protected XMLWriter.ProcessingInstruction[] createPI(Class repositoryClass) {
		//TODO We should remove this processing instruction, but currently old clients rely on this. See bug 210450.
		return new XMLWriter.ProcessingInstruction[] {XMLWriter.ProcessingInstruction.makeClassVersionInstruction(XMLConstants.PI_REPOSITORY_TARGET, repositoryClass, XMLConstants.CURRENT_VERSION)};
	}

	// XML writer for a IMetadataRepository
	protected class Writer extends MetadataWriter implements XMLConstants {

		public Writer(OutputStream output, Class repositoryClass) throws IOException {
			super(output, createPI(repositoryClass));
		}

		/**
		 * Write the given metadata repository to the output stream.
		 */
		public void write(IMetadataRepository repository) {
			start(REPOSITORY_ELEMENT);
			attribute(NAME_ATTRIBUTE, repository.getName());
			attribute(TYPE_ATTRIBUTE, repository.getType());
			attribute(VERSION_ATTRIBUTE, repository.getVersion());
			attributeOptional(PROVIDER_ATTRIBUTE, repository.getProvider());
			attributeOptional(DESCRIPTION_ATTRIBUTE, repository.getDescription()); // TODO: could be cdata?

			writeProperties(repository.getProperties());
			writeInstallableUnits(getInstallableUnits(repository));

			end(REPOSITORY_ELEMENT);
			flush();
		}

		private IInstallableUnit[] getInstallableUnits(IMetadataRepository repository) {
			// TODO: there must be a better solution to the problem.
			Set units = null;
			if (repository instanceof LocalMetadataRepository) {
				units = ((LocalMetadataRepository) repository).getInstallableUnits();
			} else if (repository instanceof URLMetadataRepository) {
				units = ((URLMetadataRepository) repository).getInstallableUnits();
			} else {
				return repository.getInstallableUnits(new NullProgressMonitor());
			}
			return (units == null ? new IInstallableUnit[0] //
					: (IInstallableUnit[]) units.toArray(new IInstallableUnit[units.size()]));
		}
	}

	/*
	 * 	Parser for the contents of a metadata repository,
	 * 	as written by the Writer class.
	 */
	private class Parser extends MetadataParser implements XMLConstants {

		private IMetadataRepository theRepository = null;
		protected Class theRepositoryClass = null;

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
					theRepository = repositoryHandler.getRepository();
				}
			} catch (SAXException e) {
				throw new IOException(e.getMessage());
			} catch (ParserConfigurationException e) {
				throw new IOException(e.getMessage());
			} finally {
				stream.close();
			}
		}

		public IMetadataRepository getRepository() {
			return theRepository;
		}

		public Class getRepositoryClass() {
			return theRepositoryClass;
		}

		protected Object getRootObject() {
			return theRepository;
		}

		private final class RepositoryDocHandler extends DocHandler {

			public RepositoryDocHandler(String rootName, RootHandler rootHandler) {
				super(rootName, rootHandler);
			}

			public void processingInstruction(String target, String data) throws SAXException {
				if (PI_REPOSITORY_TARGET.equals(target)) {
					Version repositoryVersion = extractPIVersion(target, data);
					if (!MetadataRepositoryIO.XMLConstants.XML_TOLERANCE.isIncluded(repositoryVersion)) {
						throw new SAXException(NLS.bind(Messages.MetadataRepositoryIO_Parser_Has_Incompatible_Version, repositoryVersion, MetadataRepositoryIO.XMLConstants.XML_TOLERANCE));
					}
				}
			}

		}

		private final class RepositoryHandler extends RootHandler {

			private final String[] required = new String[] {NAME_ATTRIBUTE, TYPE_ATTRIBUTE, VERSION_ATTRIBUTE};
			private final String[] optional = new String[] {DESCRIPTION_ATTRIBUTE, PROVIDER_ATTRIBUTE};

			private InstallableUnitsHandler unitsHandler = null;
			private PropertiesHandler propertiesHandler = null;

			private AbstractMetadataRepository repository = null;

			private RepositoryState state = new RepositoryState();

			public RepositoryHandler() {
				super();
			}

			public IMetadataRepository getRepository() {
				return repository;
			}

			protected void handleRootAttributes(Attributes attributes) {
				String[] values = parseAttributes(attributes, required, optional);
				Version version = checkVersion(this.elementHandled, VERSION_ATTRIBUTE, values[2]);
				state.Name = values[0];
				state.Type = values[1];
				state.Version = version;
				state.Description = values[3];
				state.Provider = values[4];
				state.Location = null;
			}

			public void startElement(String name, Attributes attributes) {
				if (PROPERTIES_ELEMENT.equals(name)) {
					if (propertiesHandler == null) {
						propertiesHandler = new PropertiesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (INSTALLABLE_UNITS_ELEMENT.equals(name)) {
					if (unitsHandler == null) {
						unitsHandler = new InstallableUnitsHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else {
					invalidElement(name, attributes);
				}
			}

			protected void finished() {
				if (isValidXML()) {
					state.Properties = (propertiesHandler == null ? new OrderedProperties(0) //
							: propertiesHandler.getProperties());
					state.Units = (unitsHandler == null ? new IInstallableUnit[0] //
							: unitsHandler.getUnits());
					try {
						//can't create repository if missing type - this is already logged when parsing attributes
						if (state.Type == null)
							return;
						Class clazz = Class.forName(state.Type);
						Object repositoryObject = clazz.newInstance();
						if (repositoryObject instanceof AbstractMetadataRepository) {
							repository = (AbstractMetadataRepository) repositoryObject;
							repository.initialize(state);
						}
					} catch (InstantiationException e) {
						// TODO: Throw a SAXException
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO: Throw a SAXException
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						// TODO: Throw a SAXException
						e.printStackTrace();
					}
				}
			}
		}

		protected String getErrorMessage() {
			return Messages.MetadataRepositoryIO_Parser_Error_Parsing_Repository;
		}

		public String toString() {
			// TODO:
			return null;
		}
	}
}
