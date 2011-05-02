/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.update;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Activator;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.p2.core.ProvisionException;

/**
 * @since 1.0
 */
public class Configuration {

	private List<Site> sites = new ArrayList<Site>();
	String date;
	boolean transientProperty;
	String version;
	String shared_ur;

	public static Configuration load(File location, URL osgiInstallArea) throws ProvisionException {
		return ConfigurationIO.read(location, osgiInstallArea);
	}

	public Configuration() {
		super();
	}

	public void save(File location, URL osgiInstallArea) throws ProvisionException {
		ConfigurationIO.write(location, this, osgiInstallArea);
	}

	public String getSharedUR() {
		return shared_ur;
	}

	public void setSharedUR(String value) {
		shared_ur = value;
	}

	public List<Site> getSites() {
		return internalGetSites(true);
	}

	List<Site> internalGetSites(boolean includeParent) {
		if (!includeParent)
			return sites;
		String shared = getSharedUR();
		if (shared == null)
			return sites;
		List<Site> result = new ArrayList<Site>(sites);
		try {
			URL url = new URL(shared);
			File location = URLUtil.toFile(url);
			if (location == null)
				return result;

			if (!location.isAbsolute()) {
				File eclipseHome = Util.getEclipseHome();
				if (eclipseHome == null)
					return null;

				location = new File(eclipseHome, location.getPath());
			}
			Configuration parent = Configuration.load(location, Util.getOSGiInstallArea());
			if (parent == null)
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Unable to load parent configuration from: " + location)); //$NON-NLS-1$
			else
				result.addAll(parent.getSites());
		} catch (MalformedURLException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while getting parent configuration location.", e)); //$NON-NLS-1$
		} catch (ProvisionException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while loading parent configuratin from: " + shared, e)); //$NON-NLS-1$
		}
		return result;
	}

	public void add(Site site) {
		sites.add(site);
	}

	public boolean removeSite(Site site) {
		return sites.remove(site);
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setVersion(String value) {
		version = value;
	}

	public String getVersion() {
		return version;
	}

	public void setTransient(boolean value) {
		transientProperty = value;
	}

	public boolean isTransient() {
		return transientProperty;
	}
}
