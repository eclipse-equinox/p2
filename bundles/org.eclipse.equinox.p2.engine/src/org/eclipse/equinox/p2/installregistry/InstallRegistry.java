/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.installregistry;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.p2.engine.Messages;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.p2.core.helpers.*;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.xml.sax.*;

public class InstallRegistry implements IInstallRegistry {
	private static String STORAGE = "installRegistry.xml"; //$NON-NLS-1$

	// what is installed in each profile
	private Map profileRegistries = new LinkedHashMap(); // Profile id -> ProfileInstallRegistry
	//	private ProfileRegistry profileRegistry; // the corresponding ProfileRegistry
	//	private File location; // XML file containing install registry
	//	private IRepository metadataRepo;
	//	private final MetadataCache installedMetadata = new MetadataCache(
	//            new RepositoryGroup("InstallRegistry"), //$NON-NLS-1$
	//            MetadataCache.POLICY_NONE);

	private transient ServiceReference busReference;
	private transient ProvisioningEventBus bus;

	public InstallRegistry() {
		busReference = EngineActivator.getContext().getServiceReference(ProvisioningEventBus.class.getName());
		bus = (ProvisioningEventBus) EngineActivator.getContext().getService(busReference);
		restore();
		bus.addListener(new SynchronousProvisioningListener() {
			public void notify(EventObject o) {
				if (o instanceof InstallableUnitEvent) {
					InstallableUnitEvent event = (InstallableUnitEvent) o;
					if (event.isPre() || !event.getResult().isOK())
						return;
					IProfileInstallRegistry registry = getProfileInstallRegistry(event.getProfile());
					if (event.isInstall() && event.getOperand().second() != null) {
						registry.addInstallableUnits(event.getOperand().second().getOriginal());
					} else if (event.isUninstall() && event.getOperand().first() != null) {
						IInstallableUnit original = event.getOperand().first().getOriginal();
						String value = registry.getInstallableUnitProfileProperty(original, IInstallableUnitConstants.PROFILE_ROOT_IU);
						boolean isRoot = value != null && value.equals(Boolean.toString(true));
						registry.removeInstallableUnits(original);
						// TODO this is odd because I'm setting up a property for something
						// not yet installed in the registry.  The implementation allows it and
						// the assumption is that the second operand will get installed or else 
						// this change will never be committed.  The alternative is to remember
						// a transitory root value that we set when the install is received.
						// The ideal solution is that this is handled in a profile delta by
						// the engine.
						// https://bugs.eclipse.org/bugs/show_bug.cgi?id=206077 
						if (isRoot && event.getOperand().second() != null) {
							registry.setInstallableUnitProfileProperty(event.getOperand().second().getOriginal(), IInstallableUnitConstants.PROFILE_ROOT_IU, Boolean.toString(true));
						}
					}
				} else if (o instanceof CommitOperationEvent) {
					persist();
					return;
				} else if (o instanceof RollbackOperationEvent) {
					restore();
					return;
				} else if (o instanceof ProfileEvent) {
					ProfileEvent pe = (ProfileEvent) o;
					if (pe.getReason() == ProfileEvent.REMOVED) {
						profileRegistries.remove(pe.getProfile().getProfileId());
						persist();
					} else if (pe.getReason() == ProfileEvent.CHANGED) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=197701
						persist();
					}
				}
			}
		});
	}

	private void persist() {
		try {
			BufferedOutputStream bof = null;
			try {
				URL registryLocation = getRegistryLocation();
				if (!registryLocation.getProtocol().equals("file")) //$NON-NLS-1$
					throw new IOException("Can't write install registry at: " + registryLocation);
				File outputFile = new File(registryLocation.toExternalForm().substring(5));
				if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs())
					throw new RuntimeException("Can't persist profile registry");
				bof = new BufferedOutputStream(new FileOutputStream(outputFile, false));
				//new XStream().toXML(profileRegistries, bof);
				Writer writer = new Writer(bof);
				writer.write(this);
			} finally {
				if (bof != null)
					bof.close();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void restore() {
		try {
			BufferedInputStream bif = null;
			try {
				Location agent = (Location) ServiceHelper.getService(EngineActivator.getContext(), AgentLocation.class.getName());
				if (agent == null)
					// TODO should likely do something here since we failed to restore.
					return;
				bif = new BufferedInputStream(new URL(agent.getURL(), STORAGE).openStream());
				// profileRegistries = (HashMap) new XStream().fromXML(bif);
				Parser parser = new Parser(EngineActivator.getContext(), EngineActivator.ID);
				parser.parse(bif);
				profileRegistries = parser.getProfileInstallRegistries();
			} finally {
				if (bif != null)
					bif.close();
			}
		} catch (FileNotFoundException e) {
			//This is ok.
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public IProfileInstallRegistry getProfileInstallRegistry(Profile profile) {
		String profileId = profile.getProfileId();
		IProfileInstallRegistry result = (IProfileInstallRegistry) this.profileRegistries.get(profileId);
		if (result == null) {
			result = new ProfileInstallRegistry(profileId);
			this.profileRegistries.put(profileId, result);
		}
		return result;
	}

	public Collection getProfileInstallRegistries() {
		return this.profileRegistries.values();
	}

	public InstallRegistry getInstallRegistry() {
		return this;
	}

	private URL getRegistryLocation() {
		AgentLocation agent = (AgentLocation) ServiceHelper.getService(EngineActivator.getContext(), AgentLocation.class.getName());
		try {
			return new URL(agent.getDataArea(EngineActivator.ID), STORAGE);
		} catch (MalformedURLException e) {
			//this is not possible because we know the above URL is valid
		}
		return null;
	}

	protected class IUIdentity {

		private String id;
		private Version version;

		public IUIdentity(String id, Version version) {
			this.id = (id != null ? id : ""); //$NON-NLS-1$
			this.version = (version != null ? version : Version.emptyVersion);
		}

		public IUIdentity(IInstallableUnit iu) {
			this(iu.getId(), iu.getVersion());
		}

		public String toString() {
			return id + ' ' + version;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((version == null) ? 0 : version.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			final IUIdentity objAsIdentity = //
			(obj instanceof IUIdentity ? (IUIdentity) obj : null);
			if (objAsIdentity != null) {
				return this.id.equals(objAsIdentity.id) && this.version.equals(objAsIdentity.version);
			}
			return false;
		}
	}

	/**
	 * Install registry for a single profile.
	 */
	public class ProfileInstallRegistry implements IProfileInstallRegistry {
		private String profileId; // id profile this data applies to
		private Set installableUnits; //id 
		private Map iuPropertiesMap; // iu->OrderedProperties

		public ProfileInstallRegistry(String profileId) {
			this.profileId = profileId;
			this.installableUnits = new LinkedHashSet();
			this.iuPropertiesMap = new LinkedHashMap();
		}

		protected ProfileInstallRegistry(String profileId, IInstallableUnit[] units, Map iuPropertiesMap) {
			this.profileId = profileId;
			this.installableUnits = new LinkedHashSet(units.length);
			this.iuPropertiesMap = new LinkedHashMap(iuPropertiesMap.size());
			addInstallableUnits(units);
			this.iuPropertiesMap.putAll(iuPropertiesMap);
		}

		public IInstallableUnit[] getInstallableUnits() {
			IInstallableUnit[] result = new IInstallableUnit[installableUnits.size()];
			return (IInstallableUnit[]) installableUnits.toArray(result);
		}

		public void addInstallableUnits(IInstallableUnit toAdd) {
			installableUnits.add(toAdd);
		}

		public void addInstallableUnits(IInstallableUnit[] toAdd) {
			for (int i = 0; i < toAdd.length; i++) {
				installableUnits.add(toAdd[i]);
			}
		}

		public void removeInstallableUnits(IInstallableUnit toRemove) {
			installableUnits.remove(toRemove);
			iuPropertiesMap.remove(new IUIdentity(toRemove));
		}

		public String getProfileId() {
			return profileId;
		}

		public IInstallableUnit getInstallableUnit(String id, String version) {
			for (Iterator i = installableUnits.iterator(); i.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) i.next();
				if (iu.getId().equals(id) && iu.getVersion().equals(new Version(version)))
					return iu;
			}
			return null;
		}

		public String getInstallableUnitProfileProperty(IInstallableUnit toGet, String key) {
			OrderedProperties properties = getInstallableUnitProfileProperties(toGet);
			return properties.getProperty(key);
		}

		public String setInstallableUnitProfileProperty(IInstallableUnit toSet, String key, String value) {
			OrderedProperties properties = getInstallableUnitProfileProperties(toSet);
			return (String) properties.setProperty(key, value);
		}

		private OrderedProperties getInstallableUnitProfileProperties(IInstallableUnit toGet) {
			OrderedProperties properties = (OrderedProperties) iuPropertiesMap.get(new IUIdentity(toGet));
			if (properties == null) {
				properties = new OrderedProperties();
				iuPropertiesMap.put(new IUIdentity(toGet), properties);
			}
			return properties;
		}

		public Map getIUIdentityToPropertiesMap() {
			return Collections.unmodifiableMap(iuPropertiesMap);
		}
	}

	private interface XMLConstants extends org.eclipse.equinox.p2.core.helpers.XMLConstants {

		// Constants defining the structure of the XML for a SimpleProfileRegistry

		// A format version number for install registry XML.
		public static final String XML_VERSION = "0.0.1"; //$NON-NLS-1$
		public static final Version CURRENT_VERSION = new Version(XML_VERSION);
		public static final VersionRange XML_TOLERANCE = new VersionRange(CURRENT_VERSION, true, CURRENT_VERSION, true);

		// Constants for processing instructions
		public static final String PI_REPOSITORY_TARGET = "installRegistry"; //$NON-NLS-1$
		public static XMLWriter.ProcessingInstruction[] PI_DEFAULTS = new XMLWriter.ProcessingInstruction[] {XMLWriter.ProcessingInstruction.makeClassVersionInstruction(PI_REPOSITORY_TARGET, SimpleProfileRegistry.class, CURRENT_VERSION)};

		// Constants for install registry elements
		public static final String INSTALL_REGISTRY_ELEMENT = "installRegistry"; //$NON-NLS-1$
		public static final String PROFILE_INSTALL_REGISTRIES_ELEMENT = "profiles"; //$NON-NLS-1$
		public static final String PROFILE_INSTALL_REGISTRY_ELEMENT = "profile"; //$NON-NLS-1$
		public static final String INSTALLABLE_UNITS_ELEMENT = "units"; //$NON-NLS-1$
		public static final String INSTALLABLE_UNIT_ELEMENT = "unit"; //$NON-NLS-1$
		public static final String IUS_PROPERTIES_ELEMENT = "iusPropertiesMap"; //$NON-NLS-1$
		public static final String IU_PROPERTIES_ELEMENT = "iusProperties"; //$NON-NLS-1$

		// Constants for attributes of an profile install registry element
		public static final String PROFILE_ID_ATTRIBUTE = "profileId"; //$NON-NLS-1$

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

	protected class Writer extends XMLWriter implements XMLConstants {

		public Writer(OutputStream output) throws IOException {
			super(output, PI_DEFAULTS);
		}

		/**
		 * Write the given artifact repository to the output stream.
		 */
		public void write(InstallRegistry istregistryry) {
			start(INSTALL_REGISTRY_ELEMENT);
			writeProfileRegistries(istregistryry.profileRegistries);
			end(INSTALL_REGISTRY_ELEMENT);
			flush();
		}

		private void writeProfileRegistries(Map profileRegistries) {
			if (profileRegistries.size() > 0) {
				start(PROFILE_INSTALL_REGISTRIES_ELEMENT);
				attribute(COLLECTION_SIZE_ATTRIBUTE, profileRegistries.size());
				for (Iterator iter = profileRegistries.keySet().iterator(); iter.hasNext();) {
					String nextProfileId = (String) iter.next();
					ProfileInstallRegistry nextProfileRegistry = (ProfileInstallRegistry) profileRegistries.get(nextProfileId);
					writeProfileRegistry(nextProfileId, nextProfileRegistry);
				}
				end(PROFILE_INSTALL_REGISTRIES_ELEMENT);
			}
		}

		private void writeProfileRegistry(String profileId, ProfileInstallRegistry profileRegistry) {
			start(PROFILE_INSTALL_REGISTRY_ELEMENT);
			attribute(PROFILE_ID_ATTRIBUTE, profileId);
			Set ius = profileRegistry.installableUnits;
			writeInstallableUnits((IInstallableUnit[]) ius.toArray(new IInstallableUnit[ius.size()]));
			writeIUPropertyMap(profileRegistry.getIUIdentityToPropertiesMap());
			end(PROFILE_INSTALL_REGISTRY_ELEMENT);
		}

		private void writeIUPropertyMap(Map iuPropertiesMap) {
			if (iuPropertiesMap.size() > 0) {
				start(IUS_PROPERTIES_ELEMENT);
				attribute(COLLECTION_SIZE_ATTRIBUTE, iuPropertiesMap.size());
				for (Iterator iter = iuPropertiesMap.keySet().iterator(); iter.hasNext();) {
					IUIdentity nextIdentity = (IUIdentity) iter.next();
					OrderedProperties properties = (OrderedProperties) iuPropertiesMap.get(nextIdentity);
					start(IU_PROPERTIES_ELEMENT);
					attribute(ID_ATTRIBUTE, nextIdentity.id);
					attribute(VERSION_ATTRIBUTE, nextIdentity.version);
					writeProperties(properties);
					end(IU_PROPERTIES_ELEMENT);
				}
				end(IUS_PROPERTIES_ELEMENT);
			}
		}

		// TODO: below is a LOT of code duplicated for MetadataRepositoryIO for writing
		//		 installable units. Need to figure out a cleanway to declare handlers
		//		 which do not depend on the context of an outer declaring Parser class,
		//		 so that handlers may be reused in different parsing contexts.
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
							cdata((String) entry.getValue(), true);
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
	 * 	Parser for the contents of an InstallRegistry,
	 * 	as written by the Writer class.
	 */
	private class Parser extends XMLParser implements XMLConstants {

		private Map profileInstallRegistries = null;

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
				InstallRegistryHandler registryHandler = new InstallRegistryHandler();
				xmlReader.setContentHandler(new InstallRegistryDocHandler(INSTALL_REGISTRY_ELEMENT, registryHandler));
				xmlReader.parse(new InputSource(stream));
				if (this.isValidXML()) {
					profileInstallRegistries = registryHandler.getProfileInstallRegistries();
				}
			} catch (SAXException e) {
				throw new IOException(e.getMessage());
			} catch (ParserConfigurationException e) {
				throw new IOException(e.getMessage());
			} finally {
				stream.close();
			}
		}

		public Map getProfileInstallRegistries() {
			return profileInstallRegistries;
		}

		protected Object getRootObject() {
			return profileInstallRegistries;
		}

		private final class InstallRegistryDocHandler extends DocHandler {

			public InstallRegistryDocHandler(String rootName, RootHandler rootHandler) {
				super(rootName, rootHandler);
			}

			public void ProcessingInstruction(String target, String data) throws SAXException {
				if (PI_REPOSITORY_TARGET.equalsIgnoreCase(target)) {
					// TODO: should the root handler be constructed based on class
					// 		 via an extension registry mechanism?
					// String clazz = extractPIClass(data);
					// and
					// TODO: version tolerance by extension
					Version repositoryVersion = extractPIVersion(target, data);
					if (!XML_TOLERANCE.isIncluded(repositoryVersion)) {
						throw new SAXException(NLS.bind(Messages.InstallRegistry_Parser_Has_Incompatible_Version, repositoryVersion, XML_TOLERANCE));
					}
				}
			}
		}

		private final class InstallRegistryHandler extends RootHandler {

			private ProfileInstallRegistriesHandler profilesHandler = null;

			private Map profyleRegistries = null;

			public InstallRegistryHandler() {
				super();
			}

			protected void handleRootAttributes(Attributes attributes) {
				parseRequiredAttributes(attributes, noAttributes);
			}

			public void startElement(String name, Attributes attributes) {
				if (PROFILE_INSTALL_REGISTRIES_ELEMENT.equalsIgnoreCase(name)) {
					if (profilesHandler == null) {
						profilesHandler = new ProfileInstallRegistriesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else {
					invalidElement(name, attributes);
				}
			}

			public Map getProfileInstallRegistries() {
				return (profyleRegistries != null ? profyleRegistries : new LinkedHashMap(0));
			}

			protected void finished() {
				if (isValidXML()) {
					ProfileInstallRegistry[] registries = (profilesHandler == null ? new ProfileInstallRegistry[0] //
							: profilesHandler.getProfileInstallRegistries());
					profyleRegistries = new LinkedHashMap(registries.length);
					for (int i = 0; i < registries.length; i++) {
						ProfileInstallRegistry nextProfileRegistry = registries[i];
						profyleRegistries.put(nextProfileRegistry.getProfileId(), nextProfileRegistry);
					}
				}
			}
		}

		protected class ProfileInstallRegistriesHandler extends AbstractHandler {

			private List profileRegistries = null;

			public ProfileInstallRegistriesHandler(AbstractHandler parentHandler, Attributes attributes) {
				super(parentHandler, PROFILE_INSTALL_REGISTRIES_ELEMENT);
				String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
				profileRegistries = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equalsIgnoreCase(PROFILE_INSTALL_REGISTRY_ELEMENT)) {
					new ProfileInstallRegistryHandler(this, attributes, profileRegistries);
				} else {
					invalidElement(name, attributes);
				}
			}

			public ProfileInstallRegistry[] getProfileInstallRegistries() {
				return (ProfileInstallRegistry[]) profileRegistries.toArray(new ProfileInstallRegistry[profileRegistries.size()]);
			}
		}

		protected class ProfileInstallRegistryHandler extends AbstractHandler {

			private final String[] required = new String[] {PROFILE_ID_ATTRIBUTE};

			List registries = null;

			private String profileId = null;

			private InstallableUnitsHandler unitsHandler = null;
			private IUsPropertiesHandler iusPropertiesHandler = null;

			public ProfileInstallRegistryHandler(AbstractHandler parentHandler, Attributes attributes, List registries) {
				super(parentHandler, PROFILE_INSTALL_REGISTRY_ELEMENT);
				profileId = parseRequiredAttributes(attributes, required)[0];
				this.registries = registries;
			}

			public void startElement(String name, Attributes attributes) {
				if (INSTALLABLE_UNITS_ELEMENT.equalsIgnoreCase(name)) {
					if (unitsHandler == null) {
						unitsHandler = new InstallableUnitsHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (IUS_PROPERTIES_ELEMENT.equalsIgnoreCase(name)) {
					if (iusPropertiesHandler == null) {
						iusPropertiesHandler = new IUsPropertiesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else {
					invalidElement(name, attributes);
				}
			}

			protected void finished() {
				if (isValidXML() && profileId != null) {
					IInstallableUnit[] units = (unitsHandler == null ? new IInstallableUnit[0] //
							: unitsHandler.getUnits());
					Map iusPropertiesMap = (iusPropertiesHandler == null ? new LinkedHashMap() //
							: iusPropertiesHandler.getIUsPropertiesMap());
					ProfileInstallRegistry registry = new ProfileInstallRegistry(profileId, units, iusPropertiesMap);
					registries.add(registry);
				}
			}
		}

		protected class IUsPropertiesHandler extends AbstractHandler {

			private Map iusPropertiesMap;

			public IUsPropertiesHandler(AbstractHandler parentHandler, Attributes attributes) {
				super(parentHandler, IUS_PROPERTIES_ELEMENT);
				String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
				iusPropertiesMap = (size != null ? new LinkedHashMap(new Integer(size).intValue()) : new LinkedHashMap(4));
			}

			public Map getIUsPropertiesMap() {
				return iusPropertiesMap;
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equalsIgnoreCase(IU_PROPERTIES_ELEMENT)) {
					new IUPropertiesHandler(this, attributes, iusPropertiesMap);
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		protected class IUPropertiesHandler extends AbstractHandler {

			private final String[] required = new String[] {ID_ATTRIBUTE, VERSION_ATTRIBUTE};

			private IUIdentity iuIdentity = null;
			private PropertiesHandler propertiesHandler = null;
			private Map iusPropertiesMap = null;

			public IUPropertiesHandler(AbstractHandler parentHandler, Attributes attributes, Map iusPropertiesMap) {
				super(parentHandler, IU_PROPERTIES_ELEMENT);
				String values[] = parseRequiredAttributes(attributes, required);
				Version version = checkVersion(IU_PROPERTIES_ELEMENT, VERSION_ATTRIBUTE, values[1]);
				iuIdentity = new IUIdentity(values[0], version);
				this.iusPropertiesMap = iusPropertiesMap;
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equalsIgnoreCase(PROPERTIES_ELEMENT)) {
					propertiesHandler = new PropertiesHandler(this, attributes);
				} else {
					invalidElement(name, attributes);
				}
			}

			protected void finished() {
				if (isValidXML() && iuIdentity != null && propertiesHandler != null) {
					iusPropertiesMap.put(iuIdentity, propertiesHandler.getProperties());
				}
			}
		}

		// TODO: below is a LOT of code duplicated for MetadataRepositoryIO for writing
		//		 installable units. Need to figure out a cleanway to declare handlers
		//		 which do not depend on the context of an outer declaring Parser class,
		//		 so that handlers may be reused in different parsing contexts.
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
			return Messages.InstallRegistry_Parser_Error_Parsing_Registry;
		}

		public String toString() {
			// TODO:
			return null;
		}

	}

}
