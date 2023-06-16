/*******************************************************************************
 *  Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.extensionlocation.*;
import org.eclipse.equinox.internal.p2.update.*;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
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
	private Set<IMetadataRepository> configRepositories;

	private String toString(Feature[] features, String[] list) {
		StringBuilder buffer = new StringBuilder();
		if (features != null) {
			for (Feature feature : features) {
				String featureURL = feature.getUrl();
				if (featureURL != null) {
					buffer.append(featureURL).append(',');
				} else {
					String id = feature.getId();
					String version = feature.getVersion();
					if (id != null && version != null)
						buffer.append("features/" + id + "_" + version + "/,"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$					
				}
			}
		}
		if (list != null) {
			for (String list1 : list) {
				buffer.append(list1).append(',');
			}
		}
		if (buffer.length() == 0)
			return ""; //$NON-NLS-1$

		return buffer.substring(0, buffer.length() - 1);
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

	@Override
	public boolean added(File file) {
		changed = changed || PLATFORM_XML.equals(file.getName());
		return false;
	}

	@Override
	public boolean changed(File file) {
		changed = changed || PLATFORM_XML.equals(file.getName());
		return false;
	}

	@Override
	public Long getSeenFile(File file) {
		return Long.valueOf(0);
	}

	@Override
	public boolean isInterested(File file) {
		return file.getName().equals(PLATFORM_XML) && lastModified != file.lastModified();
	}

	@Override
	public boolean removed(File file) {
		changed = changed || PLATFORM_XML.equals(file.getName());
		return false;
	}

	@Override
	public void startPoll() {
		changed = false;
	}

	@Override
	public void stopPoll() {
		if (changed) {
			lastModified = root.lastModified();
			try {
				Configuration configuration = Configuration.load(root, Activator.getOSGiInstallArea());
				synchronizeConfiguration(configuration);
			} catch (ProvisionException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, Messages.errorProcessingConfg, e));
			}
		}
		changed = false;
	}

	public Collection<IMetadataRepository> getMetadataRepositories() {
		if (configRepositories == null)
			return Collections.emptySet();
		return configRepositories;
	}

	/*
	 * Look through the given list of repositories and see if there is one
	 * currently associated with the given url string. Return null if one could not
	 * be found.
	 */
	private IMetadataRepository getMatchingRepo(Collection<IMetadataRepository> repositoryList, String urlString) {
		if (repositoryList == null)
			return null;
		IPath urlPath = IPath.fromOSString(urlString).makeAbsolute();
		for (IMetadataRepository repo : repositoryList) {
			File file = URIUtil.toFile(repo.getLocation());
			if (file == null)
				continue;
			IPath repoPath = IPath.fromOSString(file.getAbsolutePath());
			if (repoPath.makeAbsolute().equals(urlPath))
				return repo;
			// normalize the URLs to be the same
			if (repo instanceof ExtensionLocationMetadataRepository) {
				try {
					File one = ExtensionLocationMetadataRepository.getBaseDirectory(repo.getLocation());
					File two = ExtensionLocationMetadataRepository.getBaseDirectory(new URI(urlString));
					if (one.equals(two))
						return repo;
				} catch (ProvisionException e) {
					// Skip the repo if it's not found. Log all other errors.
					if (e.getStatus().getCode() != ProvisionException.REPOSITORY_NOT_FOUND)
						LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while comparing repository locations.", e)); //$NON-NLS-1$
				} catch (URISyntaxException e) {
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while comparing repository locations.", e)); //$NON-NLS-1$
				}
			}
		}
		return null;
	}

	/*
	 * Ensure that we have a repository for each site in the given configuration.
	 */
	protected void synchronizeConfiguration(Configuration config) {
		List<Site> sites = config.getSites();
		Set<IMetadataRepository> newRepos = new LinkedHashSet<>();
		Set<Site> toBeRemoved = new HashSet<>();
		for (Site site : sites) {
			String siteURL = site.getUrl();
			IMetadataRepository match = getMatchingRepo(Activator.getRepositories(), siteURL);
			if (match == null) {
				try {
					String linkFile = site.getLinkFile();
					if (linkFile != null && linkFile.length() > 0) {
						File link = new File(linkFile);
						if (!link.exists()) {
							toBeRemoved.add(site);
							continue;
						}
					}
					if (!site.isEnabled()) {
						toBeRemoved.add(site);
						continue;
					}
					String eclipseExtensionURL = siteURL + Constants.EXTENSION_LOCATION;
					// use the URI constructor here and not URIUtil#fromString because 
					// our string is already encoded
					URI location = new URI(eclipseExtensionURL);
					Map<String, String> properties = new HashMap<>();
					properties.put(SiteListener.SITE_POLICY, site.getPolicy());

					// In a "USER-INCLUDE" we add the site's features to the list
					// this is done to support backwards compatibility where previously features were not really installed.
					// One can always directly add the features to this list. This might be useful for excluding a particular feature
					// in a "USER-EXCLUDE" site. 
					Feature[] listFeatures = site.getPolicy().equals(Site.POLICY_USER_INCLUDE) ? site.getFeatures() : null;

					properties.put(SiteListener.SITE_LIST, toString(listFeatures, site.getList()));

					// deal with the metadata repository
					IMetadataRepository metadataRepository = null;
					try {
						metadataRepository = Activator.createExtensionLocationMetadataRepository(location, "extension location metadata repository: " + location.toString(), properties); //$NON-NLS-1$
					} catch (ProvisionException ex) {
						try {
							metadataRepository = Activator.loadMetadataRepository(location, null);
						} catch (ProvisionException inner) {
							// handle the case where someone has removed the extension location from
							// disk. Note: we use the siteURL not the eclipseextensionURL
							// use the URI constructor here and not URIUtil#fromString because 
							// our string is already encoded
							URI fileURI = new URI(siteURL);
							File file = URIUtil.toFile(fileURI);
							if (file != null && !file.exists()) {
								toBeRemoved.add(site);
								continue;
							}
							throw inner;
						}
						// set the repository properties here in case they have changed since the last time we loaded
						for (Entry<String, String> entry : properties.entrySet()) {
							metadataRepository.setProperty(entry.getKey(), entry.getValue());
						}
					}
					newRepos.add(metadataRepository);

					// now the artifact repository
					try {
						Activator.createExtensionLocationArtifactRepository(location, "extension location artifact repository: " + location, properties); //$NON-NLS-1$
					} catch (ProvisionException ex) {
						IArtifactRepository artifactRepository = Activator.loadArtifactRepository(location, null);
						// set the repository properties here in case they have changed since the last time we loaded
						for (String key : properties.keySet()) {
							String value = properties.get(key);
							artifactRepository.setProperty(key, value);
						}
					}
				} catch (URISyntaxException e) {
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.errorLoadingRepository, siteURL), e));
				} catch (ProvisionException e) {
					// Skip the repo if it's not found. Log all other errors.
					if (e.getStatus().getCode() != ProvisionException.REPOSITORY_NOT_FOUND)
						LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.errorLoadingRepository, siteURL), e));
				}
			} else {
				newRepos.add(match);
			}
		}
		if (!toBeRemoved.isEmpty()) {
			for (Site site : toBeRemoved)
				config.removeSite(site);
			try {
				config.save(root, Activator.getOSGiInstallArea());
			} catch (ProvisionException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while saving configuration at: " + root, e)); //$NON-NLS-1$
			}
		}
		configRepositories = newRepos;
	}
}
