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
package org.eclipse.equinox.internal.p2.installregistry;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.p2.engine.Messages;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataParser;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataWriter;
import org.eclipse.equinox.internal.p2.persistence.XMLWriter;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.xml.sax.*;

public class InstallRegistry implements IInstallRegistry {
	class IUIdentity {

		String id;
		Version version;

		public IUIdentity(IInstallableUnit iu) {
			this(iu.getId(), iu.getVersion());
		}

		public IUIdentity(String id, Version version) {
			this.id = (id != null ? id : ""); //$NON-NLS-1$
			this.version = (version != null ? version : Version.emptyVersion);
		}

		public boolean equals(Object obj) {
			final IUIdentity objAsIdentity = //
			(obj instanceof IUIdentity ? (IUIdentity) obj : null);
			if (objAsIdentity != null) {
				return this.id.equals(objAsIdentity.id) && this.version.equals(objAsIdentity.version);
			}
			return false;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((version == null) ? 0 : version.hashCode());
			return result;
		}

		public String toString() {
			return id + ' ' + version;
		}
	}

	/*
	 * 	Parser for the contents of an InstallRegistry,
	 * 	as written by the Writer class.
	 */
	private class Parser extends MetadataParser implements XMLConstants {

		private final class InstallRegistryDocHandler extends DocHandler {

			public InstallRegistryDocHandler(String rootName, RootHandler rootHandler) {
				super(rootName, rootHandler);
			}

			public void ProcessingInstruction(String target, String data) throws SAXException {
				if (PI_REPOSITORY_TARGET.equals(target)) {
					// TODO: should the root handler be constructed based on class
					// 		 or via an extension registry mechanism?
					// String clazz = extractPIClass(data);
					// and
					// TODO: version tolerance by extension or by class?
					Version repositoryVersion = extractPIVersion(target, data);
					if (!XMLConstants.XML_TOLERANCE.isIncluded(repositoryVersion)) {
						throw new SAXException(NLS.bind(Messages.InstallRegistry_Parser_Has_Incompatible_Version, repositoryVersion, XMLConstants.XML_TOLERANCE));
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

			public Map getProfileInstallRegistries() {
				return (profyleRegistries != null ? profyleRegistries : new LinkedHashMap(0));
			}

			protected void handleRootAttributes(Attributes attributes) {
				parseRequiredAttributes(attributes, noAttributes);
			}

			public void startElement(String name, Attributes attributes) {
				if (PROFILE_INSTALL_REGISTRIES_ELEMENT.equals(name)) {
					if (profilesHandler == null) {
						profilesHandler = new ProfileInstallRegistriesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		protected class IUPropertiesHandler extends AbstractHandler {

			private IUIdentity iuIdentity = null;

			private Map iusPropertiesMap = null;
			private PropertiesHandler propertiesHandler = null;
			private final String[] required = new String[] {ID_ATTRIBUTE, VERSION_ATTRIBUTE};

			public IUPropertiesHandler(AbstractHandler parentHandler, Attributes attributes, Map iusPropertiesMap) {
				super(parentHandler, IU_PROPERTIES_ELEMENT);
				String values[] = parseRequiredAttributes(attributes, required);
				Version version = checkVersion(IU_PROPERTIES_ELEMENT, VERSION_ATTRIBUTE, values[1]);
				iuIdentity = new IUIdentity(values[0], version);
				this.iusPropertiesMap = iusPropertiesMap;
			}

			protected void finished() {
				if (isValidXML() && iuIdentity != null && propertiesHandler != null) {
					iusPropertiesMap.put(iuIdentity, propertiesHandler.getProperties());
				}
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equals(PROPERTIES_ELEMENT)) {
					propertiesHandler = new PropertiesHandler(this, attributes);
				} else {
					invalidElement(name, attributes);
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
				if (name.equals(IU_PROPERTIES_ELEMENT)) {
					new IUPropertiesHandler(this, attributes, iusPropertiesMap);
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		protected class ProfileInstallRegistriesHandler extends AbstractHandler {

			private List registries = null;

			public ProfileInstallRegistriesHandler(AbstractHandler parentHandler, Attributes attributes) {
				super(parentHandler, PROFILE_INSTALL_REGISTRIES_ELEMENT);
				String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
				registries = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
			}

			public ProfileInstallRegistry[] getProfileInstallRegistries() {
				return (ProfileInstallRegistry[]) registries.toArray(new ProfileInstallRegistry[registries.size()]);
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equals(PROFILE_INSTALL_REGISTRY_ELEMENT)) {
					new ProfileInstallRegistryHandler(this, attributes, registries);
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		protected class ProfileInstallRegistryHandler extends AbstractHandler {

			private IUsPropertiesHandler iusPropertiesHandler = null;

			private String profileId = null;

			List registries = null;

			private final String[] required = new String[] {PROFILE_ID_ATTRIBUTE};
			private InstallableUnitsHandler unitsHandler = null;

			public ProfileInstallRegistryHandler(AbstractHandler parentHandler, Attributes attributes, List registries) {
				super(parentHandler, PROFILE_INSTALL_REGISTRY_ELEMENT);
				profileId = parseRequiredAttributes(attributes, required)[0];
				this.registries = registries;
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

			public void startElement(String name, Attributes attributes) {
				if (INSTALLABLE_UNITS_ELEMENT.equals(name)) {
					if (unitsHandler == null) {
						unitsHandler = new InstallableUnitsHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (IUS_PROPERTIES_ELEMENT.equals(name)) {
					if (iusPropertiesHandler == null) {
						iusPropertiesHandler = new IUsPropertiesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		private Map profileInstallRegistries = null;

		public Parser(BundleContext context, String bundleId) {
			super(context, bundleId);
		}

		protected String getErrorMessage() {
			return Messages.InstallRegistry_Parser_Error_Parsing_Registry;
		}

		public Map getProfileInstallRegistries() {
			return profileInstallRegistries;
		}

		protected Object getRootObject() {
			return profileInstallRegistries;
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

		public String toString() {
			// TODO:
			return null;
		}

	}

	/**
	 * Install registry for a single profile.
	 */
	public class ProfileInstallRegistry implements IProfileInstallRegistry {
		Set installableUnits; //id 
		private Map iuPropertiesMap; // iu->OrderedProperties
		private String profileId; // id profile this data applies to

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

		public void addInstallableUnits(IInstallableUnit toAdd) {
			installableUnits.add(toAdd);
		}

		public void addInstallableUnits(IInstallableUnit[] toAdd) {
			for (int i = 0; i < toAdd.length; i++) {
				installableUnits.add(toAdd[i]);
			}
		}

		public IInstallableUnit getInstallableUnit(String id, String version) {
			for (Iterator i = installableUnits.iterator(); i.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) i.next();
				if (iu.getId().equals(id) && iu.getVersion().equals(new Version(version)))
					return iu;
			}
			return null;
		}

		private OrderedProperties getInstallableUnitProfileProperties(IInstallableUnit toGet) {
			OrderedProperties properties = (OrderedProperties) iuPropertiesMap.get(new IUIdentity(toGet));
			if (properties == null) {
				properties = new OrderedProperties();
				iuPropertiesMap.put(new IUIdentity(toGet), properties);
			}
			return properties;
		}

		public String getInstallableUnitProfileProperty(IInstallableUnit toGet, String key) {
			OrderedProperties properties = getInstallableUnitProfileProperties(toGet);
			return properties.getProperty(key);
		}

		public IInstallableUnit[] getInstallableUnits() {
			IInstallableUnit[] result = new IInstallableUnit[installableUnits.size()];
			return (IInstallableUnit[]) installableUnits.toArray(result);
		}

		public Map getIUIdentityToPropertiesMap() {
			return Collections.unmodifiableMap(iuPropertiesMap);
		}

		public String getProfileId() {
			return profileId;
		}

		public void removeInstallableUnits(IInstallableUnit toRemove) {
			installableUnits.remove(toRemove);
			iuPropertiesMap.remove(new IUIdentity(toRemove));
		}

		public String setInstallableUnitProfileProperty(IInstallableUnit toSet, String key, String value) {
			OrderedProperties properties = getInstallableUnitProfileProperties(toSet);
			return (String) properties.setProperty(key, value);
		}
	}

	protected class Writer extends MetadataWriter implements XMLConstants {

		public Writer(OutputStream output) throws IOException {
			super(output, PI_DEFAULTS);
		}

		/**
		 * Write the given artifact repository to the output stream.
		 */
		public void write(InstallRegistry istregistryry) {
			start(INSTALL_REGISTRY_ELEMENT);
			writeProfileRegistries(istregistryry.internalGetRegistryMap());
			end(INSTALL_REGISTRY_ELEMENT);
			flush();
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

		private void writeProfileRegistries(Map registries) {
			if (registries.size() > 0) {
				start(PROFILE_INSTALL_REGISTRIES_ELEMENT);
				attribute(COLLECTION_SIZE_ATTRIBUTE, registries.size());
				for (Iterator iter = registries.keySet().iterator(); iter.hasNext();) {
					String nextProfileId = (String) iter.next();
					ProfileInstallRegistry nextProfileRegistry = (ProfileInstallRegistry) registries.get(nextProfileId);
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
	}

	private interface XMLConstants extends org.eclipse.equinox.internal.p2.persistence.XMLConstants {

		// Constants defining the structure of the XML for a SimpleProfileRegistry

		// A format version number for install registry XML.
		public static final Version CURRENT_VERSION = new Version(0, 0, 1);
		public static final VersionRange XML_TOLERANCE = new VersionRange(CURRENT_VERSION, true, new Version(2, 0, 0), false);
		// Constants for install registry elements
		public static final String INSTALL_REGISTRY_ELEMENT = "installRegistry"; //$NON-NLS-1$
		public static final String IU_PROPERTIES_ELEMENT = "iusProperties"; //$NON-NLS-1$
		public static final String IUS_PROPERTIES_ELEMENT = "iusPropertiesMap"; //$NON-NLS-1$
		public static final String PROFILE_INSTALL_REGISTRIES_ELEMENT = "profiles"; //$NON-NLS-1$
		public static final String PROFILE_INSTALL_REGISTRY_ELEMENT = "profile"; //$NON-NLS-1$

		// Constants for processing instructions
		public static final String PI_REPOSITORY_TARGET = "installRegistry"; //$NON-NLS-1$
		public static XMLWriter.ProcessingInstruction[] PI_DEFAULTS = new XMLWriter.ProcessingInstruction[] {XMLWriter.ProcessingInstruction.makeClassVersionInstruction(PI_REPOSITORY_TARGET, SimpleProfileRegistry.class, CURRENT_VERSION)};
		// Constants for attributes of an profile install registry element
		public static final String PROFILE_ID_ATTRIBUTE = "profileId"; //$NON-NLS-1$

	}

	private static String STORAGE = "installRegistry.xml"; //$NON-NLS-1$

	private transient ProvisioningEventBus bus;
	private transient ServiceReference busReference;

	/**
	 * What is installed in each profile. A map of String(Profile id) -> ProfileInstallRegistry.
	 * Most callers should use getRegistryMap() accessor method that does lazy initialization.
	 */
	Map profileRegistries = null;

	public InstallRegistry() {
		busReference = EngineActivator.getContext().getServiceReference(ProvisioningEventBus.class.getName());
		bus = (ProvisioningEventBus) EngineActivator.getContext().getService(busReference);
		bus.addListener(new SynchronousProvisioningListener() {
			public void notify(EventObject o) {
				if (o instanceof InstallableUnitEvent) {
					InstallableUnitEvent event = (InstallableUnitEvent) o;
					if (event.isPre() || !event.getResult().isOK())
						return;
					IProfileInstallRegistry registry = getProfileInstallRegistry(event.getProfile());
					if (event.isInstall() && event.getOperand().second() != null) {
						registry.addInstallableUnits(event.getOperand().second().unresolved());
					} else if (event.isUninstall() && event.getOperand().first() != null) {
						IInstallableUnit original = event.getOperand().first().unresolved();
						String value = registry.getInstallableUnitProfileProperty(original, IInstallableUnit.PROP_PROFILE_ROOT_IU);
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
							registry.setInstallableUnitProfileProperty(event.getOperand().second().unresolved(), IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
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
						getRegistryMap().remove(pe.getProfile().getProfileId());
						persist();
					} else if (pe.getReason() == ProfileEvent.CHANGED) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=197701
						persist();
					}
				}
			}
		});
	}

	public synchronized Collection getProfileInstallRegistries() {
		return getRegistryMap().values();
	}

	public synchronized IProfileInstallRegistry getProfileInstallRegistry(Profile profile) {
		String profileId = profile.getProfileId();
		Map registry = getRegistryMap();
		IProfileInstallRegistry result = (IProfileInstallRegistry) registry.get(profileId);
		if (result == null) {
			result = new ProfileInstallRegistry(profileId);
			registry.put(profileId, result);
		}
		return result;
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

	/**
	 * Returns the registry map with lazy initialization. Never returns null.
	 */
	Map getRegistryMap() {
		Map map = internalGetRegistryMap();
		if (map != null)
			return map;
		map = restore();
		setRegistryMap(map);
		return map;
	}

	/**
	 * Returns the registry map without lazy initialization. May return null.
	 */
	Map internalGetRegistryMap() {
		return profileRegistries;
	}

	synchronized void persist() {
		//if we haven't restored, there is nothing to persist
		if (profileRegistries == null)
			return;
		long time = 0;
		final String debugMsg = "Saving install registry"; //$NON-NLS-1$
		if (Tracing.DEBUG_INSTALL_REGISTRY) {
			Tracing.debug(debugMsg);
			time = -System.currentTimeMillis();
		}
		try {
			BufferedOutputStream bof = null;
			try {
				URL registryLocation = getRegistryLocation();
				if (!registryLocation.getProtocol().equals("file")) //$NON-NLS-1$
					throw new IOException("Can't write install registry at: " + registryLocation); //$NON-NLS-1$
				File outputFile = new File(registryLocation.toExternalForm().substring(5));
				if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs())
					throw new IOException("Can't persist profile registry"); //$NON-NLS-1$
				bof = new BufferedOutputStream(new FileOutputStream(outputFile, false));
				Writer writer = new Writer(bof);
				writer.write(this);
			} finally {
				if (bof != null)
					bof.close();
			}
			if (Tracing.DEBUG_INSTALL_REGISTRY) {
				time += System.currentTimeMillis();
				Tracing.debug(debugMsg + " time (ms): " + time); //$NON-NLS-1$ 
			}
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, EngineActivator.ID, debugMsg, e));
		}
	}

	synchronized Map restore() {
		long time = 0;
		final String debugMsg = "Restoring install registry"; //$NON-NLS-1$
		if (Tracing.DEBUG_INSTALL_REGISTRY) {
			Tracing.debug(debugMsg);
			time = -System.currentTimeMillis();
		}
		Map registryMap = null;
		try {
			BufferedInputStream bif = null;
			try {
				bif = new BufferedInputStream(getRegistryLocation().openStream());
				Parser parser = new Parser(EngineActivator.getContext(), EngineActivator.ID);
				parser.parse(bif);
				IStatus result = parser.getStatus();
				if (!result.isOK())
					LogHelper.log(result);
				registryMap = parser.getProfileInstallRegistries();
			} finally {
				if (bif != null)
					bif.close();
			}
			if (Tracing.DEBUG_INSTALL_REGISTRY) {
				time += System.currentTimeMillis();
				Tracing.debug(debugMsg + " time (ms): " + time); //$NON-NLS-1$ 
			}
		} catch (FileNotFoundException e) {
			//This is ok.
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, EngineActivator.ID, debugMsg, e));
		}
		if (registryMap == null)
			registryMap = new LinkedHashMap();
		return registryMap;
	}

	private void setRegistryMap(Map profileInstallRegistries) {
		profileRegistries = profileInstallRegistries;
	}

}
