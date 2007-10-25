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

import com.thoughtworks.xstream.XStream;
import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitConstants;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class InstallRegistry implements IInstallRegistry {
	private static String STORAGE = "installRegistry.xml";

	// what is installed in each profile
	private Map profileRegistries = new HashMap(); // Profile id -> ProfileInstallRegistry
	//	private ProfileRegistry profileRegistry; // the corresponding ProfileRegistry
	//	private File location; // xml file containing install registry
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
				Location agent = (Location) ServiceHelper.getService(EngineActivator.getContext(), AgentLocation.class.getName());
				if (agent == null)
					// TODO should likely do something here since we failed to persist.
					return;
				if (!agent.getURL().getProtocol().equals("file"))
					throw new IOException("can't write at the given location");

				File outputFile = new File(agent.getURL().getFile(), STORAGE);
				if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs())
					throw new RuntimeException("can't persist profile registry");
				bof = new BufferedOutputStream(new FileOutputStream(outputFile, false));
				new XStream().toXML(profileRegistries, bof);
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
				profileRegistries = (HashMap) new XStream().fromXML(bif);
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

	/**
	 * Install registry for a single profile.
	 */
	public class ProfileInstallRegistry implements IProfileInstallRegistry {
		private String profileId; // id profile this data applies to
		private Set installableUnits; //id 
		private Map iuPropertiesMap; // iu->OrderedProperties

		ProfileInstallRegistry(String profileId) {
			this.profileId = profileId;
			this.installableUnits = new HashSet();
			this.iuPropertiesMap = new HashMap();
		}

		public IInstallableUnit[] getInstallableUnits() {
			IInstallableUnit[] result = new IInstallableUnit[installableUnits.size()];
			return (IInstallableUnit[]) installableUnits.toArray(result);
		}

		public void addInstallableUnits(IInstallableUnit toAdd) {
			installableUnits.add(toAdd);
		}

		public void removeInstallableUnits(IInstallableUnit toRemove) {
			installableUnits.remove(toRemove);
			iuPropertiesMap.remove(toRemove);
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
			OrderedProperties properties = (OrderedProperties) iuPropertiesMap.get(toGet);
			if (properties == null) {
				properties = new OrderedProperties();
				iuPropertiesMap.put(toGet, properties);
			}
			return properties;
		}

	}
}
