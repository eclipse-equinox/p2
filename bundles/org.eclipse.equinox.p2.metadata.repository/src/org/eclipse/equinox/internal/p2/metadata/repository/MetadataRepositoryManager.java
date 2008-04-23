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
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryEvent;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.IMetadataRepositoryFactory;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Default implementation of {@link IMetadataRepositoryManager}.
 */
public class MetadataRepositoryManager implements IMetadataRepositoryManager, ProvisioningListener {
	static class RepositoryInfo {
		String description;
		boolean isSystem = false;
		boolean isEnabled = true;
		URL location;
		String name;
		SoftReference repository;
		String suffix;
	}

	private static final String ATTR_SUFFIX = "suffix"; //$NON-NLS-1$
	private static final String DEFAULT_SUFFIX = "content.xml"; //$NON-NLS-1$
	private static final String EL_FILTER = "filter"; //$NON-NLS-1$

	private static final String FACTORY = "factory"; //$NON-NLS-1$
	private static final String KEY_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String KEY_ENABLED = "enabled"; //$NON-NLS-1$
	private static final String KEY_NAME = "name"; //$NON-NLS-1$
	private static final String KEY_PROVIDER = "provider"; //$NON-NLS-1$
	private static final String KEY_SUFFIX = "suffix"; //$NON-NLS-1$
	private static final String KEY_SYSTEM = "isSystem"; //$NON-NLS-1$
	private static final String KEY_TYPE = "type"; //$NON-NLS-1$
	private static final String KEY_URL = "url"; //$NON-NLS-1$
	private static final String KEY_VERSION = "version"; //$NON-NLS-1$

	private static final String NODE_REPOSITORIES = "repositories"; //$NON-NLS-1$

	/**
	 * Map of String->RepositoryInfo, where String is the repository key
	 * obtained via getKey(URL).
	 */
	private Map repositories = null;

	//lock object to be held when referring to the repositories field
	private final Object repositoryLock = new Object();

	/**
	 * Cache List of repositories that are not reachable. Maintain cache
	 * for short duration because repository may become available at any time.
	 */
	private SoftReference unavailableRepositories;

	public MetadataRepositoryManager() {
		IProvisioningEventBus bus = (IProvisioningEventBus) ServiceHelper.getService(Activator.getContext(), IProvisioningEventBus.class.getName());
		if (bus != null)
			bus.addListener(this);
		//initialize repositories lazily
	}

	public void addRepository(IMetadataRepository repository) {
		addRepository(repository, true, null);
	}

	/**
	 * Adds a repository to the list of known repositories
	 * @param repository the repository object to add
	 * @param signalAdd whether a repository change event should be fired
	 * @param suffix the suffix used to load the repository, or <code>null</code> if unknown
	 */
	private void addRepository(IMetadataRepository repository, boolean signalAdd, String suffix) {
		boolean added = true;
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			String key = getKey(repository);
			RepositoryInfo info = (RepositoryInfo) repositories.get(key);
			if (info == null)
				info = new RepositoryInfo();
			info.repository = new SoftReference(repository);
			info.name = repository.getName();
			info.description = repository.getDescription();
			info.location = repository.getLocation();
			String value = (String) repository.getProperties().get(IRepository.PROP_SYSTEM);
			info.isSystem = value == null ? false : Boolean.valueOf(value).booleanValue();
			info.suffix = suffix;
			added = repositories.put(getKey(repository), info) == null;
		}
		// save the given repository in the preferences.
		remember(repository, suffix);
		if (added && signalAdd)
			broadcastChangeEvent(repository.getLocation(), IRepository.TYPE_METADATA, RepositoryEvent.ADDED);
	}

	public void addRepository(URL location) {
		addRepository(location, true);
	}

	private void addRepository(URL location, boolean isEnabled) {
		Assert.isNotNull(location);
		RepositoryInfo info = new RepositoryInfo();
		info.location = location;
		info.isEnabled = isEnabled;
		boolean added = true;
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			added = repositories.put(getKey(location), info) == null;
		}
		// save the given repository in the preferences.
		remember(info);
		if (added)
			broadcastChangeEvent(location, IRepository.TYPE_METADATA, RepositoryEvent.ADDED);
	}

	/**
	 * TODO Eliminate duplication with ArtifactRepositoryManager.
	 */
	protected void broadcastChangeEvent(URL location, int repositoryType, int kind) {
		IProvisioningEventBus bus = (IProvisioningEventBus) ServiceHelper.getService(Activator.getContext(), IProvisioningEventBus.class.getName());
		if (bus != null)
			bus.publishEvent(new RepositoryEvent(location, repositoryType, kind, true));
	}

	/**
	 * Check if we recently attempted to load the given location and failed
	 * to find anything. Returns <code>true</code> if the repository was not
	 * found, and <code>false</code> otherwise.
	 */
	private boolean checkNotFound(URL location) {
		if (unavailableRepositories == null)
			return false;
		List badRepos = (List) unavailableRepositories.get();
		if (badRepos == null)
			return false;
		return badRepos.contains(location);
	}

	/**
	 * Clear the fact that we tried to load a repository at this location and did not find anything.
	 */
	private void clearNotFound(URL location) {
		List badRepos;
		if (unavailableRepositories != null) {
			badRepos = (List) unavailableRepositories.get();
			if (badRepos != null) {
				badRepos.remove(location);
				return;
			}
		}
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
	 * @see org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager#createRepository(java.net.URL, java.lang.String, java.lang.String, java.util.Map)
	 */
	public IMetadataRepository createRepository(URL location, String name, String type, Map properties) throws ProvisionException {
		Assert.isNotNull(location);
		Assert.isNotNull(name);
		Assert.isNotNull(type);
		boolean loaded = false;
		try {
			//repository should not already exist
			loadRepository(location, (IProgressMonitor) null, type, true);
			loaded = true;
		} catch (ProvisionException e) {
			//expected - fall through and create the new repository
		}
		if (loaded)
			fail(location, ProvisionException.REPOSITORY_EXISTS);

		IExtension extension = RegistryFactory.getRegistry().getExtension(Activator.REPO_PROVIDER_XPT, type);
		if (extension == null)
			fail(location, ProvisionException.REPOSITORY_UNKNOWN_TYPE);
		IMetadataRepositoryFactory factory = (IMetadataRepositoryFactory) createExecutableExtension(extension, FACTORY);
		if (factory == null)
			fail(location, ProvisionException.REPOSITORY_FAILED_READ);
		IMetadataRepository result = factory.create(location, name, type, properties);
		if (result == null)
			fail(location, ProvisionException.REPOSITORY_FAILED_READ);
		clearNotFound(location);
		return result;
	}

	private void fail(URL location, int code) throws ProvisionException {
		throw new ProvisionException(failStatus(location, code));
	}

	private IStatus failStatus(URL location, int code) {
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
			case ProvisionException.REPOSITORY_INVALID_LOCATION :
				msg = NLS.bind(Messages.repoMan_invalidLocation, location);
				break;
		}
		if (msg == null)
			msg = Messages.repoMan_internalError;
		return new Status(IStatus.ERROR, Activator.ID, code, msg, null);
	}

	private IExtension[] findMatchingRepositoryExtensions(String suffix, String type) {
		IConfigurationElement[] elt = null;
		if (type != null && type.length() > 0) {
			IExtension ext = RegistryFactory.getRegistry().getExtension(Activator.REPO_PROVIDER_XPT, type);
			elt = (ext != null) ? ext.getConfigurationElements() : new IConfigurationElement[0];
		} else {
			elt = RegistryFactory.getRegistry().getConfigurationElementsFor(Activator.REPO_PROVIDER_XPT);
		}

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
		result.add(DEFAULT_SUFFIX);
		for (int i = 0; i < elements.length; i++) {
			if (elements[i].getName().equals(EL_FILTER)) {
				String suffix = elements[i].getAttribute(ATTR_SUFFIX);
				if (!result.contains(suffix))
					result.add(suffix);
			}
		}
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
		return new ConfigurationScope().getNode(Activator.ID).node(NODE_REPOSITORIES);
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
	 * @see org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager#getRepositoryProperty(java.net.URL, java.lang.String)
	 */
	public String getRepositoryProperty(URL location, String key) {
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			for (Iterator it = repositories.values().iterator(); it.hasNext();) {
				RepositoryInfo info = (RepositoryInfo) it.next();
				if (URLUtil.sameURL(info.location, location)) {
					if (IRepository.PROP_DESCRIPTION.equals(key))
						return info.description;
					if (IRepository.PROP_NAME.equals(key))
						return info.name;
					// Key not known, return null
					return null;
				}
			}
			// Repository not found, return null
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager#getEnabled(java.net.URL)
	 */
	public boolean isEnabled(URL location) {
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			for (Iterator it = repositories.values().iterator(); it.hasNext();) {
				RepositoryInfo info = (RepositoryInfo) it.next();
				if (URLUtil.sameURL(info.location, location)) {
					return info.isEnabled;
				}
			}
			// Repository not found, return false
			return false;
		}
	}

	public IMetadataRepository loadRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		return loadRepository(location, monitor, null, true);
	}

	private IMetadataRepository loadRepository(URL location, IProgressMonitor monitor, String type, boolean signalAdd) throws ProvisionException {
		Assert.isNotNull(location);
		IMetadataRepository result = getRepository(location);
		if (result != null)
			return result;
		MultiStatus notFoundStatus = new MultiStatus(Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.repoMan_notExists, location.toExternalForm()), null);
		if (checkNotFound(location))
			throw new ProvisionException(notFoundStatus);
		String[] suffixes = sortSuffixes(getAllSuffixes(), location);
		SubMonitor sub = SubMonitor.convert(monitor, Messages.repoMan_adding, suffixes.length * 100);
		try {
			for (int i = 0; i < suffixes.length; i++) {
				result = loadRepository(location, suffixes[i], type, sub.newChild(100), notFoundStatus);
				if (result != null) {
					addRepository(result, signalAdd, suffixes[i]);
					return result;
				}
			}
		} finally {
			sub.done();
		}
		rememberNotFound(location);
		throw new ProvisionException(notFoundStatus);
	}

	/**
	 * Try to load a pre-existing repo at the given location
	 */
	private IMetadataRepository loadRepository(URL location, String suffix, String type, SubMonitor monitor, MultiStatus failures) {
		IExtension[] providers = findMatchingRepositoryExtensions(suffix, type);
		// Loop over the candidates and return the first one that successfully loads
		monitor.beginTask("", providers.length * 10); //$NON-NLS-1$
		for (int i = 0; i < providers.length; i++) {
			IMetadataRepositoryFactory factory = (IMetadataRepositoryFactory) createExecutableExtension(providers[i], FACTORY);
			try {
				if (factory != null)
					return factory.load(location, monitor.newChild(10));
			} catch (ProvisionException e) {
				if (e.getStatus().getCode() != ProvisionException.REPOSITORY_NOT_FOUND)
					failures.add(e.getStatus());
				//keep trying with other factories
			}
		}
		return null;
	}

	protected void log(String message, Throwable t) {
		LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message, t));
	}

	private boolean matchesFlags(RepositoryInfo info, int flags) {
		if ((flags & REPOSITORIES_SYSTEM) == REPOSITORIES_SYSTEM)
			if (!info.isSystem)
				return false;
		if ((flags & REPOSITORIES_NON_SYSTEM) == REPOSITORIES_NON_SYSTEM)
			if (info.isSystem)
				return false;
		if ((flags & REPOSITORIES_DISABLED) == REPOSITORIES_DISABLED) {
			if (info.isEnabled)
				return false;
		} else {
			//ignore disabled repositories for all other flag types
			if (!info.isEnabled)
				return false;
		}
		if ((flags & REPOSITORIES_LOCAL) == REPOSITORIES_LOCAL)
			return "file".equals(info.location.getProtocol()); //$NON-NLS-1$
		return true;
	}

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener#notify(java.util.EventObject)
	 */
	public void notify(EventObject o) {
		if (o instanceof RepositoryEvent) {
			RepositoryEvent event = (RepositoryEvent) o;
			if (event.getKind() == RepositoryEvent.DISCOVERED && event.getRepositoryType() == IRepository.TYPE_METADATA)
				addRepository(event.getRepositoryLocation(), event.isRepositoryEnabled());
		}
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

	public IMetadataRepository refreshRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		clearNotFound(location);
		if (!removeRepository(location))
			fail(location, ProvisionException.REPOSITORY_NOT_FOUND);
		return loadRepository(location, monitor, null, false);
	}

	/*
	 * Save the list of repositories in the preference store.
	 */
	private boolean remember(IMetadataRepository repository, String suffix) {
		boolean changed = false;
		Preferences node = getPreferences().node(getKey(repository));
		changed |= putValue(node, KEY_URL, repository.getLocation().toExternalForm());
		changed |= putValue(node, KEY_DESCRIPTION, repository.getDescription());
		changed |= putValue(node, KEY_NAME, repository.getName());
		changed |= putValue(node, KEY_PROVIDER, repository.getProvider());
		changed |= putValue(node, KEY_TYPE, repository.getType());
		changed |= putValue(node, KEY_VERSION, repository.getVersion());
		changed |= putValue(node, KEY_SYSTEM, (String) repository.getProperties().get(IRepository.PROP_SYSTEM));
		changed |= putValue(node, KEY_SUFFIX, suffix);
		if (changed)
			saveToPreferences();
		return changed;
	}

	/*
	 * Save the list of repositories in the preference store.
	 */
	private boolean remember(RepositoryInfo info) {
		boolean changed = false;
		Preferences node = getPreferences().node(getKey(info.location));
		changed |= putValue(node, KEY_URL, info.location.toExternalForm());
		changed |= putValue(node, KEY_SYSTEM, Boolean.toString(info.isSystem));
		changed |= putValue(node, KEY_DESCRIPTION, info.description);
		changed |= putValue(node, KEY_NAME, info.name);
		changed |= putValue(node, KEY_SUFFIX, info.suffix);
		changed |= putValue(node, KEY_ENABLED, Boolean.toString(info.isEnabled));
		if (changed)
			saveToPreferences();
		return changed;
	}

	/**
	 * Cache the fact that we tried to load a repository at this location and did not find anything.
	 */
	private void rememberNotFound(URL location) {
		List badRepos;
		if (unavailableRepositories != null) {
			badRepos = (List) unavailableRepositories.get();
			if (badRepos != null) {
				badRepos.add(location);
				return;
			}
		}
		badRepos = new ArrayList();
		badRepos.add(location);
		unavailableRepositories = new SoftReference(badRepos);
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
		broadcastChangeEvent(toRemove, IRepository.TYPE_METADATA, RepositoryEvent.REMOVED);
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
				info.isEnabled = child.getBoolean(KEY_ENABLED, true);
				info.suffix = child.get(KEY_SUFFIX, null);
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

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager#setEnabled(java.net.URL, boolean)
	 */
	public void setEnabled(URL location, boolean enablement) {
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			RepositoryInfo info = (RepositoryInfo) repositories.get(getKey(location));
			if (info == null || info.isEnabled == enablement)
				return;
			info.isEnabled = enablement;
			remember(info);
		}
	}

	/**
	 * Optimize the order in which repository suffixes are searched by trying 
	 * the last successfully loaded suffix first.
	 */
	private String[] sortSuffixes(String[] suffixes, URL location) {
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			RepositoryInfo info = (RepositoryInfo) repositories.get(getKey(location));
			if (info == null || info.suffix == null)
				return suffixes;
			//move lastSuffix to the front of the list but preserve order of remaining entries
			String lastSuffix = info.suffix;
			for (int i = 0; i < suffixes.length; i++) {
				if (lastSuffix.equals(suffixes[i])) {
					System.arraycopy(suffixes, 0, suffixes, 1, i);
					suffixes[0] = lastSuffix;
					return suffixes;
				}
			}
		}
		return suffixes;
	}

	public IStatus validateRepositoryLocation(URL location, IProgressMonitor monitor) {
		Assert.isNotNull(location);
		IMetadataRepository result = getRepository(location);
		if (result != null)
			return Status.OK_STATUS;
		String[] suffixes = getAllSuffixes();
		SubMonitor sub = SubMonitor.convert(monitor, Messages.repo_loading, suffixes.length * 100);
		IStatus status = new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.repoMan_notExists, location.toExternalForm()), null);
		for (int i = 0; i < suffixes.length; i++) {
			SubMonitor loopMonitor = sub.newChild(100);
			IExtension[] providers = findMatchingRepositoryExtensions(suffixes[i], null);
			// Loop over the candidates and return the first one that successfully loads
			loopMonitor.beginTask("", providers.length * 10); //$NON-NLS-1$
			for (int j = 0; j < providers.length; j++) {
				IMetadataRepositoryFactory factory = (IMetadataRepositoryFactory) createExecutableExtension(providers[j], FACTORY);
				if (factory != null) {
					status = factory.validate(location, loopMonitor.newChild(10));
					if (status.isOK()) {
						sub.done();
						return status;
					}
				}
			}

		}
		sub.done();
		return status;
	}

}
