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
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.extensionlocation.ExtensionLocationMetadataRepository;
import org.eclipse.equinox.internal.p2.update.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;

/**
 * This class watches a platform.xml file. Note that we don't really need to use the DirectoryChangeListener
 * framework since we are doing a single poll on startup, but we will leave the code here in case we
 * want to watch for changes during a session. Note that the code to actually synchronize the repositories
 * is on the Activator so we will need to call out to that if this behaviour is changed.
 * 
 * @since 1.0
 */
public class PlatformXmlListener extends DirectoryChangeListener {

	private static final String PLATFORM_XML = "platform.xml"; //$NON-NLS-1$
	private boolean changed = false;
	private File root;
	private long lastModified = -1l;
	private Set configRepositories;

	/*
	 * Construct a new listener based on the given platform.xml file.
	 */
	public PlatformXmlListener(File file) {
		super();
		if (!PLATFORM_XML.equals(file.getName()))
			throw new IllegalArgumentException();
		this.root = file;
	}

	/*
	 * Do a look-up and return the OSGi install area if it is set.
	 */
	private static URL getOSGiInstallArea() {
		Location location = (Location) ServiceHelper.getService(Activator.getContext(), Location.class.getName(), Location.INSTALL_FILTER);
		if (location == null)
			return null;
		if (!location.isSet())
			return null;
		return location.getURL();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#added(java.io.File)
	 */
	public boolean added(File file) {
		changed = changed || PLATFORM_XML.equals(file.getName());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#changed(java.io.File)
	 */
	public boolean changed(File file) {
		changed = changed || PLATFORM_XML.equals(file.getName());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#getSeenFile(java.io.File)
	 */
	public Long getSeenFile(File file) {
		return new Long(0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener#isInterested(java.io.File)
	 */
	public boolean isInterested(File file) {
		return file.getName().equals(PLATFORM_XML) && lastModified != file.lastModified();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#removed(java.io.File)
	 */
	public boolean removed(File file) {
		changed = changed || PLATFORM_XML.equals(file.getName());
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#startPoll()
	 */
	public void startPoll() {
		changed = false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#stopPoll()
	 */
	public void stopPoll() {
		if (changed) {
			lastModified = root.lastModified();
			try {
				Configuration configuration = ConfigurationParser.parse(root, getOSGiInstallArea());
				synchronizeConfiguration(configuration);
			} catch (ProvisionException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, Messages.errorProcessingConfg, e));
			}
		}
		changed = false;
	}

	public Collection getMetadataRepositories() {
		return configRepositories;
	}

	/*
	 * Look through the given list of repositories and see if there is one
	 * currently associated with the given url string. Return null if one could not
	 * be found.
	 */
	private IMetadataRepository getMatchingRepo(Collection repositoryList, String urlString) {
		if (repositoryList == null)
			return null;
		IPath urlPath = new Path(urlString).makeAbsolute();
		for (Iterator iter = repositoryList.iterator(); iter.hasNext();) {
			IMetadataRepository repo = (IMetadataRepository) iter.next();
			Path repoPath = new Path(repo.getLocation().toExternalForm());
			if (repoPath.makeAbsolute().equals(urlPath))
				return repo;
		}
		return null;
	}

	/*
	 * Ensure that we have a repository for each site in the given configuration.
	 */
	protected void synchronizeConfiguration(Configuration config) {
		List sites = config.getSites();
		Set newRepos = new LinkedHashSet();
		for (Iterator iter = sites.iterator(); iter.hasNext();) {
			Site site = (Site) iter.next();
			String siteURL = site.getUrl();
			if (siteURL.startsWith("file:") && siteURL.endsWith("/eclipse/")) //$NON-NLS-1$//$NON-NLS-2$
				siteURL = siteURL.substring(0, siteURL.length() - 8);
			IMetadataRepository match = getMatchingRepo(configRepositories, siteURL);
			if (match == null) {
				try {
					URL repoURL = new URL(siteURL);
					IMetadataRepository newRepo = new ExtensionLocationMetadataRepository(repoURL, site, null);
					Activator.addRepository(newRepo);
					Activator.loadArtifactRepository(repoURL);
					newRepos.add(newRepo);
				} catch (MalformedURLException e) {
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.errorLoadingRepository, siteURL), e));
				} catch (ProvisionException e) {
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.errorLoadingRepository, siteURL), e));
				}
			} else {
				newRepos.add(match);
			}
		}
		configRepositories = newRepos;
	}
}
