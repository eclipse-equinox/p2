/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository.simple;

import java.io.*;
import java.net.URI;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.persistence.XMLParser;
import org.eclipse.equinox.internal.p2.persistence.XMLWriter;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.xml.sax.*;

/**
 * This class reads and writes artifact repository metadata
 * (e.g. table of contents files);
 * 
 * This class is not used for reading or writing the actual artifacts.
 */

// TODO: Should a registration/factory mechanism be supported
//		 for getting a repository reader/writer given a repository type
public class SimpleArtifactRepositoryIO {

	private final IProvisioningAgent agent;
	private Location lockLocation = null;

	public SimpleArtifactRepositoryIO(IProvisioningAgent agent) {
		this.agent = agent;
	}

	/**
	 * Writes the given artifact repository to the stream.
	 * This method performs buffering, and closes the stream when finished.
	 */
	public void write(SimpleArtifactRepository repository, OutputStream output) {
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
	 * Reads the artifact repository from the given stream,
	 * and returns the contained array of abstract artifact repositories.
	 * 
	 * This method performs buffering, and closes the stream when finished.
	 */
	public IArtifactRepository read(URI location, InputStream input, IProgressMonitor monitor, boolean acquireLock) throws ProvisionException {
		BufferedInputStream bufferedInput = null;
		try {
			try {
				bufferedInput = new BufferedInputStream(input);
				Parser repositoryParser = new Parser(Activator.getContext(), Activator.ID);
				repositoryParser.setErrorContext(location.toURL().toExternalForm());
				IStatus result = null;
				boolean lock = false;
				try {
					if (canLock(location) && acquireLock) {
						lock = lock(location, true, monitor);
						if (lock) {
							repositoryParser.parse(input);
							result = repositoryParser.getStatus();
						} else {
							result = Status.CANCEL_STATUS;
						}
					} else {
						repositoryParser.parse(input);
						result = repositoryParser.getStatus();
					}
				} finally {
					if (lock)
						unlock(location);
				}

				switch (result.getSeverity()) {
					case IStatus.CANCEL :
						throw new OperationCanceledException();
					case IStatus.ERROR :
						throw new ProvisionException(result);
					case IStatus.WARNING :
					case IStatus.INFO :
						LogHelper.log(result);
				}
				SimpleArtifactRepository repository = repositoryParser.getRepository();
				if (repository == null)
					throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, Messages.io_parseError, null));
				return repository;
			} finally {
				if (bufferedInput != null)
					bufferedInput.close();
			}
		} catch (IOException ioe) {
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, ioe));
		}
	}

	private synchronized boolean canLock(URI repositoryLocation) {
		if (!URIUtil.isFileURI(repositoryLocation))
			return false;

		try {
			lockLocation = getLockLocation(repositoryLocation);
		} catch (IOException e) {
			return false;
		}
		return !lockLocation.isReadOnly();
	}

	private synchronized boolean lock(URI repositoryLocation, boolean wait, IProgressMonitor monitor) throws IOException {
		if (!Activator.getInstance().enableArtifactLocking())
			return true; // Don't use locking

		lockLocation = getLockLocation(repositoryLocation);
		boolean locked = lockLocation.lock();
		if (locked || !wait)
			return locked;

		//Someone else must have the directory locked
		while (true) {
			if (monitor.isCanceled())
				return false;
			try {
				Thread.sleep(200); // 5x per second
			} catch (InterruptedException e) {/*ignore*/
			}
			locked = lockLocation.lock();
			if (locked)
				return true;
		}
	}

	private void unlock(URI repositoryLocation) {
		if (!Activator.getInstance().enableArtifactLocking())
			return;

		if (lockLocation != null) {
			lockLocation.release();
		}
	}

	/**
	 * Returns the location of the lock file.
	 */
	private Location getLockLocation(URI repositoryLocation) throws IOException {
		if (!URIUtil.isFileURI(repositoryLocation)) {
			throw new IOException("Cannot lock a non file based repository"); //$NON-NLS-1$
		}
		return Activator.getInstance().getLockLocation(repositoryLocation);
	}

	private interface XMLConstants extends org.eclipse.equinox.internal.p2.persistence.XMLConstants {

		// Constants defining the structure of the XML for a SimpleArtifactRepository

		// A format version number for simple artifact repository XML.
		public static final Version COMPATIBLE_VERSION = Version.createOSGi(1, 0, 0);
		public static final Version CURRENT_VERSION = Version.createOSGi(1, 1, 0);
		public static final VersionRange XML_TOLERANCE = new VersionRange(COMPATIBLE_VERSION, true, Version.createOSGi(2, 0, 0), false);

		// Constants for processing instructions
		public static final String PI_REPOSITORY_TARGET = "artifactRepository"; //$NON-NLS-1$
		public static XMLWriter.ProcessingInstruction[] PI_DEFAULTS = new XMLWriter.ProcessingInstruction[] {XMLWriter.ProcessingInstruction.makeTargetVersionInstruction(PI_REPOSITORY_TARGET, CURRENT_VERSION)};

		// Constants for artifact repository elements
		public static final String REPOSITORY_ELEMENT = "repository"; //$NON-NLS-1$
		public static final String REPOSITORY_PROPERTIES_ELEMENT = "repositoryProperties"; //$NON-NLS-1$
		public static final String MAPPING_RULES_ELEMENT = "mappings"; //$NON-NLS-1$
		public static final String MAPPING_RULE_ELEMENT = "rule"; //$NON-NLS-1$
		public static final String ARTIFACTS_ELEMENT = "artifacts"; //$NON-NLS-1$
		public static final String ARTIFACT_ELEMENT = "artifact"; //$NON-NLS-1$
		public static final String PROCESSING_STEPS_ELEMENT = "processing"; //$NON-NLS-1$
		public static final String PROCESSING_STEP_ELEMENT = "step"; //$NON-NLS-1$

		public static final String MAPPING_RULE_FILTER_ATTRIBUTE = "filter"; //$NON-NLS-1$
		public static final String MAPPING_RULE_OUTPUT_ATTRIBUTE = "output"; //$NON-NLS-1$

		public static final String ARTIFACT_CLASSIFIER_ATTRIBUTE = CLASSIFIER_ATTRIBUTE;

		public static final String STEP_DATA_ATTRIBUTE = "data"; //$NON-NLS-1$
		public static final String STEP_REQUIRED_ATTRIBUTE = "required"; //$NON-NLS-1$
	}

	// XML writer for a SimpleArtifactRepository
	protected class Writer extends XMLWriter implements XMLConstants {

		public Writer(OutputStream output) throws IOException {
			super(output, PI_DEFAULTS);
		}

		/**
		 * Write the given artifact repository to the output stream.
		 */
		public void write(SimpleArtifactRepository repository) {
			start(REPOSITORY_ELEMENT);
			attribute(NAME_ATTRIBUTE, repository.getName());
			attribute(TYPE_ATTRIBUTE, repository.getType());
			attribute(VERSION_ATTRIBUTE, repository.getVersion());
			attributeOptional(PROVIDER_ATTRIBUTE, repository.getProvider());
			attributeOptional(DESCRIPTION_ATTRIBUTE, repository.getDescription()); // TODO: could be cdata?

			writeProperties(repository.getProperties());
			writeMappingRules(repository.getRules());
			writeArtifacts(repository.getDescriptors());

			end(REPOSITORY_ELEMENT);
			flush();
		}

		private void writeMappingRules(String[][] rules) {
			if (rules.length > 0) {
				start(MAPPING_RULES_ELEMENT);
				attribute(COLLECTION_SIZE_ATTRIBUTE, rules.length);
				for (int i = 0; i < rules.length; i++) {
					start(MAPPING_RULE_ELEMENT);
					attribute(MAPPING_RULE_FILTER_ATTRIBUTE, rules[i][0]);
					attribute(MAPPING_RULE_OUTPUT_ATTRIBUTE, rules[i][1]);
					end(MAPPING_RULE_ELEMENT);
				}
				end(MAPPING_RULES_ELEMENT);
			}
		}

		private void writeArtifacts(Set<SimpleArtifactDescriptor> artifactDescriptors) {
			start(ARTIFACTS_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, artifactDescriptors.size());
			for (SimpleArtifactDescriptor descriptor : artifactDescriptors) {
				IArtifactKey key = descriptor.getArtifactKey();
				start(ARTIFACT_ELEMENT);
				attribute(ARTIFACT_CLASSIFIER_ATTRIBUTE, key.getClassifier());
				attribute(ID_ATTRIBUTE, key.getId());
				attribute(VERSION_ATTRIBUTE, key.getVersion());
				writeProcessingSteps(descriptor.getProcessingSteps());
				writeProperties(descriptor.getProperties());
				writeProperties(REPOSITORY_PROPERTIES_ELEMENT, descriptor.getRepositoryProperties());
				end(ARTIFACT_ELEMENT);
			}
			end(ARTIFACTS_ELEMENT);
		}

		private void writeProcessingSteps(IProcessingStepDescriptor[] processingSteps) {
			if (processingSteps.length > 0) {
				start(PROCESSING_STEPS_ELEMENT);
				attribute(COLLECTION_SIZE_ATTRIBUTE, processingSteps.length);
				for (int i = 0; i < processingSteps.length; i++) {
					start(PROCESSING_STEP_ELEMENT);
					attribute(ID_ATTRIBUTE, processingSteps[i].getProcessorId());
					attribute(STEP_DATA_ATTRIBUTE, processingSteps[i].getData());
					attribute(STEP_REQUIRED_ATTRIBUTE, processingSteps[i].isRequired());
					end(PROCESSING_STEP_ELEMENT);
				}
				end(PROCESSING_STEPS_ELEMENT);
			}
		}
	}

	/*
	 * Parser for the contents of a SimpleArtifactRepository,
	 * as written by the Writer class.
	 */
	private class Parser extends XMLParser implements XMLConstants {

		private SimpleArtifactRepository theRepository = null;

		public Parser(BundleContext context, String bundleId) {
			super(context, bundleId);
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

		public SimpleArtifactRepository getRepository() {
			return theRepository;
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
					// TODO: should the root handler be constructed based on class
					// 		 via an extension registry mechanism?
					// String clazz = extractPIClass(data);
					// TODO: version tolerance by extension
					Version repositoryVersion = extractPIVersion(target, data);
					if (!XML_TOLERANCE.isIncluded(repositoryVersion)) {
						throw new SAXException(NLS.bind(Messages.io_incompatibleVersion, repositoryVersion, XML_TOLERANCE));
					}
				}
			}

		}

		private final class RepositoryHandler extends RootHandler {

			private final String[] required = new String[] {NAME_ATTRIBUTE, TYPE_ATTRIBUTE, VERSION_ATTRIBUTE};
			private final String[] optional = new String[] {DESCRIPTION_ATTRIBUTE, PROVIDER_ATTRIBUTE};

			private String[] attrValues = new String[required.length + optional.length];

			private MappingRulesHandler mappingRulesHandler = null;
			private PropertiesHandler propertiesHandler = null;
			private ArtifactsHandler artifactsHandler = null;

			private SimpleArtifactRepository repository = null;

			public RepositoryHandler() {
				super();
			}

			public SimpleArtifactRepository getRepository() {
				return repository;
			}

			protected void handleRootAttributes(Attributes attributes) {
				attrValues = parseAttributes(attributes, required, optional);
				attrValues[2] = checkVersion(REPOSITORY_ELEMENT, VERSION_ATTRIBUTE, attrValues[2]).toString();
			}

			public void startElement(String name, Attributes attributes) {
				if (MAPPING_RULES_ELEMENT.equals(name)) {
					if (mappingRulesHandler == null) {
						mappingRulesHandler = new MappingRulesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (ARTIFACTS_ELEMENT.equals(name)) {
					if (artifactsHandler == null) {
						artifactsHandler = new ArtifactsHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (PROPERTIES_ELEMENT.equals(name)) {
					if (propertiesHandler == null) {
						propertiesHandler = new PropertiesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else {
					invalidElement(name, attributes);
				}
			}

			protected void finished() {
				if (isValidXML()) {
					String[][] mappingRules = (mappingRulesHandler == null ? new String[0][0] //
							: mappingRulesHandler.getMappingRules());
					Map<String, String> properties = (propertiesHandler == null ? new OrderedProperties(0) //
							: propertiesHandler.getProperties());
					Set<SimpleArtifactDescriptor> artifacts = (artifactsHandler == null ? new HashSet<SimpleArtifactDescriptor>(0) //
							: artifactsHandler.getArtifacts());
					repository = new SimpleArtifactRepository(agent, attrValues[0], attrValues[1], attrValues[2], attrValues[3], //
							attrValues[4], artifacts, mappingRules, properties);
				}
			}
		}

		protected class MappingRulesHandler extends AbstractHandler {

			private List<String[]> mappingRules;

			public MappingRulesHandler(AbstractHandler parentHandler, Attributes attributes) {
				super(parentHandler, MAPPING_RULES_ELEMENT);
				String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
				mappingRules = (size != null ? new ArrayList<String[]>(Integer.parseInt(size)) : new ArrayList<String[]>(4));
			}

			public String[][] getMappingRules() {
				String[][] rules = new String[mappingRules.size()][2];
				for (int index = 0; index < mappingRules.size(); index++) {
					String[] ruleAttributes = mappingRules.get(index);
					rules[index] = ruleAttributes;
				}
				return rules;
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equals(MAPPING_RULE_ELEMENT)) {
					new MappingRuleHandler(this, attributes, mappingRules);
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		protected class MappingRuleHandler extends AbstractHandler {

			private final String[] required = new String[] {MAPPING_RULE_FILTER_ATTRIBUTE, MAPPING_RULE_OUTPUT_ATTRIBUTE};

			public MappingRuleHandler(AbstractHandler parentHandler, Attributes attributes, List<String[]> mappingRules) {
				super(parentHandler, MAPPING_RULE_ELEMENT);
				mappingRules.add(parseRequiredAttributes(attributes, required));
			}

			public void startElement(String name, Attributes attributes) {
				invalidElement(name, attributes);
			}
		}

		protected class ArtifactsHandler extends AbstractHandler {

			private Set<SimpleArtifactDescriptor> artifacts;

			public ArtifactsHandler(AbstractHandler parentHandler, Attributes attributes) {
				super(parentHandler, ARTIFACTS_ELEMENT);
				String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
				artifacts = (size != null ? new LinkedHashSet<SimpleArtifactDescriptor>(Integer.parseInt(size)) : new LinkedHashSet<SimpleArtifactDescriptor>(4));
			}

			public Set<SimpleArtifactDescriptor> getArtifacts() {
				return artifacts;
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equals(ARTIFACT_ELEMENT)) {
					new ArtifactHandler(this, attributes, artifacts);
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		protected class ArtifactHandler extends AbstractHandler {

			private final String[] required = new String[] {ARTIFACT_CLASSIFIER_ATTRIBUTE, ID_ATTRIBUTE, VERSION_ATTRIBUTE};

			private Set<SimpleArtifactDescriptor> artifacts;
			SimpleArtifactDescriptor currentArtifact = null;

			private PropertiesHandler propertiesHandler = null;
			private PropertiesHandler repositoryPropertiesHandler = null;
			private ProcessingStepsHandler processingStepsHandler = null;

			public ArtifactHandler(AbstractHandler parentHandler, Attributes attributes, Set<SimpleArtifactDescriptor> artifacts) {
				super(parentHandler, ARTIFACT_ELEMENT);
				this.artifacts = artifacts;
				String[] values = parseRequiredAttributes(attributes, required);
				Version version = checkVersion(ARTIFACT_ELEMENT, VERSION_ATTRIBUTE, values[2]);
				// TODO: resolve access restriction on ArtifactKey construction
				currentArtifact = new SimpleArtifactDescriptor(new ArtifactKey(values[0], values[1], version));
			}

			public void startElement(String name, Attributes attributes) {
				if (PROCESSING_STEPS_ELEMENT.equals(name)) {
					if (processingStepsHandler == null) {
						processingStepsHandler = new ProcessingStepsHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (PROPERTIES_ELEMENT.equals(name)) {
					if (propertiesHandler == null) {
						propertiesHandler = new PropertiesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (REPOSITORY_PROPERTIES_ELEMENT.equals(name)) {
					if (repositoryPropertiesHandler == null) {
						repositoryPropertiesHandler = new PropertiesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else {
					invalidElement(name, attributes);
				}
			}

			protected void finished() {
				if (isValidXML() && currentArtifact != null) {
					Map<String, String> properties = (propertiesHandler == null ? new OrderedProperties(0) : propertiesHandler.getProperties());
					currentArtifact.addProperties(properties);

					properties = (repositoryPropertiesHandler == null ? new OrderedProperties(0) : repositoryPropertiesHandler.getProperties());
					currentArtifact.addRepositoryProperties(properties);

					IProcessingStepDescriptor[] processingSteps = (processingStepsHandler == null ? new ProcessingStepDescriptor[0] //
							: processingStepsHandler.getProcessingSteps());
					currentArtifact.setProcessingSteps(processingSteps);
					artifacts.add(currentArtifact);
				}
			}
		}

		protected class ProcessingStepsHandler extends AbstractHandler {

			private List<IProcessingStepDescriptor> processingSteps;

			public ProcessingStepsHandler(AbstractHandler parentHandler, Attributes attributes) {
				super(parentHandler, PROCESSING_STEPS_ELEMENT);
				String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
				processingSteps = (size != null ? new ArrayList<IProcessingStepDescriptor>(Integer.parseInt(size)) : new ArrayList<IProcessingStepDescriptor>(4));
			}

			public IProcessingStepDescriptor[] getProcessingSteps() {
				return processingSteps.toArray(new ProcessingStepDescriptor[processingSteps.size()]);
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equals(PROCESSING_STEP_ELEMENT)) {
					new ProcessingStepHandler(this, attributes, processingSteps);
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		protected class ProcessingStepHandler extends AbstractHandler {

			private final String[] required = new String[] {ID_ATTRIBUTE, STEP_REQUIRED_ATTRIBUTE};
			private final String[] optional = new String[] {STEP_DATA_ATTRIBUTE};

			public ProcessingStepHandler(AbstractHandler parentHandler, Attributes attributes, List<IProcessingStepDescriptor> processingSteps) {
				super(parentHandler, PROCESSING_STEP_ELEMENT);
				String[] attributeValues = parseAttributes(attributes, required, optional);
				processingSteps.add(new ProcessingStepDescriptor(attributeValues[0], attributeValues[2], checkBoolean(PROCESSING_STEP_ELEMENT, STEP_REQUIRED_ATTRIBUTE, attributeValues[1]).booleanValue()));
			}

			public void startElement(String name, Attributes attributes) {
				invalidElement(name, attributes);
			}
		}

		protected String getErrorMessage() {
			return Messages.io_parseError;
		}

		public String toString() {
			// TODO:
			return null;
		}

	}

}
