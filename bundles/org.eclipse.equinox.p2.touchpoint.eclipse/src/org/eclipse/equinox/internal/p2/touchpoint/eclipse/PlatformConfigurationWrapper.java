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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.update.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
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
	private Manipulator manipulator;

	private static String FEATURES = "features/"; //$NON-NLS-1$

	private static URL getOSGiInstallArea(Manipulator manipulator) {
		final String OSGI = "org.eclipse.osgi"; //$NON-NLS-1$
		BundleInfo[] bis = manipulator.getConfigData().getBundles();
		String searchFor = "org.eclipse.equinox.launcher"; //$NON-NLS-1$
		for (int i = 0; i < bis.length; i++) {
			if (bis[i].getSymbolicName().equals(searchFor)) {
				if (bis[i].getLocation() != null) {
					try {
						if (bis[i].getLocation().startsWith("file:")) //$NON-NLS-1$
							return fromOSGiJarToOSGiInstallArea(bis[i].getLocation().substring(5)).toURL();
					} catch (MalformedURLException e) {
						//do nothing
					}
				}
				if (searchFor.equals(OSGI))
					return null;
				searchFor = OSGI;
				i = -1;
			}
		}
		return null;
	}

	private static File fromOSGiJarToOSGiInstallArea(String path) {
		IPath parentFolder = new Path(path).removeLastSegments(1);
		if (parentFolder.lastSegment().equals("plugins")) //$NON-NLS-1$
			return parentFolder.removeLastSegments(1).toFile();
		return parentFolder.toFile();
	}

	public PlatformConfigurationWrapper(URL configDir, URL featurePool, Manipulator manipulator) {
		this.configuration = null;
		this.configFile = new File(configDir.getFile(), "/org.eclipse.update/platform.xml"); //$NON-NLS-1$
		this.poolURL = featurePool;
		this.manipulator = manipulator;
	}

	private void loadDelegate() {
		if (configuration != null)
			return;

		try {
			if (configFile.exists()) {
				configuration = Configuration.load(configFile, getOSGiInstallArea(manipulator));
			} else {
				configuration = new Configuration();
			}
		} catch (ProvisionException pe) {
			// TODO: Make this a real message
			throw new IllegalStateException("Error parsing platform configuration."); //$NON-NLS-1$;
		}

		poolSite = getSite(poolURL);
		if (poolSite == null) {
			poolSite = createSite(poolURL);
			configuration.add(poolSite);
		}
	}

	/*
	 * Create and return a site object based on the given location.
	 */
	private Site createSite(URL location) {
		Site result = new Site();
		result.setUrl(location.toExternalForm());
		result.setPolicy(Site.POLICY_MANAGED_ONLY);
		result.setEnabled(true);
		return result;
	}

	/*
	 * Look in the configuration and return the site object whose location matches
	 * the given URL. Return null if there is no match.
	 */
	private Site getSite(URL url) {
		List sites = configuration.getSites();
		for (Iterator iter = sites.iterator(); iter.hasNext();) {
			Site nextSite = (Site) iter.next();
			String nextURL = nextSite.getUrl();
			if (new Path(nextURL).equals(new Path(url.toExternalForm()))) {
				return nextSite;
			}
		}
		return null;
	}

	/*
	 * Look in the configuration and return the site which contains the feature
	 * with the given identifier and version. Return null if there is none.
	 */
	private Site getSite(String id, String version) {
		List sites = configuration.getSites();
		for (Iterator iter = sites.iterator(); iter.hasNext();) {
			Site site = (Site) iter.next();
			Feature[] features = site.getFeatures();
			for (int i = 0; i < features.length; i++) {
				if (id.equals(features[i].getId()) && version.equals(features[i].getVersion()))
					return site;
			}
		}
		return null;
	}

	/*
	 * @see org.eclipse.update.configurator.IPlatformConfiguration#createFeatureEntry(java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean, java.lang.String, java.net.URL[])
	 */
	public IStatus addFeatureEntry(File file, String id, String version, String pluginIdentifier, String pluginVersion, boolean primary, String application, URL[] root) {
		loadDelegate();
		if (configuration == null)
			return new Status(IStatus.WARNING, Activator.ID, "Platform configuration not available.", null); //$NON-NLS-1$

		URL fileURL = null;
		try {
			File featureDir = file.getParentFile();
			if (featureDir == null || !featureDir.getName().equals("features"))
				return new Status(IStatus.ERROR, Activator.ID, "Parent directory should be \"features\": " + file.getAbsolutePath(), null);
			File locationDir = featureDir.getParentFile();
			if (locationDir == null)
				return new Status(IStatus.ERROR, Activator.ID, "Unable to calculate extension location for: " + file.getAbsolutePath(), null);

			fileURL = locationDir.toURL();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new Status(IStatus.ERROR, Activator.ID, "Unable to create URL from file: " + file.getAbsolutePath(), null);
		}
		Site site = getSite(fileURL);
		if (site == null) {
			site = createSite(fileURL);
			configuration.add(site);
		}
		Feature addedFeature = new Feature(site);
		addedFeature.setId(id);
		addedFeature.setVersion(version);
		addedFeature.setUrl(makeFeatureURL(id, version));
		site.addFeature(addedFeature);
		return Status.OK_STATUS;
	}

	/*
	 * @see org.eclipse.update.configurator.IPlatformConfiguration#findConfiguredFeatureEntry(java.lang.String)
	 */
	public IStatus removeFeatureEntry(String id, String version) {
		loadDelegate();
		if (configuration == null)
			return new Status(IStatus.WARNING, Activator.ID, "Platform configuration not available.", null); //$NON-NLS-1$

		Site site = getSite(id, version);
		if (site == null)
			site = poolSite;
		Feature removedFeature = site.removeFeature(makeFeatureURL(id, version));
		return (removedFeature != null ? Status.OK_STATUS : new Status(IStatus.ERROR, Activator.ID, "A feature with the specified id was not found.", null)); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.update.configurator.IPlatformConfiguration#save()
	 */
	public void save() throws ProvisionException {
		if (configuration != null) {
			configFile.getParentFile().mkdirs();
			configuration.save(configFile, getOSGiInstallArea(manipulator));
		}
	}

	private static String makeFeatureURL(String id, String version) {
		return FEATURES + id + "_" + version + "/"; //$NON-NLS-1$ //$NON-NLS-2$;
	}

	//	}

}
