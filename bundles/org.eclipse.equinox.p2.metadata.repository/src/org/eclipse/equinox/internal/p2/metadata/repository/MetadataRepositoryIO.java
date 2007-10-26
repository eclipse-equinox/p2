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

import com.thoughtworks.xstream.XStream;
import java.io.*;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.helpers.*;
import org.eclipse.equinox.p2.core.repository.RepositoryCreationException;
import org.eclipse.equinox.p2.metadata.*;
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
 * The implementation currently uses XStream.
 */
class MetadataRepositoryIO {

	/**
	 * Reads metadata from the given stream, and returns the contained array
	 * of abstract metadata repositories.
	 * This method performs buffering, and closes the stream when finished.
	 * 
	 * @deprecated
	 */
	public static IMetadataRepository read(InputStream input) throws RepositoryCreationException {
		XStream stream = new XStream();
		BufferedInputStream bufferedInput = null;
		try {
			try {
				bufferedInput = new BufferedInputStream(input);
				return (IMetadataRepository) stream.fromXML(bufferedInput);
			} finally {
				if (bufferedInput != null)
					bufferedInput.close();
			}
		} catch (IOException e) {
			throw new RepositoryCreationException(e);
		}
	}

	/**
	 * 	@deprecated
	 */
	public static void write(IMetadataRepository repository, OutputStream output) {
		XStream stream = new XStream();
		OutputStream bufferedOutput = null;
		try {
			try {
				bufferedOutput = new BufferedOutputStream(output);
				stream.toXML(repository, bufferedOutput);
			} finally {
				if (bufferedOutput != null)
					bufferedOutput.close();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writeNew(IMetadataRepository repository, OutputStream output) {
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

	/**
	 * Reads metadata from the given stream, and returns the contained array
	 * of abstract metadata repositories.
	 * This method performs buffering, and closes the stream when finished.
	 */
	public IMetadataRepository readNew(InputStream input) throws RepositoryCreationException {
		BufferedInputStream bufferedInput = null;
		try {
			try {
				bufferedInput = new BufferedInputStream(input);

				Parser repositoryParser = new Parser(Activator.getContext(), Activator.ID);
				repositoryParser.parse(input);
				if (repositoryParser.isValidXML()) {
					throw new RepositoryCreationException(new CoreException(repositoryParser.getStatus()));
				}
				IMetadataRepository theRepository = repositoryParser.getRepository();
				// TODO: temporary - try write after read for comparison:
				//			see note below about call to writeInstallableUnits(...)
				writeNew(theRepository, new FileOutputStream(new File("C:/Ap2/servers2/metadataRepository/writeback.xml"))); //$NON-NLS-1$
				return theRepository;
			} finally {
				if (bufferedInput != null)
					bufferedInput.close();
			}
		} catch (IOException ioe) {
			throw new RepositoryCreationException(ioe);
		}
	}

	private interface XMLConstants extends org.eclipse.equinox.p2.core.helpers.XMLConstants {

		// Constants defining the structure of the XML for a MetadataRepository

		// A format version number for metadata repository XML.
		public static final String XML_VERSION = "0.0.1"; //$NON-NLS-1$
		public static final Version CURRENT_VERSION = new Version(XML_VERSION);
		public static final VersionRange XML_TOLERANCE = new VersionRange(CURRENT_VERSION, true, CURRENT_VERSION, true);

		// Constants for processing Instructions
		public static final String PI_REPOSITORY_TARGET = "metadataRepository"; //$NON-NLS-1$
		//public static XMLWriter.ProcessingInstruction[] PI_DEFAULTS = new XMLWriter.ProcessingInstruction[] {XMLWriter.ProcessingInstruction.makeClassVersionInstruction(PI_REPOSITORY_TARGET, IMetadataRepository.class, CURRENT_VERSION)};

		// Constants for metadata repository elements
		public static final String REPOSITORY_ELEMENT = "repository"; //$NON-NLS-1$
		public static final String INSTALLABLE_UNITS_ELEMENT = "units"; //$NON-NLS-1$
		public static final String INSTALLABLE_UNIT_ELEMENT = "unit"; //$NON-NLS-1$
		public static final String PROCESSING_STEPS_ELEMENT = "processing"; //$NON-NLS-1$
		public static final String PROCESSING_STEP_ELEMENT = "step"; //$NON-NLS-1$

		// Constants for sub-elements of an installable unit element
		public static final String ARTIFACT_KEYS_ELEMENT = "artifacts"; //$NON-NLS-1$
		public static final String ARTIFACT_KEY_ELEMENT = "artifact"; //$NON-NLS-1$
		public static final String REQUIRED_CAPABILITIES_ELEMENT = "requires"; //$NON-NLS-1$
		public static final String REQUIRED_CAPABILITY_ELEMENT = "required"; //$NON-NLS-1$
		public static final String PROVIDED_CAPABILITIES_ELEMENT = "provides"; //$NON-NLS-1$
		public static final String PROVIDED_CAPABILITY_ELEMENT = "provided"; //$NON-NLS-1$
		public static final String TOUCHPOINT_TYPE_ELEMENT = "touchpoint"; //$NON-NLS-1$
		public static final String TOUCHPOINT_DATA_ELEMENT = "touchpointData"; //$NON-NLS-1$
		public static final String IU_FILTER_ELEMENT = "filter"; //$NON-NLS-1$
		public static final String APPLICABILITY_FILTER_ELEMENT = "applicability"; //$NON-NLS-1$

		// Constants for attributes of an installable unit element
		public static final String SINGLETON_ATTRIBUTE = "singleton"; //$NON-NLS-1$
		public static final String FRAGMENT_ATTRIBUTE = "fragment"; //$NON-NLS-1$

		// Constants for attributes of a fragment installable unit element
		public static final String FRAGMENT_HOST_ID_ATTRIBUTE = "hostId"; //$NON-NLS-1$
		public static final String FRAGMENT_HOST_RANGE_ATTRIBUTE = "hostRange"; //$NON-NLS-1$

		// Constants for sub-elements of a required capability element
		public static final String CAPABILITY_FILTER_ELEMENT = "filter"; //$NON-NLS-1$
		public static final String CAPABILITY_SELECTORS_ELEMENT = "selectors"; //$NON-NLS-1$
		public static final String CAPABILITY_SELECTOR_ELEMENT = "selector"; //$NON-NLS-1$

		// Constants for attributes of a required capability element
		public static final String CAPABILITY_OPTIONAL_ATTRIBUTE = "optional"; //$NON-NLS-1$
		public static final String CAPABILITY_MULTIPLE_ATTRIBUTE = "multiple"; //$NON-NLS-1$

		// Constants for attributes of an artifact key element
		public static final String ARTIFACT_KEY_NAMESPACE_ATTRIBUTE = NAMESPACE_ATTRIBUTE;
		public static final String ARTIFACT_KEY_CLASSIFIER_ATTRIBUTE = "classifier"; //$NON-NLS-1$

		// Constants for sub-elements of a touchpoint data element
		public static final String TOUCHPOINT_DATA_INSTRUCTIONS_ELEMENT = "instructions"; //$NON-NLS-1$
		public static final String TOUCHPOINT_DATA_INSTRUCTION_ELEMENT = "instruction"; //$NON-NLS-1$

		// Constants for attributes of an a touchpoint data instruction element
		public static final String TOUCHPOINT_DATA_INSTRUCTION_KEY_ATTRIBUTE = "key"; //$NON-NLS-1$

	}

	protected XMLWriter.ProcessingInstruction[] createPI(Class repositoryClass) {
		return new XMLWriter.ProcessingInstruction[] {XMLWriter.ProcessingInstruction.makeClassVersionInstruction(XMLConstants.PI_REPOSITORY_TARGET, repositoryClass, XMLConstants.CURRENT_VERSION)};
	}

	// XML writer for a IMetadataRepository
	protected class Writer extends XMLWriter implements XMLConstants {

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
			// TODO: there is probably a better solution to the problem.
			// TODO: Because the implementation of IMetadataRepository.getInstallableUnits
			//		 in LocalMetadataRepository uses a query, the order of IUs is not preserved
			//		 write after read. FIX THIS!
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

		private void writeInstallableUnits(IInstallableUnit[] installableUnits) {
			if (installableUnits.length > 0) {
				start(INSTALLABLE_UNITS_ELEMENT);
				attribute(COLLECTION_SIZE_ATTRIBUTE, installableUnits.length);
				for (int i = 0; i < installableUnits.length; i++) {
					writeInstallableUnit(installableUnits[i]);
				}
				end(INSTALLABLE_UNITS_ELEMENT);
			}
		}

		private void writeInstallableUnit(IInstallableUnit resolvedIU) {
			IInstallableUnit iu = (!(resolvedIU instanceof IResolvedInstallableUnit) ? resolvedIU//
					: ((IResolvedInstallableUnit) resolvedIU).getOriginal());
			start(INSTALLABLE_UNIT_ELEMENT);
			attribute(ID_ATTRIBUTE, iu.getId());
			attribute(VERSION_ATTRIBUTE, iu.getVersion());
			attribute(SINGLETON_ATTRIBUTE, iu.isSingleton(), true);
			attribute(FRAGMENT_ATTRIBUTE, iu.isFragment(), false);

			if (iu.isFragment() && iu instanceof IInstallableUnitFragment) {
				IInstallableUnitFragment fragment = (IInstallableUnitFragment) iu;
				attribute(FRAGMENT_HOST_ID_ATTRIBUTE, fragment.getHostId());
				attribute(FRAGMENT_HOST_RANGE_ATTRIBUTE, fragment.getHostVersionRange());
			}

			writeProperties(iu.getProperties());
			writeProvidedCapabilities(iu.getProvidedCapabilities());
			writeRequiredCapabilities(iu.getRequiredCapabilities());
			writeTrimmedCdata(IU_FILTER_ELEMENT, iu.getFilter());
			writeTrimmedCdata(APPLICABILITY_FILTER_ELEMENT, iu.getApplicabilityFilter());

			writeArtifactKeys(iu.getArtifacts());
			writeTouchpointType(iu.getTouchpointType());
			writeTouchpointData(iu.getTouchpointData());

			end(INSTALLABLE_UNIT_ELEMENT);
		}

		private void writeProvidedCapabilities(ProvidedCapability[] capabilities) {
			if (capabilities != null && capabilities.length > 0) {
				start(PROVIDED_CAPABILITIES_ELEMENT);
				attribute(COLLECTION_SIZE_ATTRIBUTE, capabilities.length);
				for (int i = 0; i < capabilities.length; i++) {
					start(PROVIDED_CAPABILITY_ELEMENT);
					attribute(NAMESPACE_ATTRIBUTE, capabilities[i].getNamespace());
					attribute(NAME_ATTRIBUTE, capabilities[i].getName());
					attribute(VERSION_ATTRIBUTE, capabilities[i].getVersion());
					end(PROVIDED_CAPABILITY_ELEMENT);
				}
				end(PROVIDED_CAPABILITIES_ELEMENT);
			}
		}

		private void writeRequiredCapabilities(RequiredCapability[] capabilities) {
			if (capabilities != null && capabilities.length > 0) {
				start(REQUIRED_CAPABILITIES_ELEMENT);
				attribute(COLLECTION_SIZE_ATTRIBUTE, capabilities.length);
				for (int i = 0; i < capabilities.length; i++) {
					writeRequiredCapability(capabilities[i]);
				}
				end(REQUIRED_CAPABILITIES_ELEMENT);
			}
		}

		private void writeRequiredCapability(RequiredCapability capability) {
			start(REQUIRED_CAPABILITY_ELEMENT);
			attribute(NAMESPACE_ATTRIBUTE, capability.getNamespace());
			attribute(NAME_ATTRIBUTE, capability.getName());
			attribute(VERSION_RANGE_ATTRIBUTE, capability.getRange());
			attribute(CAPABILITY_OPTIONAL_ATTRIBUTE, capability.isOptional(), false);
			attribute(CAPABILITY_MULTIPLE_ATTRIBUTE, capability.isMultiple(), false);

			writeTrimmedCdata(CAPABILITY_FILTER_ELEMENT, capability.getFilter());

			String[] selectors = capability.getSelectors();
			if (selectors.length > 0) {
				start(CAPABILITY_SELECTORS_ELEMENT);
				attribute(COLLECTION_SIZE_ATTRIBUTE, selectors.length);
				for (int j = 0; j < selectors.length; j++) {
					writeTrimmedCdata(CAPABILITY_SELECTOR_ELEMENT, selectors[j]);
				}
				end(CAPABILITY_SELECTORS_ELEMENT);
			}

			end(REQUIRED_CAPABILITY_ELEMENT);
		}

		private void writeArtifactKeys(IArtifactKey[] artifactKeys) {
			if (artifactKeys != null && artifactKeys.length > 0) {
				start(ARTIFACT_KEYS_ELEMENT);
				attribute(COLLECTION_SIZE_ATTRIBUTE, artifactKeys.length);
				for (int i = 0; i < artifactKeys.length; i++) {
					start(ARTIFACT_KEY_ELEMENT);
					attribute(ARTIFACT_KEY_NAMESPACE_ATTRIBUTE, artifactKeys[i].getNamespace());
					attribute(ARTIFACT_KEY_CLASSIFIER_ATTRIBUTE, artifactKeys[i].getClassifier());
					attribute(ID_ATTRIBUTE, artifactKeys[i].getId());
					attribute(VERSION_ATTRIBUTE, artifactKeys[i].getVersion());
					end(ARTIFACT_KEY_ELEMENT);
				}
				end(ARTIFACT_KEYS_ELEMENT);
			}
		}

		private void writeTouchpointType(TouchpointType touchpointType) {
			start(TOUCHPOINT_TYPE_ELEMENT);
			attribute(ID_ATTRIBUTE, touchpointType.getId());
			attribute(VERSION_ATTRIBUTE, touchpointType.getVersion());
			end(TOUCHPOINT_TYPE_ELEMENT);
		}

		private void writeTouchpointData(TouchpointData[] touchpointData) {
			if (touchpointData != null && touchpointData.length > 0) {
				start(TOUCHPOINT_DATA_ELEMENT);
				attribute(COLLECTION_SIZE_ATTRIBUTE, touchpointData.length);
				for (int i = 0; i < touchpointData.length; i++) {
					TouchpointData nextData = touchpointData[i];
					Map instructions = nextData.getInstructions();
					if (instructions.size() > 0) {
						start(TOUCHPOINT_DATA_INSTRUCTIONS_ELEMENT);
						attribute(COLLECTION_SIZE_ATTRIBUTE, instructions.size());
						for (Iterator iter = instructions.entrySet().iterator(); iter.hasNext();) {
							Map.Entry entry = (Map.Entry) iter.next();
							start(TOUCHPOINT_DATA_INSTRUCTION_ELEMENT);
							attribute(TOUCHPOINT_DATA_INSTRUCTION_KEY_ATTRIBUTE, entry.getKey());
							cdataLines((String) entry.getValue(), true);
							end(TOUCHPOINT_DATA_INSTRUCTION_ELEMENT);
						}
					}
				}
				end(TOUCHPOINT_DATA_ELEMENT);
			}
		}

		private void writeTrimmedCdata(String element, String filter) {
			String trimmed;
			if (filter != null && (trimmed = filter.trim()).length() > 0) {
				start(element);
				cdata(trimmed);
				end(element);
			}
		}
	}

	/*
	 * Parser for the contents of a LocalMetadata,
	 * as written by the Writer class.
	 */
	private class Parser extends XMLParser implements XMLConstants {

		private IMetadataRepository theRepository = null;
		protected Class theRepositoryClass = LocalMetadataRepository.class;

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

		public void ProcessingInstruction(String target, String data) throws SAXException {
			if (PI_REPOSITORY_TARGET.equalsIgnoreCase(target)) {
				// TODO: should the root handler be constructed based on class
				// 		 via an extension registry mechanism?
				String clazz = extractPIClass(data);
				try {
					theRepositoryClass = Class.forName(clazz);
				} catch (ClassNotFoundException e) {
					// throw new SAXException(NLS.bind(Messages.MetadataRepositoryIO_Repository_Class_Not_Found, clazz));
					throw new SAXException("Metadata repository class " + clazz + "not found"); //$NON-NLS-1$//$NON-NLS-2$
				}

				// TODO: version tolerance by extension
				Version repositoryVersion = extractPIVersion(target, data);
				if (!XML_TOLERANCE.isIncluded(repositoryVersion)) {
					throw new SAXException(NLS.bind(Messages.MetadataRepositoryIO_Parser_Has_Incompatible_Version, repositoryVersion, XML_TOLERANCE));
				}
			}
		}

		private final class RepositoryDocHandler extends DocHandler {

			public RepositoryDocHandler(String rootName, RootHandler rootHandler) {
				super(rootName, rootHandler);
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
				state.Location = null; // new URL("file://C:/bogus");				
			}

			public void startElement(String name, Attributes attributes) {
				if (PROPERTIES_ELEMENT.equalsIgnoreCase(name)) {
					if (propertiesHandler == null) {
						propertiesHandler = new PropertiesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (INSTALLABLE_UNITS_ELEMENT.equalsIgnoreCase(name)) {
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
					//					// TODO: need to support URLMetadataRepository as well.
					//					try {
					//						repository = new LocalMetadataRepository(attrValues[0], attrValues[1], attrValues[2], new URL("file://C:/bogus"), attrValues[3], attrValues[4]);
					//					} catch (MalformedURLException e) {
					//						// TODO Auto-generated catch block
					//						e.printStackTrace();
					//					}
					state.Properties = (propertiesHandler == null ? new OrderedProperties(0) //
							: propertiesHandler.getProperties());
					state.Units = (unitsHandler == null ? new IInstallableUnit[0] //
							: unitsHandler.getUnits());
					try {
						Object repositoryObject = theRepositoryClass.newInstance();
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
					}
				}
			}
		}

		protected class InstallableUnitsHandler extends AbstractHandler {

			private ArrayList units;

			public InstallableUnitsHandler(AbstractHandler parentHandler, Attributes attributes) {
				super(parentHandler, INSTALLABLE_UNITS_ELEMENT);
				String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
				units = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
			}

			public IInstallableUnit[] getUnits() {
				return (IInstallableUnit[]) units.toArray(new IInstallableUnit[units.size()]);
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equalsIgnoreCase(INSTALLABLE_UNIT_ELEMENT)) {
					new InstallableUnitHandler(this, attributes, units);
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		protected class InstallableUnitHandler extends AbstractHandler {

			private final String[] required = new String[] {ID_ATTRIBUTE, VERSION_ATTRIBUTE};
			private final String[] optional = new String[] {SINGLETON_ATTRIBUTE, FRAGMENT_ATTRIBUTE, FRAGMENT_HOST_ID_ATTRIBUTE, FRAGMENT_HOST_RANGE_ATTRIBUTE};

			InstallableUnit currentUnit = null;

			private PropertiesHandler propertiesHandler = null;
			private ProvidedCapabilitiesHandler providedCapabilitiesHandler = null;
			private RequiredCapabilitiesHandler requiredCapabilitiesHandler = null;
			private TextHandler filterHandler = null;
			private TextHandler applicabilityHandler = null;
			private ArtifactsHandler artifactsHandler = null;
			private TouchpointTypeHandler touchpointTypeHandler = null;
			private TouchpointDataHandler touchpointDataHandler = null;

			public InstallableUnitHandler(AbstractHandler parentHandler, Attributes attributes, List units) {
				super(parentHandler, INSTALLABLE_UNIT_ELEMENT);
				String[] values = parseAttributes(attributes, required, optional);

				Version version = checkVersion(INSTALLABLE_UNIT_ELEMENT, VERSION_ATTRIBUTE, values[1]);
				boolean singleton = checkBoolean(INSTALLABLE_UNIT_ELEMENT, SINGLETON_ATTRIBUTE, values[2], true).booleanValue();
				boolean isFragment = checkBoolean(INSTALLABLE_UNIT_ELEMENT, FRAGMENT_ATTRIBUTE, values[3], false).booleanValue();
				if (isFragment) {
					// TODO: tooling default fragment does not have a host id
					// checkRequiredAttribute(INSTALLABLE_UNIT_ELEMENT, FRAGMENT_HOST_ID_ATTRIBUTE, values[4]);
					checkRequiredAttribute(INSTALLABLE_UNIT_ELEMENT, FRAGMENT_HOST_RANGE_ATTRIBUTE, values[5]);
					VersionRange hostRange = checkVersionRange(INSTALLABLE_UNIT_ELEMENT, FRAGMENT_HOST_RANGE_ATTRIBUTE, values[5]);
					currentUnit = new InstallableUnitFragment(values[0], version, singleton, values[4], hostRange);
				} else {
					if (values[4] != null) {
						unexpectedAttribute(INSTALLABLE_UNIT_ELEMENT, FRAGMENT_HOST_ID_ATTRIBUTE, values[4]);
					} else if (values[5] != null) {
						unexpectedAttribute(INSTALLABLE_UNIT_ELEMENT, FRAGMENT_HOST_RANGE_ATTRIBUTE, values[4]);
					}
					currentUnit = new InstallableUnit(values[0], version, singleton);
				}
				units.add(currentUnit);
			}

			public IInstallableUnit getInstallableUnit() {
				return currentUnit;
			}

			public void startElement(String name, Attributes attributes) {
				if (PROPERTIES_ELEMENT.equalsIgnoreCase(name)) {
					if (propertiesHandler == null) {
						propertiesHandler = new PropertiesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (PROVIDED_CAPABILITIES_ELEMENT.equalsIgnoreCase(name)) {
					if (providedCapabilitiesHandler == null) {
						providedCapabilitiesHandler = new ProvidedCapabilitiesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (REQUIRED_CAPABILITIES_ELEMENT.equalsIgnoreCase(name)) {
					if (requiredCapabilitiesHandler == null) {
						requiredCapabilitiesHandler = new RequiredCapabilitiesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (IU_FILTER_ELEMENT.equalsIgnoreCase(name)) {
					if (filterHandler == null) {
						filterHandler = new TextHandler(this, IU_FILTER_ELEMENT, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (APPLICABILITY_FILTER_ELEMENT.equalsIgnoreCase(name)) {
					if (applicabilityHandler == null) {
						applicabilityHandler = new TextHandler(this, APPLICABILITY_FILTER_ELEMENT, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (ARTIFACT_KEYS_ELEMENT.equalsIgnoreCase(name)) {
					if (artifactsHandler == null) {
						artifactsHandler = new ArtifactsHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (TOUCHPOINT_TYPE_ELEMENT.equalsIgnoreCase(name)) {
					if (touchpointTypeHandler == null) {
						touchpointTypeHandler = new TouchpointTypeHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (TOUCHPOINT_DATA_ELEMENT.equalsIgnoreCase(name)) {
					if (touchpointDataHandler == null) {
						touchpointDataHandler = new TouchpointDataHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else {
					invalidElement(name, attributes);
				}
			}

			protected void finished() {
				if (isValidXML() && currentUnit != null) {
					OrderedProperties properties = (propertiesHandler == null ? new OrderedProperties(0) //
							: propertiesHandler.getProperties());
					currentUnit.addProperties(properties);
					ProvidedCapability[] providedCapabilities = (providedCapabilitiesHandler == null ? new ProvidedCapability[0] //
							: providedCapabilitiesHandler.getProvidedCapabilities());
					currentUnit.setCapabilities(providedCapabilities);
					RequiredCapability[] requiredCapabilities = (requiredCapabilitiesHandler == null ? new RequiredCapability[0] //
							: requiredCapabilitiesHandler.getRequiredCapabilities());
					currentUnit.setRequiredCapabilities(requiredCapabilities);
					if (filterHandler != null) {
						currentUnit.setFilter(filterHandler.getText());
					}
					if (applicabilityHandler != null) {
						currentUnit.setApplicabilityFilter(applicabilityHandler.getText());
					}
					IArtifactKey[] artifacts = (artifactsHandler == null ? new IArtifactKey[0] //
							: artifactsHandler.getArtifactKeys());
					currentUnit.setArtifacts(artifacts);
					if (touchpointTypeHandler != null) {
						currentUnit.setTouchpointType(touchpointTypeHandler.getTouchpointType());
					} else {
						// TODO: create an error
					}
					TouchpointData[] touchpointData = (touchpointDataHandler == null ? new TouchpointData[0] //
							: touchpointDataHandler.getTouchpointData());
					currentUnit.addTouchpointData(touchpointData);
				}
			}
		}

		protected class ProvidedCapabilitiesHandler extends AbstractHandler {

			private List providedCapabilities;

			public ProvidedCapabilitiesHandler(AbstractHandler parentHandler, Attributes attributes) {
				super(parentHandler, PROVIDED_CAPABILITIES_ELEMENT);
				String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
				providedCapabilities = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
			}

			public ProvidedCapability[] getProvidedCapabilities() {
				return (ProvidedCapability[]) providedCapabilities.toArray(new ProvidedCapability[providedCapabilities.size()]);
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equalsIgnoreCase(PROVIDED_CAPABILITY_ELEMENT)) {
					new ProvidedCapabilityHandler(this, attributes, providedCapabilities);
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		protected class ProvidedCapabilityHandler extends AbstractHandler {

			private final String[] required = new String[] {NAMESPACE_ATTRIBUTE, NAME_ATTRIBUTE, VERSION_ATTRIBUTE};

			public ProvidedCapabilityHandler(AbstractHandler parentHandler, Attributes attributes, List capabilities) {
				super(parentHandler, PROVIDED_CAPABILITY_ELEMENT);
				String[] values = parseRequiredAttributes(attributes, required);
				Version version = checkVersion(PROVIDED_CAPABILITY_ELEMENT, VERSION_ATTRIBUTE, values[2]);
				capabilities.add(new ProvidedCapability(values[0], values[1], version));
			}

			public void startElement(String name, Attributes attributes) {
				invalidElement(name, attributes);
			}
		}

		protected class RequiredCapabilitiesHandler extends AbstractHandler {

			private List requiredCapabilities;

			public RequiredCapabilitiesHandler(AbstractHandler parentHandler, Attributes attributes) {
				super(parentHandler, REQUIRED_CAPABILITIES_ELEMENT);
				String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
				requiredCapabilities = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
			}

			public RequiredCapability[] getRequiredCapabilities() {
				return (RequiredCapability[]) requiredCapabilities.toArray(new RequiredCapability[requiredCapabilities.size()]);
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equalsIgnoreCase(REQUIRED_CAPABILITY_ELEMENT)) {
					new RequiredCapabilityHandler(this, attributes, requiredCapabilities);
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		protected class RequiredCapabilityHandler extends AbstractHandler {

			private final String[] required = new String[] {NAMESPACE_ATTRIBUTE, NAME_ATTRIBUTE, VERSION_RANGE_ATTRIBUTE};
			private final String[] optional = new String[] {CAPABILITY_OPTIONAL_ATTRIBUTE, CAPABILITY_MULTIPLE_ATTRIBUTE};

			private RequiredCapability currentCapability = null;

			private TextHandler filterHandler = null;
			private CapabilitySelectorsHandler selectorsHandler = null;

			public RequiredCapabilityHandler(AbstractHandler parentHandler, Attributes attributes, List capabilities) {
				super(parentHandler, REQUIRED_CAPABILITY_ELEMENT);
				String[] values = parseAttributes(attributes, required, optional);
				VersionRange range = checkVersionRange(REQUIRED_CAPABILITY_ELEMENT, VERSION_RANGE_ATTRIBUTE, values[2]);
				boolean isOptional = checkBoolean(REQUIRED_CAPABILITY_ELEMENT, CAPABILITY_OPTIONAL_ATTRIBUTE, values[3], false).booleanValue();
				boolean isMultiple = checkBoolean(REQUIRED_CAPABILITY_ELEMENT, CAPABILITY_MULTIPLE_ATTRIBUTE, values[4], false).booleanValue();
				currentCapability = new RequiredCapability(values[0], values[1], range, null, isOptional, isMultiple);
				capabilities.add(currentCapability);
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equalsIgnoreCase(CAPABILITY_FILTER_ELEMENT)) {
					filterHandler = new TextHandler(this, CAPABILITY_FILTER_ELEMENT, attributes);
				} else if (name.equalsIgnoreCase(CAPABILITY_SELECTORS_ELEMENT)) {
					selectorsHandler = new CapabilitySelectorsHandler(this, attributes);
				} else {
					invalidElement(name, attributes);
				}
			}

			protected void finished() {
				if (isValidXML()) {
					if (currentCapability != null) {
						if (filterHandler != null) {
							currentCapability.setFilter(filterHandler.getText());
						}
						if (selectorsHandler != null) {
							currentCapability.setSelectors(selectorsHandler.getSelectors());
						}
					}
				}
			}
		}

		protected class ArtifactsHandler extends AbstractHandler {

			private List artifacts;

			public ArtifactsHandler(AbstractHandler parentHandler, Attributes attributes) {
				super(parentHandler, ARTIFACT_KEYS_ELEMENT);
				String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
				artifacts = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
			}

			public IArtifactKey[] getArtifactKeys() {
				return (IArtifactKey[]) artifacts.toArray(new IArtifactKey[artifacts.size()]);
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equalsIgnoreCase(ARTIFACT_KEY_ELEMENT)) {
					new ArtifactHandler(this, attributes, artifacts);
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		protected class ArtifactHandler extends AbstractHandler {

			private final String[] required = new String[] {NAMESPACE_ATTRIBUTE, CLASSIFIER_ATTRIBUTE, ID_ATTRIBUTE, VERSION_ATTRIBUTE};

			public ArtifactHandler(AbstractHandler parentHandler, Attributes attributes, List artifacts) {
				super(parentHandler, ARTIFACT_KEY_ELEMENT);
				String[] values = parseRequiredAttributes(attributes, required);
				Version version = checkVersion(ARTIFACT_KEY_ELEMENT, VERSION_ATTRIBUTE, values[3]);
				artifacts.add(new ArtifactKey(values[0], values[1], values[2], version));
			}

			public void startElement(String name, Attributes attributes) {
				invalidElement(name, attributes);
			}
		}

		protected class CapabilitySelectorsHandler extends AbstractHandler {

			private List selectors;

			public CapabilitySelectorsHandler(AbstractHandler parentHandler, Attributes attributes) {
				super(parentHandler, CAPABILITY_SELECTORS_ELEMENT);
				String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
				selectors = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
			}

			public String[] getSelectors() {
				return (String[]) selectors.toArray(new String[selectors.size()]);
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equalsIgnoreCase(CAPABILITY_SELECTOR_ELEMENT)) {
					new TextHandler(this, CAPABILITY_SELECTOR_ELEMENT, attributes, selectors);
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		protected class TouchpointTypeHandler extends AbstractHandler {

			private final String[] required = new String[] {ID_ATTRIBUTE, VERSION_ATTRIBUTE};

			TouchpointType touchpointType = null;

			public TouchpointTypeHandler(AbstractHandler parentHandler, Attributes attributes) {
				super(parentHandler, TOUCHPOINT_TYPE_ELEMENT);
				String[] values = parseRequiredAttributes(attributes, required);
				Version version = checkVersion(TOUCHPOINT_TYPE_ELEMENT, VERSION_ATTRIBUTE, values[1]);
				touchpointType = new TouchpointType(values[0], version);
			}

			public TouchpointType getTouchpointType() {
				return touchpointType;
			}

			public void startElement(String name, Attributes attributes) {
				invalidElement(name, attributes);
			}
		}

		protected class TouchpointDataHandler extends AbstractHandler {

			TouchpointData touchpointData = null;

			List data = null;

			public TouchpointDataHandler(AbstractHandler parentHandler, Attributes attributes) {
				super(parentHandler, TOUCHPOINT_DATA_ELEMENT);
				String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
				data = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
			}

			public TouchpointData[] getTouchpointData() {
				return (TouchpointData[]) data.toArray(new TouchpointData[data.size()]);
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equalsIgnoreCase(TOUCHPOINT_DATA_INSTRUCTIONS_ELEMENT)) {
					new TouchpointInstructionsHandler(this, attributes, data);
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		protected class TouchpointInstructionsHandler extends AbstractHandler {

			Map instructions = null;

			public TouchpointInstructionsHandler(AbstractHandler parentHandler, Attributes attributes, List data) {
				super(parentHandler, TOUCHPOINT_DATA_INSTRUCTIONS_ELEMENT);
				String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
				instructions = (size != null ? new LinkedHashMap(new Integer(size).intValue()) : new LinkedHashMap(4));
				data.add(new TouchpointData(instructions));
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equalsIgnoreCase(TOUCHPOINT_DATA_INSTRUCTION_ELEMENT)) {
					new TouchpointInstructionHandler(this, attributes, instructions);
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		protected class TouchpointInstructionHandler extends TextHandler {

			private final String[] required = new String[] {TOUCHPOINT_DATA_INSTRUCTION_KEY_ATTRIBUTE};

			Map instructions = null;
			String key = null;

			public TouchpointInstructionHandler(AbstractHandler parentHandler, Attributes attributes, Map instructions) {
				super(parentHandler, TOUCHPOINT_DATA_INSTRUCTION_ELEMENT);
				key = parseRequiredAttributes(attributes, required)[0];
				this.instructions = instructions;
			}

			protected void finished() {
				if (isValidXML()) {
					if (key != null) {
						instructions.put(key, getText());
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
