/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.update.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

/**	
 * 	This class provides a wrapper for reading and writing platform.xml.
 * 
 * 	Only a minimal set of operations is exposed.
 */
public class PlatformConfigurationWrapper {

	private Configuration configuration = null;
	private Site poolSite = null;
	private File configFile;
	private URL poolURL;
	private URL osgiInstallArea;

	private static String FEATURES = "features/"; //$NON-NLS-1$

	public PlatformConfigurationWrapper(URL configDir, URL featurePool, URL osgiInstallArea) {
		this.configuration = null;
		this.configFile = new File(configDir.getFile(), "/org.eclipse.update/platform.xml"); //$NON-NLS-1$
		this.poolURL = featurePool;
		this.osgiInstallArea = osgiInstallArea;
	}

	private void loadDelegate() {
		if (configuration != null)
			return;

		try {
			if (configFile.exists()) {
				configuration = Configuration.load(configFile, osgiInstallArea);
			} else {
				configuration = new Configuration();
			}
		} catch (ProvisionException pe) {
			// TODO: Make this a real message
			throw new IllegalStateException("Error parsing platform configuration."); //$NON-NLS-1$;
		}

		List sites = configuration.getSites();
		for (Iterator iter = sites.iterator(); iter.hasNext();) {
			Site nextSite = (Site) iter.next();
			String nextURL = nextSite.getUrl();
			if (new Path(nextURL).equals(new Path(poolURL.toExternalForm()))) {
				poolSite = nextSite;
				break;
			}
		}

		if (poolSite == null) {
			poolSite = new Site();
			poolSite.setUrl(poolURL.toExternalForm());
			poolSite.setPolicy(Site.POLICY_MANAGED_ONLY);
			poolSite.setEnabled(true);
			configuration.add(poolSite);
		}
	}

	/*
	 * @see org.eclipse.update.configurator.IPlatformConfiguration#createFeatureEntry(java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean, java.lang.String, java.net.URL[])
	 */
	public IStatus addFeatureEntry(String id, String version, String pluginIdentifier, String pluginVersion, boolean primary, String application, URL[] root) {
		loadDelegate();
		if (configuration == null)
			return new Status(IStatus.WARNING, Activator.ID, "Platform configuration not available.", null); //$NON-NLS-1$

		Feature addedFeature = new Feature(poolSite);
		addedFeature.setId(id);
		addedFeature.setVersion(version);
		addedFeature.setUrl(makeFeatureURL(id, version));
		poolSite.addFeature(addedFeature);
		return Status.OK_STATUS;
	}

	/*
	 * @see org.eclipse.update.configurator.IPlatformConfiguration#findConfiguredFeatureEntry(java.lang.String)
	 */
	public IStatus removeFeatureEntry(String id, String version) {
		loadDelegate();
		if (configuration == null)
			return new Status(IStatus.WARNING, Activator.ID, "Platform configuration not available.", null); //$NON-NLS-1$

		Feature removedFeature = poolSite.removeFeature(makeFeatureURL(id, version));
		return (removedFeature != null ? Status.OK_STATUS : new Status(IStatus.ERROR, Activator.ID, "A feature with the specified id was not found.", null)); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.update.configurator.IPlatformConfiguration#save()
	 */
	public void save(URL newOsgiInstallArea) throws ProvisionException {
		if (configuration != null) {
			configFile.getParentFile().mkdirs();
			configuration.save(configFile, newOsgiInstallArea);
		}
	}

	private static String makeFeatureURL(String id, String version) {
		return FEATURES + id + "_" + version + "/"; //$NON-NLS-1$ //$NON-NLS-2$;
	}

	//	}

}
