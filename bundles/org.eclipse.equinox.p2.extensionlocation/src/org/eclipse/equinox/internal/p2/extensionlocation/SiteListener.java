/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.extensionlocation;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.internal.p2.metadata.generator.features.FeatureParser;
import org.eclipse.equinox.internal.p2.update.Site;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.*;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.Messages;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.ServiceReference;

/**
 * @since 1.0
 */
public class SiteListener extends RepositoryListener {

	private static final String FEATURES = "features"; //$NON-NLS-1$
	private static final String PLUGINS = "plugins"; //$NON-NLS-1$
	private DirectoryChangeListener delegate;
	private Site site;
	private String[] managedFiles;

	/*
	 * Return true if the given list contains the full path of the given file 
	 * handle. Return false otherwise.
	 */
	private static boolean contains(String[] plugins, File file) {
		String filename = file.getAbsolutePath();
		for (int i = 0; i < plugins.length; i++)
			if (filename.endsWith(new File(plugins[i]).toString()))
				return true;
		return false;
	}

	/*
	 * Create a new site listener on the given site.
	 */
	public SiteListener(Site site, DirectoryChangeListener delegate) {
		super(Activator.getContext(), site.getUrl());
		this.delegate = delegate;
		this.site = site;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener#isInterested(java.io.File)
	 */
	public boolean isInterested(File file) {
		// make sure that our delegate and super-class are both interested in 
		// the file before we consider it
		if (!delegate.isInterested(file) || !super.isInterested(file))
			return false;
		String policy = site.getPolicy();
		String[] plugins = site.getList();
		if (Site.POLICY_MANAGED_ONLY.equals(policy)) {
			// we only want plug-ins referenced by features
			return contains(getManagedFiles(), file);
		} else if (Site.POLICY_USER_EXCLUDE.equals(policy)) {
			// ensure the file doesn't refer to a plug-in in our list
			return plugins.length == 0 ? true : !contains(plugins, file);
		} else if (Site.POLICY_USER_INCLUDE.equals(policy)) {
			// we are only interested in plug-ins in the list
			return plugins.length == 0 ? false : contains(plugins, file);
		}
		return false;
	}

	/*
	 * Return an array of files which are managed. This includes all of the features
	 * for this site, as well as the locations for all the plug-ins referenced by those
	 * features.
	 */
	private String[] getManagedFiles() {
		if (managedFiles != null)
			return managedFiles;
		List result = new ArrayList();
		File siteLocation;
		try {
			siteLocation = URLUtil.toFile(new URL(site.getUrl()));
		} catch (MalformedURLException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Unable to create a URL from site locatin: " + site.getUrl(), e));
			return new String[0];
		}
		Map pluginCache = getPlugins(siteLocation);
		Map featureCache = getFeatures(siteLocation);
		for (Iterator iter = featureCache.keySet().iterator(); iter.hasNext();) {
			File featureFile = (File) iter.next();
			// add the feature path
			result.add(featureFile.toString());
			org.eclipse.equinox.internal.provisional.p2.metadata.generator.Feature feature = (org.eclipse.equinox.internal.provisional.p2.metadata.generator.Feature) featureCache.get(featureFile);
			FeatureEntry[] entries = feature.getEntries();
			for (int inner = 0; inner < entries.length; inner++) {
				FeatureEntry entry = entries[inner];
				// grab the right location from the plug-in cache
				String key = entry.getId() + '/' + entry.getVersion();
				File pluginLocation = (File) pluginCache.get(key);
				if (pluginLocation != null)
					result.add(pluginLocation.toString());
			}
		}
		managedFiles = (String[]) result.toArray(new String[result.size()]);
		return managedFiles;
	}

	/*
	 * Iterate over the feature directory and return a map of 
	 * File to Feature objects (from the generator bundle)
	 */
	private Map getFeatures(File siteLocation) {
		Map result = new HashMap();
		File featureDir = new File(siteLocation, FEATURES);
		File[] children = featureDir.listFiles();
		for (int i = 0; i < children.length; i++) {
			File child = children[i];
			FeatureParser parser = new FeatureParser();
			Feature entry = parser.parse(child);
			if (entry != null)
				result.put(child, entry);
		}
		return result;
	}

	/*
	 * Iterate over the plugins directory and return a map of
	 * plug-in id/version to File locations.
	 */
	private Map getPlugins(File siteLocation) {
		ServiceReference reference = Activator.getContext().getServiceReference(PlatformAdmin.class.getName());
		if (reference == null)
			throw new IllegalStateException(Messages.platformadmin_not_registered);
		try {
			PlatformAdmin platformAdmin = (PlatformAdmin) Activator.getContext().getService(reference);
			if (platformAdmin == null)
				throw new IllegalStateException(Messages.platformadmin_not_registered);
			StateObjectFactory stateObjectFactory = platformAdmin.getFactory();
			BundleDescriptionFactory factory = new BundleDescriptionFactory(stateObjectFactory, null);
			File[] plugins = new File(siteLocation, PLUGINS).listFiles();
			Map result = new HashMap();
			for (int i = 0; plugins != null && i < plugins.length; i++) {
				File bundleLocation = plugins[i];
				BundleDescription description = factory.getBundleDescription(bundleLocation);
				String id = description.getSymbolicName();
				String version = description.getVersion().toString();
				result.put(id + '/' + version, bundleLocation);
			}
			return result;
		} finally {
			Activator.getContext().ungetService(reference);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener#added(java.io.File)
	 */
	public boolean added(File file) {
		return delegate.added(file);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener#changed(java.io.File)
	 */
	public boolean changed(File file) {
		return delegate.changed(file);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener#getSeenFile(java.io.File)
	 */
	public Long getSeenFile(File file) {
		return delegate.getSeenFile(file);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener#removed(java.io.File)
	 */
	public boolean removed(File file) {
		return delegate.removed(file);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener#startPoll()
	 */
	public void startPoll() {
		delegate.startPoll();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener#stopPoll()
	 */
	public void stopPoll() {
		delegate.stopPoll();
	}

}
