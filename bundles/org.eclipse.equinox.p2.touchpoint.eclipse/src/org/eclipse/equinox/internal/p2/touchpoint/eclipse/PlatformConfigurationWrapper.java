/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Pascal Rapicault - Support for bundled macosx http://bugs.eclipse.org/57349
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import java.net.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.update.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.util.NLS;

/**	
 * 	This class provides a wrapper for reading and writing platform.xml.
 * 
 * 	Only a minimal set of operations is exposed.
 */
public class PlatformConfigurationWrapper {

	private Configuration configuration = null;
	private Site poolSite = null;
	private File configFile;
	private URI poolURI;
	private Manipulator manipulator;

	private static String FEATURES = "features/"; //$NON-NLS-1$

	/*
	 * Use the given manipulator to calculate the OSGi install location. We can't
	 * just use the Location service here because we may not be installing into
	 * ourselves. (see https://bugs.eclipse.org/354552)
	 * 
	 * First try and calculate the location based relative to the data provided
	 * in the manipulator's launcher data. If that doesn't work then calculate
	 * it based on the location of known JARs. If that still doesn't work then
	 * return null.
	 */
	private static URL getOSGiInstallArea(Manipulator manipulator) {

		// first see if the launcher home is set
		LauncherData launcherData = manipulator.getLauncherData();
		File home = launcherData.getHome();
		if (home != null) {
			try {
				return home.toURI().toURL();
			} catch (MalformedURLException e) {
				// ignore - shouldn't happen
			}
		}

		// next try and calculate the value based on the location of the framework (OSGi) jar.
		File fwkJar = launcherData.getFwJar();
		if (fwkJar != null) {
			try {
				return fromOSGiJarToOSGiInstallArea(fwkJar.getAbsolutePath()).toURI().toURL();
			} catch (MalformedURLException e) {
				// ignore - shouldn't happen
			}
		}

		// finally calculate the value based on the location of the launcher executable itself
		File launcherFile = launcherData.getLauncher();
		if (launcherFile != null) {
			if (Constants.OS_MACOSX.equals(launcherData.getOS())) {
				//the equinox launcher will look 3 levels up on the mac when going from executable to launcher.jar
				//see org.eclipse.equinox.executable/library/eclipse.c : findStartupJar();
				IPath launcherPath = new Path(launcherFile.getAbsolutePath());
				if (launcherPath.segmentCount() > 2) {
					//removing "Eclipse.app/Contents/MacOS/eclipse"
					launcherPath = launcherPath.removeLastSegments(2);
					try {
						return launcherPath.toFile().toURI().toURL();
					} catch (MalformedURLException e) {
						// ignore - shouldn't happen
					}
				}
			}
			try {
				return launcherFile.getParentFile().toURI().toURL();
			} catch (MalformedURLException e) {
				// ignore - shouldn't happen
			}
		}

		// we couldn't calculate it based on the info in the launcher data, so
		// try to do it based on the location of known JARs.
		final String OSGI = "org.eclipse.osgi"; //$NON-NLS-1$
		BundleInfo[] bis = manipulator.getConfigData().getBundles();
		String searchFor = "org.eclipse.equinox.launcher"; //$NON-NLS-1$
		for (int i = 0; i < bis.length; i++) {
			if (bis[i].getSymbolicName().equals(searchFor)) {
				if (bis[i].getLocation() != null) {
					try {
						if (bis[i].getLocation().getScheme().equals("file")) //$NON-NLS-1$
							return fromOSGiJarToOSGiInstallArea(bis[i].getLocation().getPath()).toURI().toURL();
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

	public PlatformConfigurationWrapper(File configDir, URI featurePool, Manipulator manipulator) {
		this.configuration = null;
		this.configFile = new File(configDir, "/org.eclipse.update/platform.xml"); //$NON-NLS-1$
		this.poolURI = featurePool;
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
			throw new IllegalStateException(Messages.error_parsing_configuration);
		}
		if (poolURI == null)
			throw new IllegalStateException("Error creating platform configuration. No bundle pool defined."); //$NON-NLS-1$

		poolSite = getSite(poolURI);
		if (poolSite == null) {
			poolSite = createSite(poolURI, getDefaultPolicy());
			configuration.add(poolSite);
		}
	}

	/*
	 * Return the default policy to use when creating a new site. If there are
	 * any sites with the MANAGED-ONLY policy, then that is the default.
	 * Otherwise the default is USER-EXCLUDE.
	 */
	private String getDefaultPolicy() {
		for (Site site : configuration.getSites()) {
			if (Site.POLICY_MANAGED_ONLY.equals(site.getPolicy()))
				return Site.POLICY_MANAGED_ONLY;
		}
		return Site.POLICY_USER_EXCLUDE;
	}

	/*
	 * Create and return a site object based on the given location.
	 */
	private Site createSite(URI location, String policy) {
		Site result = new Site();
		result.setUrl(location.toString());
		result.setPolicy(policy);
		result.setEnabled(true);
		return result;
	}

	/*
	 * Look in the configuration and return the site object whose location matches
	 * the given URL. Return null if there is no match.
	 */
	private Site getSite(URI url) {
		List<Site> sites = configuration.getSites();
		File file = URIUtil.toFile(url);
		for (Site nextSite : sites) {
			try {
				File nextFile = URIUtil.toFile(new URI(nextSite.getUrl()));
				if (nextFile == null)
					continue;
				if (nextFile.equals(file))
					return nextSite;
			} catch (URISyntaxException e) {
				//ignore incorrectly formed site
			}
		}
		return null;
	}

	/*
	 * Look in the configuration and return the site which contains the feature
	 * with the given identifier and version. Return null if there is none.
	 */
	private Site getSite(String id, String version) {
		List<Site> sites = configuration.getSites();
		for (Site site : sites) {
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
	public IStatus addFeatureEntry(File file, String id, String version, String pluginIdentifier, String pluginVersion, boolean primary, String application, URL[] root, String linkFile) {
		loadDelegate();
		if (configuration == null)
			return new Status(IStatus.WARNING, Activator.ID, Messages.platform_config_unavailable, null);

		URI fileURL = null;
		File featureDir = file.getParentFile();
		if (featureDir == null || !featureDir.getName().equals("features")) //$NON-NLS-1$
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.parent_dir_features, file.getAbsolutePath()), null);
		File locationDir = featureDir.getParentFile();
		if (locationDir == null)
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.cannot_calculate_extension_location, file.getAbsolutePath()), null);

		fileURL = locationDir.toURI();
		Site site = getSite(fileURL);
		if (site == null) {
			site = createSite(fileURL, getDefaultPolicy());
			if (linkFile != null)
				site.setLinkFile(linkFile);
			configuration.add(site);
		} else {
			// check to see if the feature already exists in this site
			if (site.getFeature(id, version) != null)
				return Status.OK_STATUS;
		}
		Feature addedFeature = new Feature(site);
		addedFeature.setId(id);
		addedFeature.setVersion(version);
		addedFeature.setUrl(makeFeatureURL(id, version));
		addedFeature.setApplication(application);
		addedFeature.setPluginIdentifier(pluginIdentifier);
		addedFeature.setPluginVersion(pluginVersion);
		addedFeature.setRoots(root);
		addedFeature.setPrimary(primary);
		site.addFeature(addedFeature);
		return Status.OK_STATUS;
	}

	/*
	 * @see org.eclipse.update.configurator.IPlatformConfiguration#findConfiguredFeatureEntry(java.lang.String)
	 */
	public IStatus removeFeatureEntry(String id, String version) {
		loadDelegate();
		if (configuration == null)
			return new Status(IStatus.WARNING, Activator.ID, Messages.platform_config_unavailable, null);

		Site site = getSite(id, version);
		if (site == null)
			site = poolSite;
		site.removeFeature(makeFeatureURL(id, version));
		// if we weren't able to remove the feature from the site because it
		// didn't exist, then someone already did our job for us and it is ok.
		return Status.OK_STATUS;
	}

	public boolean containsFeature(URI siteURI, String featureId, String featureVersion) {
		loadDelegate();
		if (configuration == null)
			return false;

		Site site = getSite(siteURI);
		if (site == null)
			return false;

		return site.getFeature(featureId, featureVersion) != null;
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
