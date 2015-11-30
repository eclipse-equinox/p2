/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Wind River - fix for bug 299227
 *     Sonatype, Inc. - transport split
 *     Red Hat,Inc. - fixes for bugs 249133, 460967
 *     Ericsson AB (Pascal Rapicault) - reading preferences from base in shared install
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository.helpers;

import java.io.*;
import java.lang.ref.SoftReference;
import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.repository.Activator;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.core.spi.IAgentService;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Common code shared between artifact and metadata repository managers.
 */
public abstract class AbstractRepositoryManager<T> implements IRepositoryManager<T>, IAgentService, ProvisioningListener {
	protected static class RepositoryInfo<R> {
		public String description;
		public boolean isEnabled = true;
		public boolean isSystem = false;
		public URI location;
		public String name;
		public String nickname;
		public SoftReference<IRepository<R>> repository;
		public String suffix;

		public RepositoryInfo() {
			super();
		}
	}

	public static final String ATTR_SUFFIX = "suffix"; //$NON-NLS-1$
	public static final String EL_FACTORY = "factory"; //$NON-NLS-1$
	public static final String EL_FILTER = "filter"; //$NON-NLS-1$
	public static final String KEY_DESCRIPTION = "description"; //$NON-NLS-1$
	public static final String KEY_ENABLED = "enabled"; //$NON-NLS-1$
	public static final String KEY_NAME = "name"; //$NON-NLS-1$
	public static final String KEY_NICKNAME = "nickname"; //$NON-NLS-1$
	public static final String KEY_PROVIDER = "provider"; //$NON-NLS-1$
	public static final String KEY_SUFFIX = "suffix"; //$NON-NLS-1$
	public static final String KEY_SYSTEM = "isSystem"; //$NON-NLS-1$
	public static final String KEY_TYPE = "type"; //$NON-NLS-1$
	public static final String KEY_URI = "uri"; //$NON-NLS-1$
	public static final String KEY_URL = "url"; //$NON-NLS-1$
	public static final String KEY_VERSION = "version"; //$NON-NLS-1$

	public static final String NODE_REPOSITORIES = "repositories"; //$NON-NLS-1$
	private static final String INDEX_FILE = "p2.index"; //$NON-NLS-1$

	/**
	 * Map of String->RepositoryInfo, where String is the repository key
	 * obtained via getKey(URI).
	 */
	protected Map<String, RepositoryInfo<T>> repositories = null;

	//lock object to be held when referring to the repositories field
	protected final Object repositoryLock = new Object();

	/**
	 * Cache List of repositories that are not reachable. Maintain cache
	 * for short duration because repository may become available at any time.
	 */
	protected SoftReference<List<URI>> unavailableRepositories;

	/**
	 * Set used to manage exclusive load locks on repository locations.
	 */
	private final Map<URI, Thread> loadLocks = new HashMap<URI, Thread>();
	private final IAgentLocation agentLocation;
	protected final IProvisioningEventBus eventBus;
	protected final IProvisioningAgent agent;

	protected AbstractRepositoryManager(IProvisioningAgent agent) {
		super();
		this.agent = agent;
		agentLocation = (IAgentLocation) agent.getService(IAgentLocation.SERVICE_NAME);
		eventBus = (IProvisioningEventBus) agent.getService(IProvisioningEventBus.SERVICE_NAME);
		eventBus.addListener(this);
	}

	/**
	 * Adds a repository to the list of known repositories
	 * @param repository the repository object to add
	 * @param signalAdd whether a repository change event should be fired
	 * @param suffix the suffix used to load the repository, or <code>null</code> if unknown
	 */
	protected void addRepository(IRepository<T> repository, boolean signalAdd, String suffix) {
		boolean added = false;
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			String key = getKey(repository.getLocation());
			RepositoryInfo<T> info = repositories.get(key);
			if (info == null) {
				info = new RepositoryInfo<T>();
				added = true;
				repositories.put(key, info);
			}
			info.repository = new SoftReference<IRepository<T>>(repository);
			info.name = repository.getName();
			info.description = repository.getDescription();
			info.location = repository.getLocation();
			String value = repository.getProperties().get(IRepository.PROP_SYSTEM);
			if (value != null)
				info.isSystem = Boolean.parseBoolean(value);
			info.suffix = suffix;
		}
		// save the given repository in the preferences.
		remember(repository, suffix);
		if (added && signalAdd)
			broadcastChangeEvent(repository.getLocation(), getRepositoryType(), RepositoryEvent.ADDED, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager#addRepository(java.net.URI)
	 */
	public void addRepository(URI location) {
		checkValidLocation(location);
		//add the repository, or enable it if already known
		if (!addRepository(location, true, true))
			setEnabled(location, true);
	}

	/**
	 * Adds the repository to the list of known repositories. 
	 * @param location The repository location
	 * @param isEnabled Whether the repository should be enabled
	 * @param signalAdd Whether a repository add event should be broadcast
	 * @return <code>true</code> if the repository was actually added, and 
	 * <code>false</code> otherwise.
	 */
	private boolean addRepository(URI location, boolean isEnabled, boolean signalAdd) {
		RepositoryInfo<T> info = new RepositoryInfo<T>();
		info.location = location;
		info.isEnabled = isEnabled;
		boolean added = true;
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			if (contains(location))
				return false;
			added = repositories.put(getKey(location), info) == null;
			// save the given repository in the preferences.
			remember(info, true);
		}
		if (added && signalAdd)
			broadcastChangeEvent(location, getRepositoryType(), RepositoryEvent.ADDED, isEnabled);
		return added;
	}

	protected IRepository<T> basicGetRepository(URI location) {
		checkValidLocation(location);
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			RepositoryInfo<T> info = repositories.get(getKey(location));
			if (info == null || info.repository == null)
				return null;
			IRepository<T> repo = info.repository.get();
			//update our repository info because the repository may have changed
			if (repo != null)
				addRepository(repo, false, info.suffix);
			return repo;
		}
	}

	public IRepository<T> basicRefreshRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
		checkValidLocation(location);
		clearNotFound(location);
		boolean wasEnabled = isEnabled(location);
		String nick = getRepositoryProperty(location, IRepository.PROP_NICKNAME);
		//remove the repository so  event is broadcast and repositories can clear their caches
		if (!removeRepository(location))
			fail(location, ProvisionException.REPOSITORY_NOT_FOUND);
		boolean loaded = false;
		try {
			IRepository<T> result = loadRepository(location, monitor, null, 0);
			loaded = true;
			setEnabled(location, wasEnabled);
			return result;
		} finally {
			//if we failed to load, make sure the repository is not lost
			if (!loaded)
				addRepository(location, wasEnabled, true);
			if (nick != null)
				setRepositoryProperty(location, IRepository.PROP_NICKNAME, nick);
		}
	}

	private void broadcastChangeEvent(URI location, int repositoryType, int kind, boolean isEnabled) {
		if (eventBus != null)
			eventBus.publishEvent(new RepositoryEvent(location, repositoryType, kind, isEnabled));
	}

	/**
	 * Check if we recently attempted to load the given location and failed
	 * to find anything. Returns <code>true</code> if the repository was not
	 * found, and <code>false</code> otherwise.
	 */
	private boolean checkNotFound(URI location) {
		if (unavailableRepositories == null)
			return false;
		List<URI> badRepos = unavailableRepositories.get();
		if (badRepos == null)
			return false;
		return badRepos.contains(location);
	}

	/**
	 * Clear the fact that we tried to load a repository at this location and did not find anything.
	 */
	private void clearNotFound(URI location) {
		List<URI> badRepos;
		if (unavailableRepositories != null) {
			badRepos = unavailableRepositories.get();
			if (badRepos != null) {
				badRepos.remove(location);
				return;
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager#contains(java.net.URI)
	 */
	public boolean contains(URI location) {
		checkValidLocation(location);
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			return repositories.containsKey(getKey(location));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager#createRepository(java.net.URL, java.lang.String, java.lang.String, java.util.Map)
	 */
	protected IRepository<T> doCreateRepository(URI location, String name, String type, Map<String, String> properties) throws ProvisionException {
		checkValidLocation(location);
		Assert.isNotNull(name);
		Assert.isNotNull(type);
		IRepository<T> result = null;
		try {
			enterLoad(location, new NullProgressMonitor());
			boolean loaded = false;
			try {
				//repository should not already exist
				loadRepository(location, (IProgressMonitor) null, type, 0);
				loaded = true;
			} catch (ProvisionException e) {
				//expected - fall through and create the new repository
			}
			if (loaded)
				fail(location, ProvisionException.REPOSITORY_EXISTS);

			IExtension extension = RegistryFactory.getRegistry().getExtension(getRepositoryProviderExtensionPointId(), type);
			if (extension == null)
				fail(location, ProvisionException.REPOSITORY_UNKNOWN_TYPE);
			//		MetadataRepositoryFactory factory = (MetadataRepositoryFactory) createExecutableExtension(extension, EL_FACTORY);
			//		if (factory == null)
			//			fail(location, ProvisionException.REPOSITORY_FAILED_READ);
			result = factoryCreate(location, name, type, properties, extension);
			if (result == null)
				fail(location, ProvisionException.REPOSITORY_FAILED_READ);
			clearNotFound(location);
			addRepository(result, false, null);
		} finally {
			exitLoad(location);
		}
		//fire event after releasing load lock
		broadcastChangeEvent(location, getRepositoryType(), RepositoryEvent.ADDED, true);
		return result;
	}

	/**
	 * Returns the executable extension, or <code>null</code> if there
	 * was no corresponding extension, or an error occurred loading it
	 */
	protected Object createExecutableExtension(IExtension extension, String element) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i].getName().equals(element)) {
				try {
					return elements[i].createExecutableExtension("class"); //$NON-NLS-1$
				} catch (CoreException e) {
					log("Error loading repository extension: " + extension.getUniqueIdentifier(), e); //$NON-NLS-1$
					return null;
				}
			}
		}
		log("Malformed repository extension: " + extension.getUniqueIdentifier(), null); //$NON-NLS-1$
		return null;
	}

	/**
	 * Obtains an exclusive right to load a repository at the given location. Blocks
	 * if another thread is currently loading at that location. Invocation of this
	 * method must be followed by a subsequent call to {@link #exitLoad(URI)}.
	 * 
	 * To avoid deadlock between the loadLock and repositoryLock, this method
	 * must not be called when repositoryLock is held.
	 * 
	 * @param location The location to lock
	 */
	private void enterLoad(URI location, IProgressMonitor monitor) {
		Thread current = Thread.currentThread();
		synchronized (loadLocks) {
			while (true) {
				Thread owner = loadLocks.get(location);
				if (owner == null || current.equals(owner))
					break;
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				try {
					loadLocks.wait(1000);
				} catch (InterruptedException e) {
					//keep trying
				}
			}
			loadLocks.put(location, current);
		}
	}

	/**
	 * Relinquishes the exclusive right to load a repository at the given location. Unblocks
	 * other threads waiting to load at that location.
	 * @param location The location to unlock
	 */
	private void exitLoad(URI location) {
		synchronized (loadLocks) {
			loadLocks.remove(location);
			loadLocks.notifyAll();
		}
	}

	/**
	 * Creates and returns a repository using the given repository factory extension. Returns
	 * null if no factory could be found associated with that extension.
	 */
	protected abstract IRepository<T> factoryCreate(URI location, String name, String type, Map<String, String> properties, IExtension extension) throws ProvisionException;

	/**
	 * Loads and returns a repository using the given repository factory extension. Returns
	 * null if no factory could be found associated with that extension.
	 */
	protected abstract IRepository<T> factoryLoad(URI location, IExtension extension, int flags, SubMonitor monitor) throws ProvisionException;

	private void fail(URI location, int code) throws ProvisionException {
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
			case ProvisionException.REPOSITORY_FAILED_AUTHENTICATION :
				msg = NLS.bind(Messages.repoManAuthenticationFailedFor_0, location);
				break;
		}
		if (msg == null)
			msg = Messages.repoMan_internalError;
		throw new ProvisionException(new Status(IStatus.ERROR, getBundleId(), code, msg, null));
	}

	protected IExtension[] findMatchingRepositoryExtensions(String suffix, String type) {
		IConfigurationElement[] elt = null;
		if (type != null && type.length() > 0) {
			IExtension ext = RegistryFactory.getRegistry().getExtension(getRepositoryProviderExtensionPointId(), type);
			elt = (ext != null) ? ext.getConfigurationElements() : new IConfigurationElement[0];
		} else {
			elt = RegistryFactory.getRegistry().getConfigurationElementsFor(getRepositoryProviderExtensionPointId());
		}
		int count = 0;
		for (int i = 0; i < elt.length; i++) {
			if (EL_FILTER.equals(elt[i].getName())) {
				if (!suffix.equals(elt[i].getAttribute(ATTR_SUFFIX))) {
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

	protected String[] getAllSuffixes() {
		final IExtensionRegistry registry = RegistryFactory.getRegistry();
		if (registry == null) {
			log("Extension registry not found", new RuntimeException()); //$NON-NLS-1$
			return new String[0];
		}
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(getRepositoryProviderExtensionPointId());
		ArrayList<String> result = new ArrayList<String>(elements.length);
		result.add(getDefaultSuffix());
		for (int i = 0; i < elements.length; i++) {
			if (elements[i].getName().equals(EL_FILTER)) {
				String suffix = elements[i].getAttribute(ATTR_SUFFIX);
				if (!result.contains(suffix))
					result.add(suffix);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * Returns the bundle id of the bundle that provides the concrete repository manager
	 * @return a symbolic bundle id
	 */
	protected abstract String getBundleId();

	/**
	 * Returns the default repository suffix. This is used to ensure a particular
	 * repository type is preferred over all others.
	 */
	protected abstract String getDefaultSuffix();

	/*
	 * Return a string key based on the given repository location which
	 * is suitable for use as a preference node name.
	 * TODO: convert local file system URI to canonical form
	 */
	private String getKey(URI location) {
		String key = location.toString().replace('/', '_');
		//remove trailing slash
		if (key.endsWith("_")) //$NON-NLS-1$
			key = key.substring(0, key.length() - 1);
		return key;
	}

	public IProvisioningAgent getAgent() {
		return agent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager#getKnownRepositories(int)
	 */
	public URI[] getKnownRepositories(int flags) {
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			ArrayList<URI> result = new ArrayList<URI>();
			for (RepositoryInfo<T> info : repositories.values()) {
				if (matchesFlags(info, flags))
					result.add(info.location);
			}
			return result.toArray(new URI[result.size()]);
		}
	}

	/**
	 * Return the preference node which is the root for where we store the repository information.
	 * Returns <code>null</code> if no preferences are available
	 */
	Preferences getPreferences() {
		if (agentLocation == null)
			return null;
		IPreferencesService prefService = ServiceHelper.getService(Activator.getContext(), IPreferencesService.class);
		if (prefService == null)
			return null;
		try {
			//see ProfileScope for preference path format
			String locationString = EncodingUtils.encodeSlashes(agentLocation.getRootLocation().toString());
			return prefService.getRootNode().node("/profile/" + locationString + "/_SELF_/" + getBundleId() + '/' + NODE_REPOSITORIES); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	Preferences getSharedPreferences() {
		if (agentLocation == null)
			return null;
		IPreferencesService prefService = ServiceHelper.getService(Activator.getContext(), IPreferencesService.class);
		if (prefService == null)
			return null;
		try {
			//see ProfileScope for preference path format
			String locationString = EncodingUtils.encodeSlashes(agentLocation.getRootLocation().toString());
			return prefService.getRootNode().node("/profile/shared/" + locationString + "/_SELF_/" + getBundleId() + '/' + NODE_REPOSITORIES); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Restores a repository location from the preferences.
	 */
	private URI getRepositoryLocation(Preferences node) {
		//prefer the location stored in URI form
		String locationString = node.get(KEY_URI, null);
		try {
			if (locationString != null) {
				URI result = new URI(locationString);
				if (result.isAbsolute())
					return result;
				log("Invalid repository URI: " + locationString, new RuntimeException()); //$NON-NLS-1$
			}
		} catch (URISyntaxException e) {
			log("Error while restoring repository: " + locationString, e); //$NON-NLS-1$
		}
		//we used to store the repository as a URL, so try old key for backwards compatibility
		locationString = node.get(KEY_URL, null);
		try {
			if (locationString != null) {
				URI result = URIUtil.toURI(new URL(locationString));
				if (result.isAbsolute())
					return result;
				log("Invalid repository URL: " + locationString, new RuntimeException()); //$NON-NLS-1$
			}
		} catch (MalformedURLException e) {
			log("Error while restoring repository: " + locationString, e); //$NON-NLS-1$
		} catch (URISyntaxException e) {
			log("Error while restoring repository: " + locationString, e); //$NON-NLS-1$
		}
		return null;
	}

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager#getRepositoryProperty(java.net.URI, java.lang.String)
	 */
	public String getRepositoryProperty(URI location, String key) {
		checkValidLocation(location);
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			RepositoryInfo<T> info = repositories.get(getKey(location));
			if (info == null)
				return null;// Repository not found
			if (IRepository.PROP_DESCRIPTION.equals(key))
				return info.description;
			else if (IRepository.PROP_NAME.equals(key))
				return info.name;
			else if (IRepository.PROP_SYSTEM.equals(key))
				return Boolean.toString(info.isSystem);
			else if (IRepository.PROP_NICKNAME.equals(key))
				return info.nickname;
			// Key not known, return null
			return null;
		}
	}

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager#getRepositoryProperty(java.net.URI, java.lang.String)
	 */
	public void setRepositoryProperty(URI location, String key, String value) {
		checkValidLocation(location);
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			RepositoryInfo<T> info = repositories.get(getKey(location));
			if (info == null)
				return;// Repository not found
			if (IRepository.PROP_DESCRIPTION.equals(key))
				info.description = value;
			else if (IRepository.PROP_NAME.equals(key))
				info.name = value;
			else if (IRepository.PROP_NICKNAME.equals(key))
				info.nickname = value;
			else if (IRepository.PROP_SYSTEM.equals(key))
				//only true if value.equals("true") which is OK because a repository is only system if it's explicitly set to system.
				info.isSystem = Boolean.parseBoolean(value);
			remember(info, true);
		}
	}

	/**
	 * Returns the fully qualified id of the repository provider extension point.
	 */
	protected abstract String getRepositoryProviderExtensionPointId();

	/**
	 * Returns the system property used to specify additional repositories to be
	 * automatically added to the list of known repositories.
	 */
	protected abstract String getRepositorySystemProperty();

	/**
	 * Returns the repository type stored in this manager.
	 */
	protected abstract int getRepositoryType();

	/**
	 * Returns the preferred search order for this location
	 */
	protected abstract String[] getPreferredRepositorySearchOrder(LocationProperties properties);

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager#isEnabled(java.net.URI)
	 */
	public boolean isEnabled(URI location) {
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			RepositoryInfo<T> info = repositories.get(getKey(location));
			if (info != null)
				return info.isEnabled;
			// Repository not found, return false
			return false;
		}
	}

	protected IRepository<T> loadRepository(URI location, IProgressMonitor monitor, String type, int flags) throws ProvisionException {
		checkValidLocation(location);
		SubMonitor sub = SubMonitor.convert(monitor, 100);
		boolean added = false;
		IRepository<T> result = null;

		try {
			enterLoad(location, sub.newChild(5));
			result = basicGetRepository(location);
			if (result != null)
				return result;
			if (checkNotFound(location))
				fail(location, ProvisionException.REPOSITORY_NOT_FOUND);
			//add the repository first so that it will be enabled, but don't send add event until after the load
			added = addRepository(location, true, false);

			LocationProperties indexFile = loadIndexFile(location, sub.newChild(15));
			String[] preferredOrder = getPreferredRepositorySearchOrder(indexFile);
			String[] suffixes = sortSuffixes(getAllSuffixes(), location, preferredOrder);

			sub = SubMonitor.convert(sub, NLS.bind(Messages.repoMan_adding, location), suffixes.length * 100);
			ProvisionException failure = null;
			try {
				for (int i = 0; i < suffixes.length; i++) {
					if (sub.isCanceled())
						throw new OperationCanceledException();
					try {
						result = loadRepository(location, suffixes[i], type, flags, sub.newChild(100));
					} catch (ProvisionException e) {
						failure = e;
						break;
					}
					if (result != null) {
						addRepository(result, false, suffixes[i]);
						break;
					}
				}
			} finally {
				sub.done();
			}
			if (result == null) {
				//if we just added the repository, remove it because it cannot be loaded
				if (added)
					removeRepository(location, false);
				//eagerly cleanup missing system repositories
				if (Boolean.parseBoolean(getRepositoryProperty(location, IRepository.PROP_SYSTEM)))
					removeRepository(location);
				else if (failure == null || (failure.getStatus().getCode() != ProvisionException.REPOSITORY_FAILED_AUTHENTICATION && failure.getStatus().getCode() != ProvisionException.REPOSITORY_FAILED_READ))
					rememberNotFound(location);
				if (failure != null)
					throw failure;
				fail(location, ProvisionException.REPOSITORY_NOT_FOUND);
			}
		} finally {
			exitLoad(location);
		}
		//broadcast the add event after releasing lock
		if (added)
			broadcastChangeEvent(location, getRepositoryType(), RepositoryEvent.ADDED, true);
		return result;
	}

	/**
	 * Fetches the p2.index file from the server. If the file could not be fetched
	 * a NullSafe version is returned.
	 */
	private LocationProperties loadIndexFile(URI location, IProgressMonitor monitor) {
		LocationProperties locationProperties = LocationProperties.createEmptyIndexFile();
		//Handle the case of in-memory repos
		if (!isURL(location))
			return locationProperties;

		if ("file".equals(location.getScheme())) { //$NON-NLS-1$ 
			InputStream localStream = null;
			try {
				try {
					File indexFile = URIUtil.toFile(getIndexFileURI(location));
					if (indexFile != null && indexFile.exists() && indexFile.canRead()) {
						localStream = new FileInputStream(indexFile);
						locationProperties = LocationProperties.create(localStream);
					}
				} finally {
					if (localStream != null)
						localStream.close();
				}
			} catch (IOException e) {
				//do nothing.
			}
			return locationProperties;
		}

		//Handle non local repos (i.e. not file:)
		ByteArrayOutputStream index = new ByteArrayOutputStream();
		IStatus indexFileStatus = null;
		indexFileStatus = getTransport().download(getIndexFileURI(location), index, monitor);
		if (indexFileStatus != null && indexFileStatus.isOK())
			locationProperties = LocationProperties.create(new ByteArrayInputStream(index.toByteArray()));

		return locationProperties;
	}

	/**
	 * Basic sanity checking on location argument
	 */
	private URI checkValidLocation(URI location) {
		if (location == null)
			throw new IllegalArgumentException("Location cannot be null"); //$NON-NLS-1$
		if (!location.isAbsolute())
			throw new IllegalArgumentException("Location must be absolute: " + location); //$NON-NLS-1$
		return location;
	}

	private static boolean isURL(URI location) {
		try {
			new URL(location.toASCIIString());
		} catch (MalformedURLException e) {
			return false;
		}
		return true;
	}

	private IRepository<T> loadRepository(URI location, String suffix, String type, int flags, SubMonitor monitor) throws ProvisionException {
		IExtension[] providers = findMatchingRepositoryExtensions(suffix, type);
		// Loop over the candidates and return the first one that successfully loads
		monitor.beginTask(null, providers.length * 10);
		for (int i = 0; i < providers.length; i++)
			try {
				IRepository<T> repo = factoryLoad(location, providers[i], flags, monitor);
				if (repo != null)
					return repo;
			} catch (ProvisionException e) {
				if (e.getStatus().getCode() != ProvisionException.REPOSITORY_NOT_FOUND)
					throw e;
			} catch (OperationCanceledException e) {
				//always propagate cancelation
				throw e;
			} catch (Exception e) {
				//catch and log unexpected errors and move onto the next factory
				log("Unexpected error loading extension: " + providers[i].getUniqueIdentifier(), e); //$NON-NLS-1$
			} catch (LinkageError e) {
				//catch and log unexpected errors and move onto the next factory
				log("Unexpected error loading extension: " + providers[i].getUniqueIdentifier(), e); //$NON-NLS-1$
			}
		return null;
	}

	protected void log(String message, Throwable t) {
		LogHelper.log(new Status(IStatus.ERROR, getBundleId(), message, t));
	}

	private boolean matchesFlags(RepositoryInfo<T> info, int flags) {
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
			return "file".equals(info.location.getScheme()) || info.location.toString().startsWith("jar:file"); //$NON-NLS-1$ //$NON-NLS-2$
		if ((flags & REPOSITORIES_NON_LOCAL) == REPOSITORIES_NON_LOCAL)
			return !("file".equals(info.location.getScheme()) || info.location.toString().startsWith("jar:file")); //$NON-NLS-1$ //$NON-NLS-2$
		return true;
	}

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener#notify(java.util.EventObject)
	 */
	public void notify(EventObject o) {
		if (o instanceof RepositoryEvent) {
			RepositoryEvent event = (RepositoryEvent) o;
			if (event.getKind() == RepositoryEvent.DISCOVERED && event.getRepositoryType() == getRepositoryType())
				addRepository(event.getRepositoryLocation(), event.isRepositoryEnabled(), true);
		}
	}

	/**
	 * Sets a preference and returns <code>true</code> if the preference
	 * was actually changed.
	 */
	protected boolean putValue(Preferences node, String key, String newValue) {
		String oldValue = node.get(key, null);
		if (oldValue == newValue || (oldValue != null && oldValue.equals(newValue)))
			return false;
		if (newValue == null)
			node.remove(key);
		else
			node.put(key, newValue);
		return true;
	}

	/*
	 * Add the given repository object to the preferences and save.
	 */
	private void remember(IRepository<T> repository, String suffix) {
		boolean changed = false;
		Preferences node = getPreferences();
		// Ensure we retrieved preferences
		if (node == null)
			return;
		node = node.node(getKey(repository.getLocation()));

		try {
			changed |= putValue(node, KEY_URI, repository.getLocation().toString());
			changed |= putValue(node, KEY_URL, null);
			changed |= putValue(node, KEY_DESCRIPTION, repository.getDescription());
			changed |= putValue(node, KEY_NAME, repository.getName());
			changed |= putValue(node, KEY_PROVIDER, repository.getProvider());
			changed |= putValue(node, KEY_TYPE, repository.getType());
			changed |= putValue(node, KEY_VERSION, repository.getVersion());
			//allow repository manager to define system property if it is undefined in the repository itself
			String value = repository.getProperties().get(IRepository.PROP_SYSTEM);
			if (value != null)
				changed |= putValue(node, KEY_SYSTEM, value);
			changed |= putValue(node, KEY_SUFFIX, suffix);
			if (changed)
				saveToPreferences();
		} catch (IllegalStateException e) {
			//the repository was removed concurrently, so we don't need to save it
		}
	}

	/**
	 * Writes the state of the repository information into the appropriate preference node.
	 * 
	 * @param info The info to write to the preference node
	 * @param flush <code>true</code> if the preference node should be flushed to
	 * disk, and <code>false</code> otherwise
	 */
	private boolean remember(RepositoryInfo<T> info, boolean flush) {
		boolean changed = false;
		Preferences node = getPreferences();
		// Ensure we retrieved preferences
		if (node == null)
			return changed;
		node = node.node(getKey(info.location));
		try {
			changed |= putValue(node, KEY_URI, info.location.toString());
			changed |= putValue(node, KEY_URL, null);
			changed |= putValue(node, KEY_SYSTEM, Boolean.toString(info.isSystem));
			changed |= putValue(node, KEY_DESCRIPTION, info.description);
			changed |= putValue(node, KEY_NAME, info.name);
			changed |= putValue(node, KEY_NICKNAME, info.nickname);
			changed |= putValue(node, KEY_SUFFIX, info.suffix);
			changed |= putValue(node, KEY_ENABLED, Boolean.toString(info.isEnabled));
			if (changed && flush)
				saveToPreferences();
			return changed;
		} catch (IllegalStateException e) {
			//the repository was removed concurrently, so we don't need to save it
			return false;
		}
	}

	/**
	 * Cache the fact that we tried to load a repository at this location and did not find anything.
	 */
	private void rememberNotFound(URI location) {
		List<URI> badRepos;
		if (unavailableRepositories != null) {
			badRepos = unavailableRepositories.get();
			if (badRepos != null) {
				badRepos.add(location);
				return;
			}
		}
		badRepos = new ArrayList<URI>();
		badRepos.add(location);
		unavailableRepositories = new SoftReference<List<URI>>(badRepos);
	}

	public boolean removeRepository(URI toRemove) {
		return removeRepository(checkValidLocation(toRemove), true);
	}

	private boolean removeRepository(URI toRemove, boolean signalRemove) {
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
			if (Tracing.DEBUG_REMOVE_REPO) {
				String msg = "Removing repository: " + toRemove; //$NON-NLS-1$
				Tracing.debug(msg);
				new Exception(msg).printStackTrace();
			}
			Preferences node = getPreferences();
			if (node != null) {
				node.node(repoKey).removeNode();
				saveToPreferences();
			}
			clearNotFound(toRemove);
		} catch (BackingStoreException e) {
			log("Error saving preferences", e); //$NON-NLS-1$
		}
		//TODO: compute and pass appropriate isEnabled flag
		if (signalRemove)
			broadcastChangeEvent(toRemove, getRepositoryType(), RepositoryEvent.REMOVED, true);
		return true;
	}

	/*
	 * Load the list of repositories from the preferences.
	 */
	private void basicRestoreFromPreferences(Preferences node, boolean save) {
		// restore the list of repositories from the preference store
		if (node == null)
			return;
		String[] children;
		try {
			children = node.childrenNames();
		} catch (BackingStoreException e) {
			log("Error restoring repositories from preferences", e); //$NON-NLS-1$
			return;
		}
		for (int i = 0; i < children.length; i++) {
			Preferences child = node.node(children[i]);
			URI location = getRepositoryLocation(child);
			if (location == null) {
				try {
					child.removeNode();
					continue;
				} catch (BackingStoreException e) {
					log("Error removing invalid repository", e); //$NON-NLS-1$
				}
			}
			RepositoryInfo<T> info = new RepositoryInfo<T>();
			info.location = location;
			info.name = child.get(KEY_NAME, null);
			info.nickname = child.get(KEY_NICKNAME, null);
			info.description = child.get(KEY_DESCRIPTION, null);
			info.isSystem = child.getBoolean(KEY_SYSTEM, false);
			info.isEnabled = child.getBoolean(KEY_ENABLED, true);
			info.suffix = child.get(KEY_SUFFIX, null);
			repositories.put(getKey(info.location), info);
		}
		// now that we have loaded everything, remember them
		if (save)
			saveToPreferences();
	}

	private void restoreFromSystemProperty() {
		String locationString = Activator.getContext().getProperty(getRepositorySystemProperty());
		if (locationString != null) {
			StringTokenizer tokenizer = new StringTokenizer(locationString, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				try {
					addRepository(new URI(tokenizer.nextToken()), true, true);
				} catch (URISyntaxException e) {
					log("Error while restoring repository " + locationString, e); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Restores the repository list.
	 */
	private void restoreRepositories() {
		synchronized (repositoryLock) {
			repositories = new HashMap<String, RepositoryInfo<T>>();
			restoreSpecialRepositories();
			restoreFromSystemProperty();
			basicRestoreFromPreferences(getSharedPreferences(), false);
			basicRestoreFromPreferences(getPreferences(), true);
		}
	}

	/**
	 * Hook method to restore special additional repositories.
	 */
	protected void restoreSpecialRepositories() {
		//by default no special repositories
	}

	/*
	 * Save the list of repositories to the file-system.
	 */
	private void saveToPreferences() {
		try {
			Preferences node = getPreferences();
			if (node != null)
				node.flush();
		} catch (BackingStoreException e) {
			log("Error while saving repositories in preferences", e); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager#setEnabled(java.net.URI, boolean)
	 */
	public void setEnabled(URI location, boolean enablement) {
		checkValidLocation(location);
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			RepositoryInfo<T> info = repositories.get(getKey(location));
			if (info == null || info.isEnabled == enablement)
				return;
			info.isEnabled = enablement;
			remember(info, true);
		}
		broadcastChangeEvent(location, getRepositoryType(), RepositoryEvent.ENABLEMENT, enablement);
	}

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.p2.core.spi.IAgentService#start()
	 */
	public void start() {
		//nothing to do
	}

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.p2.core.spi.IAgentService#stop()
	 */
	public void stop() {
		eventBus.removeListener(this);
		//ensure all repository state in memory is written to disk
		boolean changed = false;
		synchronized (repositoryLock) {
			if (repositories != null) {
				for (RepositoryInfo<T> info : repositories.values()) {
					changed |= remember(info, false);
				}
			}
		}
		if (changed) {
			if (Tracing.DEBUG)
				Tracing.debug("Unsaved preferences when shutting down " + getClass().getName()); //$NON-NLS-1$
			saveToPreferences();
		}
		repositories = null;
		unavailableRepositories = null;
	}

	/**
	 * Optimize the order in which repository suffixes are searched by trying 
	 * the last successfully loaded suffix first.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected String[] sortSuffixes(String[] suffixes, URI location, String[] preferredOrder) {
		String[] result = new String[suffixes.length];
		System.arraycopy(suffixes, 0, result, 0, suffixes.length);

		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			RepositoryInfo<T> info = repositories.get(getKey(location));
			if (info != null && info.suffix != null) {
				//move lastSuffix to the front of the list but preserve order of remaining entries
				String lastSuffix = info.suffix;
				for (int i = 0; i < result.length; i++) {
					if (lastSuffix.equals(result[i])) {
						System.arraycopy(result, 0, result, 1, i);
						result[0] = lastSuffix;
						break;
					}
				}
			}
			// Now make sure that anything in the "preferredOrder" is at the top
			if (preferredOrder != null) {
				int priority = 0;
				for (int i = 0; i < preferredOrder.length; i++) {
					String currentSuffix = preferredOrder[i];
					if (LocationProperties.END.equals(currentSuffix.trim())) {
						// All suffixes from here on should be ignored
						String[] tmp = new String[priority];
						System.arraycopy(result, 0, tmp, 0, priority);
						return tmp;
					}
					for (int j = priority; j < result.length; j++) {
						if (result[j].equalsIgnoreCase(currentSuffix.trim())) {
							String tmp = result[j];
							System.arraycopy(result, priority, result, priority + 1, j - priority);
							result[priority] = tmp;
							priority++;
							break;
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * Performs a query against the contents of each known 
	 * repository, accumulating any objects that satisfy the query in the 
	 * provided collector.
	 * <p>
	 * Note that using this method can be quite expensive, as every known
	 * repository will be loaded in order to query each one.  If a
	 * client wishes to query only certain repositories, it is better to use
	 * {@link #getKnownRepositories(int)} to filter the list of repositories
	 * loaded and then query each of the returned repositories.
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor. 
	 * 
	 * @param query The query to perform against each element in each known repository
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return A collector containing the results of the query
	 */
	public IQueryResult<T> query(IQuery<T> query, IProgressMonitor monitor) {
		URI[] locations = getKnownRepositories(REPOSITORIES_ALL);
		List<IRepository<T>> queryables = new ArrayList<IRepository<T>>(locations.length); // use a list since we don't know exactly how many will load
		SubMonitor sub = SubMonitor.convert(monitor, locations.length * 10);
		for (int i = 0; i < locations.length; i++) {
			try {
				if (sub.isCanceled())
					throw new OperationCanceledException();
				queryables.add(loadRepository(locations[i], sub.newChild(9), null, 0));
			} catch (ProvisionException e) {
				//ignore this repository for this query
			}
		}
		try {
			IQueryable<T> compoundQueryable = QueryUtil.compoundQueryable(queryables);
			return compoundQueryable.query(query, sub.newChild(locations.length * 1));
		} finally {
			sub.done();
		}
	}

	private static URI getIndexFileURI(URI base) {
		final String name = INDEX_FILE;
		String spec = base.toString();
		if (spec.endsWith(name))
			return base;
		return URIUtil.append(base, name);
	}

	protected Transport getTransport() {
		return (Transport) agent.getService(Transport.SERVICE_NAME);
	}

	public void flushCache() {
		synchronized (repositoryLock) {
			if (repositories != null) {
				Collection<RepositoryInfo<T>> repos = repositories.values();
				for (Iterator<RepositoryInfo<T>> iterator = repos.iterator(); iterator.hasNext();) {
					RepositoryInfo<T> repositoryInfo = iterator.next();
					repositoryInfo.repository = null;
				}
			}
		}
	}
}
