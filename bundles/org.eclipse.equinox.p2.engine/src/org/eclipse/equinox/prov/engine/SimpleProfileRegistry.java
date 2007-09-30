/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.prov.engine;

import com.thoughtworks.xstream.XStream;
import java.io.*;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Properties;
import org.eclipse.equinox.internal.prov.engine.EngineActivator;
import org.eclipse.equinox.internal.prov.engine.Messages;
import org.eclipse.equinox.prov.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.prov.core.helpers.ServiceHelper;
import org.eclipse.equinox.prov.core.location.AgentLocation;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;

public class SimpleProfileRegistry implements IProfileRegistry {
	private static String STORAGE = "profileRegistry.xml"; //$NON-NLS-1$

	/**
	 * Map of String(Profile id)->Profile. 
	 */
	private LinkedHashMap profiles = new LinkedHashMap(8);

	private Properties properties = new Properties();

	private String self;

	public SimpleProfileRegistry() {
		self = EngineActivator.getContext().getProperty("eclipse.prov.profile"); //$NON-NLS-1$
		restore();
		updateRoamingProfile();
	}

	/**
	 * If the current profile for self is marked as a roaming profile, we need
	 * to update its install and bundle pool locations.
	 */
	private void updateRoamingProfile() {
		Profile selfProfile = getProfile(SELF);
		if (selfProfile == null)
			return;
		//only update if self is a roaming profile
		if (!Boolean.valueOf(selfProfile.getValue(Profile.PROP_ROAMING)).booleanValue())
			return;
		Location installLocation = (Location) ServiceHelper.getService(EngineActivator.getContext(), Location.class.getName(), Location.INSTALL_FILTER);
		String locationString = installLocation.getURL().getPath();
		boolean changed = false;
		if (!locationString.equals(selfProfile.getValue(Profile.PROP_INSTALL_FOLDER))) {
			selfProfile.setValue(Profile.PROP_INSTALL_FOLDER, locationString);
			changed = true;
		}
		if (!locationString.equals(selfProfile.getValue(Profile.PROP_CACHE))) {
			selfProfile.setValue(Profile.PROP_CACHE, locationString);
			changed = true;
		}
		if (changed)
			persist();
	}

	public String toString() {
		return this.profiles.toString();
	}

	public Profile getProfile(String id) {
		if (SELF.equals(id))
			id = self;
		return (Profile) profiles.get(id);
	}

	public Profile[] getProfiles() {
		return (Profile[]) profiles.values().toArray(new Profile[profiles.size()]);
	}

	public void addProfile(Profile toAdd) throws IllegalArgumentException {
		if (isNamedSelf(toAdd))
			throw new IllegalArgumentException(NLS.bind(Messages.Profile_Not_Named_Self, toAdd.getProfileId()));
		String id = toAdd.getProfileId();
		if (getProfile(id) == null) {
			profiles.put(id, toAdd);
		} else
			throw new IllegalArgumentException(NLS.bind(Messages.Profile_Duplicate_Profile_Id, id));
		broadcastChangeEvent(toAdd, ProfileEvent.ADDED);
		persist(); //TODO This is not enough to keep track of the changes that are being done in a profile. This will likely have to be based on some event like commit
	}

	private void broadcastChangeEvent(Profile profile, byte reason) {
		((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new ProfileEvent(profile, reason));
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
				XStream xml = new XStream();
				Object[] read = (Object[]) xml.fromXML(bif);
				properties = (Properties) read[0];
				profiles = (LinkedHashMap) read[1];
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

	private void persist() {
		OutputStream os;
		try {
			Location agent = (Location) ServiceHelper.getService(EngineActivator.getContext(), AgentLocation.class.getName());
			if (agent == null)
				// TODO should likely do something here since we failed to persist.
				return;
			if (!agent.getURL().getProtocol().equals("file"))
				throw new IOException("can't write at the given location");

			File outputFile = new File(agent.getURL().getFile(), STORAGE);
			if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs())
				throw new RuntimeException("can't persist profile registry at: " + outputFile);
			os = new BufferedOutputStream(new FileOutputStream(outputFile));
			try {
				XStream xstream = new XStream();
				xstream.toXML(new Object[] {properties, profiles}, os);
			} finally {
				os.close();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void removeProfile(Profile toRemove) {
		if (isNamedSelf(toRemove))
			throw new IllegalArgumentException(NLS.bind(Messages.Profile_Not_Named_Self, toRemove.getProfileId()));
		if (profiles.remove(toRemove.getProfileId()) == null)
			return;
		broadcastChangeEvent(toRemove, ProfileEvent.REMOVED);
		persist();
	}

	private boolean isNamedSelf(Profile p) {
		if (SELF.equals(p.getParentProfile()))
			return true;
		return false;
	}

	public Properties getProperties() {
		return properties;
	}

	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	public void removeProperty(String key) {
		properties.remove(key);
	}

}
