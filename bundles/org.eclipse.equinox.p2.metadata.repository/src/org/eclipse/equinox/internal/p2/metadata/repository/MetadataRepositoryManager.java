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
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.Query;
import org.eclipse.equinox.spi.p2.metadata.repository.IMetadataRepositoryFactory;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class MetadataRepositoryManager implements IMetadataRepositoryManager {
	static class RepositoryInfo {
		String description;
		boolean isSystem = false;
		URL location;
		String name;
		SoftReference repository;
	}

	private static final String FACTORY = "factory"; //$NON-NLS-1$

	private static final String KEY_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String KEY_NAME = "name"; //$NON-NLS-1$
	private static final String KEY_PROVIDER = "provider"; //$NON-NLS-1$
	private static final String KEY_SYSTEM = "isSystem"; //$NON-NLS-1$
	private static final String KEY_TYPE = "type"; //$NON-NLS-1$
	private static final String KEY_URL = "url"; //$NON-NLS-1$
	private static final String KEY_VERSION = "version"; //$NON-NLS-1$
	private static final String NODE_REPOSITORIES = "repositories"; //$NON-NLS-1$

	/**
	 * Map of String->RepositoryInfo, where String is the repository key
	 * obtained vai getKey(URL).
	 */
	private Map repositories = null;
	//lock object to be held when referring to the repositories field
	private final Object repositoryLock = new Object();

	public MetadataRepositoryManager() {
		//initialize repositories lazily
	}

	public void addRepository(IMetadataRepository repository) {
		RepositoryInfo info = new RepositoryInfo();
		info.repository = new SoftReference(repository);
		info.name = repository.getName();
		info.description = repository.getDescription();
		info.location = repository.getLocation();
		String value = (String) repository.getProperties().get(IRepository.PROP_SYSTEM);
		info.isSystem = value == null ? false : Boolean.valueOf(value).booleanValue();
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			repositories.put(getKey(repository), info);
		}
		// save the given repository in the preferences.
		remember(repository);
	}

	public void addRepository(URL location) {
		Assert.isNotNull(location);
		RepositoryInfo info = new RepositoryInfo();
		info.location = location;
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			repositories.put(getKey(location), info);
		}
		// save the given repository in the preferences.
		remember(info);
	}

	/**
	 * Returns the executable extension, or <code>null</code> if there
	 * was no corresponding extension, or an error occurred loading it
	 */
	private Object createExecutableExtension(IExtension extension, String element) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		CoreException failure = null;
		for (int i = 0; i < elements.length; i++) {
			if (elements[i].getName().equals(element)) {
				try {
					return elements[i].createExecutableExtension("class"); //$NON-NLS-1$
				} catch (CoreException e) {
					log("Error loading repository extension: " + extension.getUniqueIdentifier(), failure); //$NON-NLS-1$
					return null;
				}
			}
		}
		log("Malformed repository extension: " + extension.getUniqueIdentifier(), null); //$NON-NLS-1$
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager#createRepository(java.net.URL, java.lang.String, java.lang.String)
	 */
	public IMetadataRepository createRepository(URL location, String name, String type) throws ProvisionException {
		Assert.isNotNull(location);
		Assert.isNotNull(name);
		Assert.isNotNull(type);
		try {
			//repository should not already exist
			loadRepository(location, (IProgressMonitor) null);
			fail(location, ProvisionException.REPOSITORY_EXISTS);
		} catch (ProvisionException e) {
			//expected - fall through and create the new repository
		}
		IExtension extension = RegistryFactory.getRegistry().getExtension(Activator.REPO_PROVIDER_XPT, type);
		if (extension == null)
			fail(location, ProvisionException.REPOSITORY_UNKNOWN_TYPE);
		IMetadataRepositoryFactory factory = (IMetadataRepositoryFactory) createExecutableExtension(extension, FACTORY);
		if (factory == null)
			fail(location, ProvisionException.REPOSITORY_FAILED_READ);
		IMetadataRepository result = factory.create(location, name, type);
		if (result == null)
			fail(location, ProvisionException.REPOSITORY_FAILED_READ);
		addRepository(result);
		return result;
	}

	private void fail(URL location, int code) throws ProvisionException {
		String msg = null;
		switch (code) {
			case ProvisionException.REPOSITORY_EXISTS :
				msg = NLS.bind(Messages.repoMan_exists, location);
				break;
			case ProvisionException.REPOSITORY_UNKNOWN_TYPE :
				msg = NLS.bind(Messages.repoMan_unknownType, location);
				break;
			case ProvisionException.REPOSITORY_FAILED_READ :
				msg = NLS.bind(Messages.repoMan_failedRead, location);
				break;
			case ProvisionException.REPOSITORY_NOT_FOUND :
				msg = NLS.bind(Messages.repoMan_notExists, location);
				break;
		}
		if (msg == null)
			msg = Messages.repoMan_internalError;
		throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, code, msg, null));
	}

	private IExtension[] findMatchingRepositoryExtensions(String suffix) {
		IConfigurationElement[] elt = RegistryFactory.getRegistry().getConfigurationElementsFor(Activator.REPO_PROVIDER_XPT);
		int count = 0;
		for (int i = 0; i < elt.length; i++) {
			if (elt[i].getName().equals("filter")) { //$NON-NLS-1$
				if (!elt[i].getAttribute("suffix").equals(suffix)) { //$NON-NLS-1$
					elt[i] = null;
				} else {
					count++;
				}
			} else {
				elt[i] = null;
			}
		}
		IExtension[] results = new IExtension[count];
		for (int i = 0; i < elt.length; i++) {
			if (elt[i] != null)
				results[--count] = elt[i].getDeclaringExtension();
		}
		return results;
	}

	private String[] getAllSuffixes() {
		IConfigurationElement[] elements = RegistryFactory.getRegistry().getConfigurationElementsFor(Activator.REPO_PROVIDER_XPT);
		ArrayList result = new ArrayList(elements.length);
		for (int i = 0; i < elements.length; i++)
			if (elements[i].getName().equals("filter")) //$NON-NLS-1$
				result.add(elements[i].getAttribute("suffix")); //$NON-NLS-1$
		return (String[]) result.toArray(new String[result.size()]);
	}

	/*
	 * Return a string key based on the given repository which
	 * is suitable for use as a preference node name.
	 */
	private String getKey(IMetadataRepository repository) {
		return getKey(repository.getLocation());
	}

	/*
	 * Return a string key based on the given repository location which
	 * is suitable for use as a preference node name.
	 */
	private String getKey(URL location) {
		return location.toExternalForm().replace('/', '_');
	}

	public URL[] getKnownRepositories(int flags) {
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			ArrayList result = new ArrayList();
			int i = 0;
			for (Iterator it = repositories.values().iterator(); it.hasNext(); i++) {
				RepositoryInfo info = (RepositoryInfo) it.next();
				if (matchesFlags(info, flags))
					result.add(info.location);
			}
			return (URL[]) result.toArray(new URL[result.size()]);
		}
	}

	/*
	 * Return the preference node which is the root for where we store the repository information.
	 */
	private Preferences getPreferences() {
		return new ConfigurationScope().getNode(Activator.PI_METADATA_REPOSITORY).node(NODE_REPOSITORIES);
	}

	public IMetadataRepository getRepository(URL location) {
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			for (Iterator it = repositories.values().iterator(); it.hasNext();) {
				RepositoryInfo info = (RepositoryInfo) it.next();
				if (URLUtil.sameURL(info.location, location)) {
					if (info.repository == null)
						return null;
					IMetadataRepository repo = (IMetadataRepository) info.repository.get();
					//update our repository info because the repository may have changed
					if (repo != null)
						addRepository(repo);
					return repo;
				}
			}
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager#getRepositoryProperty(java.net.URL, java.lang.String)
	 */
	public String getRepositoryProperty(URL location, String key) {
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			for (Iterator it = repositories.values().iterator(); it.hasNext();) {
				RepositoryInfo info = (RepositoryInfo) it.next();
				if (URLUtil.sameURL(info.location, location)) {
					if (PROP_DESCRIPTION.equals(key))
						return info.description;
					if (PROP_NAME.equals(key))
						return info.name;
					// Key not known, return null
					return null;
				}
			}
			// Repository not found, return null
			return null;
		}
	}

	public IMetadataRepository loadRepository(URL location, IProgressMonitor progress) throws ProvisionException {
		Assert.isNotNull(location);
		IMetadataRepository result = getRepository(location);
		if (result != null)
			return result;
		String[] suffixes = getAllSuffixes();
		if (progress == null)
			progress = new NullProgressMonitor();
		progress.beginTask(NLS.bind(Messages.REPOMGR_ADDING_REPO, location.toExternalForm()), 1);
		for (int i = 0; i < suffixes.length; i++) {
			result = loadRepository(location, suffixes[i]);
			if (result != null) {
				addRepository(result);
				progress.done();
				return result;
			}
		}
		progress.done();
		fail(location, ProvisionException.REPOSITORY_NOT_FOUND);
		return null;//will never get here
	}

	/**
	 * Try to load a pre-existing repo at the given location
	 * @throws ProvisionException 
	 */
	private IMetadataRepository loadRepository(URL location, String suffix) throws ProvisionException {
		IExtension[] providers = findMatchingRepositoryExtensions(suffix);
		// Loop over the candidates and return the first one that successfully loads
		for (int i = 0; i < providers.length; i++) {
			IMetadataRepositoryFactory factory = (IMetadataRepositoryFactory) createExecutableExtension(providers[i], FACTORY);
			if (factory != null)
				return factory.load(location);
		}
		return null;
	}

	protected void log(String message, Throwable t) {
		LogHelper.log(new Status(IStatus.ERROR, Activator.PI_METADATA_REPOSITORY, message, t));
	}

	private boolean matchesFlags(RepositoryInfo info, int flags) {
		if ((flags & REPOSITORIES_SYSTEM) == REPOSITORIES_SYSTEM)
			if (!info.isSystem)
				return false;
		if ((flags & REPOSITORIES_NON_SYSTEM) == REPOSITORIES_NON_SYSTEM)
			if (info.isSystem)
				return false;
		if ((flags & REPOSITORIES_LOCAL) == REPOSITORIES_LOCAL)
			return "file".equals(info.location.getProtocol()); //$NON-NLS-1$
		return true;
	}

	/**
	 * Sets a preference and returns <code>true</code> if the preference
	 * was actually changed.
	 */
	private boolean putValue(Preferences node, String key, String newValue) {
		String oldValue = node.get(key, null);
		if (oldValue == newValue || (oldValue != null && oldValue.equals(newValue)))
			return false;
		if (newValue == null)
			node.remove(key);
		else
			node.put(key, newValue);
		return true;
	}

	/**
	 * Performs a query against all of the installable units of each known 
	 * repository, accumulating any objects that satisfy the query in the 
	 * provided collector.
	 * <p>
	 * Note that using this method can be quite expensive, as every known
	 * metadata repository will be loaded in order to query each one.  If a
	 * client wishes to query only certain repositories, it is better to use
	 * {@link #getKnownRepositories(int)} to filter the list of repositories
	 * loaded and then query each of the returned repositories.
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor. 
	 * 
	 * @param query The query to perform against each installable unit in each known repository
	 * @param collector Collects the results of the query
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The collector argument
	 */
	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		URL[] locations = getKnownRepositories(REPOSITORIES_ALL);
		SubMonitor sub = SubMonitor.convert(monitor, locations.length * 10);
		for (int i = 0; i < locations.length; i++) {
			try {
				loadRepository(locations[i], sub.newChild(9)).query(query, collector, sub.newChild(1));
			} catch (ProvisionException e) {
				//ignore this repository for this query
			}
		}
		sub.done();
		return collector;
	}

	/*
	 * Save the list of repositories in the preference store.
	 */
	private void remember(IMetadataRepository repository) {
		boolean changed = false;
		Preferences node = getPreferences().node(getKey(repository));
		changed |= putValue(node, KEY_URL, repository.getLocation().toExternalForm());
		changed |= putValue(node, KEY_DESCRIPTION, repository.getDescription());
		changed |= putValue(node, KEY_NAME, repository.getName());
		changed |= putValue(node, KEY_PROVIDER, repository.getProvider());
		changed |= putValue(node, KEY_TYPE, repository.getType());
		changed |= putValue(node, KEY_VERSION, repository.getVersion());
		changed |= putValue(node, KEY_SYSTEM, (String) repository.getProperties().get(IRepository.PROP_SYSTEM));
		if (changed)
			saveToPreferences();
	}

	/*
	 * Save the list of repositories in the preference store.
	 */
	private void remember(RepositoryInfo info) {
		boolean changed = false;
		Preferences node = getPreferences().node(getKey(info.location));
		changed |= putValue(node, KEY_URL, info.location.toExternalForm());
		changed |= putValue(node, KEY_SYSTEM, Boolean.toString(info.isSystem));
		changed |= putValue(node, KEY_DESCRIPTION, info.description);
		changed |= putValue(node, KEY_NAME, info.name);
		if (changed)
			saveToPreferences();
	}

	public boolean removeRepository(URL toRemove) {
		Assert.isNotNull(toRemove);
		final String repoKey = getKey(toRemove);
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			if (repositories.remove(repoKey) == null)
				return false;
		}
		// remove the repository from the preference store
		try {
			getPreferences().node(repoKey).removeNode();
			saveToPreferences();
		} catch (BackingStoreException e) {
			log("Error saving preferences", e); //$NON-NLS-1$
		}
		return true;
	}

	/**
	 * Restore the list of repositories from the preference store.
	 */
	private void restoreFromPreferences() {
		// restore the list of repositories from the preference store
		Preferences node = getPreferences();
		String[] children;
		try {
			children = node.childrenNames();
		} catch (BackingStoreException e) {
			log("Error restoring repositories from preferences", e); //$NON-NLS-1$
			return;
		}
		for (int i = 0; i < children.length; i++) {
			Preferences child = node.node(children[i]);
			String locationString = child.get(KEY_URL, null);
			if (locationString == null)
				continue;
			try {
				RepositoryInfo info = new RepositoryInfo();
				info.location = new URL(locationString);
				info.name = child.get(KEY_NAME, null);
				info.description = child.get(KEY_DESCRIPTION, null);
				info.isSystem = child.getBoolean(KEY_SYSTEM, false);
				repositories.put(getKey(info.location), info);
			} catch (MalformedURLException e) {
				log("Error while restoring repository: " + locationString, e); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Restores metadata repositories specified as system properties.
	 */
	private void restoreFromSystemProperty() {
		String locationString = Activator.getContext().getProperty("eclipse.p2.metadataRepository"); //$NON-NLS-1$
		if (locationString == null)
			return;
		StringTokenizer tokenizer = new StringTokenizer(locationString, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String pathString = tokenizer.nextToken();
			try {
				RepositoryInfo info = new RepositoryInfo();
				info.location = new URL(pathString);
				repositories.put(getKey(info.location), info);
			} catch (MalformedURLException e) {
				log("Error while restoring repository " + pathString, e); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Restores the repository list.
	 */
	protected void restoreRepositories() {
		synchronized (repositoryLock) {
			repositories = new HashMap();
			restoreFromSystemProperty();
			restoreFromPreferences();
		}
	}

	/*
	 * Save the repository list in the file-system
	 */
	private void saveToPreferences() {
		try {
			getPreferences().flush();
		} catch (BackingStoreException e) {
			log("Error while saving repositories in preferences", e); //$NON-NLS-1$
		}
	}
}
