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
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.internal.p2.extensionlocation.*;
import org.eclipse.equinox.internal.p2.update.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
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

	private String toString(String[] list) {
		if (list == null || list.length == 0)
			return ""; //$NON-NLS-1$
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < list.length; i++) {
			buffer.append(list[i]);
			if (i + 1 < list.length)
				buffer.append(',');
		}
		return buffer.toString();
	}

	/*
	 * Construct a new listener based on the given platform.xml file.
	 */
	public PlatformXmlListener(File file) {
		super();
		if (!PLATFORM_XML.equals(file.getName()))
			throw new IllegalArgumentException();
		this.root = file;
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
				Configuration configuration = ConfigurationParser.parse(root, Activator.getOSGiInstallArea());
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
		Set toBeRemoved = new HashSet();
		for (Iterator iter = sites.iterator(); iter.hasNext();) {
			Site site = (Site) iter.next();
			String siteURL = site.getUrl();
			String eclipseExtensionURL = siteURL + Constants.EXTENSION_LOCATION;
			IMetadataRepository match = getMatchingRepo(configRepositories, eclipseExtensionURL);
			if (match == null) {
				try {
					URL location = new URL(eclipseExtensionURL);
					Map properties = new HashMap();
					properties.put(SiteListener.SITE_POLICY, site.getPolicy());
					properties.put(SiteListener.SITE_LIST, toString(site.getList()));
					properties.put(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());

					// deal with the metadata repository
					IMetadataRepository metadataRepository = null;
					try {
						metadataRepository = Activator.createExtensionLocationMetadataRepository(location, "extension location metadata repository: " + location.toExternalForm(), properties); //$NON-NLS-1$
					} catch (ProvisionException ex) {
						try {
							metadataRepository = Activator.loadMetadataRepository(location, null);
						} catch (ProvisionException inner) {
							// handle the case where someone has removed the extension location from
							// disk. Note: we use the siteURL not the eclipseextensionURL
							URL fileURL = new URL(siteURL);
							File file = URLUtil.toFile(fileURL);
							if (file != null && !file.exists()) {
								toBeRemoved.add(site);
								continue;
							}
							throw inner;
						}
						// set the repository properties here in case they have changed since the last time we loaded
						for (Iterator inner = properties.keySet().iterator(); inner.hasNext();) {
							String key = (String) inner.next();
							String value = (String) properties.get(key);
							metadataRepository.setProperty(key, value);
						}
						// now that we know we have the right properties set on the repository
						// we will force initialization so things like the timestamp will be updated
						// if necessary
						if (metadataRepository instanceof ExtensionLocationMetadataRepository)
							((ExtensionLocationMetadataRepository) metadataRepository).ensureInitialized();
					}
					newRepos.add(metadataRepository);

					// now the artifact repository
					try {
						Activator.createExtensionLocationArtifactRepository(location, "extension location artifact repository: " + location.toExternalForm(), properties); //$NON-NLS-1$
					} catch (ProvisionException ex) {
						IArtifactRepository artifactRepository = Activator.loadArtifactRepository(location, null);
						// set the repository properties here in case they have changed since the last time we loaded
						for (Iterator inner = properties.keySet().iterator(); inner.hasNext();) {
							String key = (String) inner.next();
							String value = (String) properties.get(key);
							artifactRepository.setProperty(key, value);
						}
						// now that we know we have the right properties set on the repository
						// we will force initialization so things like the timestamp will be updated
						// if necessary
						if (artifactRepository instanceof ExtensionLocationArtifactRepository)
							((ExtensionLocationArtifactRepository) artifactRepository).ensureInitialized();
					}
				} catch (MalformedURLException e) {
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.errorLoadingRepository, siteURL), e));
				} catch (ProvisionException e) {
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.errorLoadingRepository, siteURL), e));
				}
			} else {
				newRepos.add(match);
			}
		}
		if (!toBeRemoved.isEmpty()) {
			for (Iterator iter = toBeRemoved.iterator(); iter.hasNext();)
				config.removeSite((Site) iter.next());
			try {
				config.save(root, Activator.getOSGiInstallArea());
			} catch (ProvisionException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while saving configuration at: " + root, e)); //$NON-NLS-1$
			}
		}
		configRepositories = newRepos;
	}
}
