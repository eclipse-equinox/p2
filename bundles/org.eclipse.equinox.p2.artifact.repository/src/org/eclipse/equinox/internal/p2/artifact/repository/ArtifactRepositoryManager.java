/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.IArtifactRepositoryFactory;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

// TODO Need to react to repository going away 
// TODO the current assumption that the "location" is the dir/root limits us to 
// having just one repository in a given URL..  
public class ArtifactRepositoryManager implements IArtifactRepositoryManager {
	static class RepositoryInfo {
		String description;
		boolean isSystem = false;
		URL location;
		String name;
		SoftReference repository;
	}

	private static final String EL_FILTER = "filter"; //$NON-NLS-1$
	private static final String ATTR_SUFFIX = "suffix"; //$NON-NLS-1$
	private static final String EL_FACTORY = "factory"; //$NON-NLS-1$

	private static final String KEY_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String KEY_NAME = "name"; //$NON-NLS-1$
	private static final String KEY_PROVIDER = "provider"; //$NON-NLS-1$
	private static final String KEY_SYSTEM = "isSystem"; //$NON-NLS-1$
	private static final String KEY_TYPE = "type"; //$NON-NLS-1$
	private static final String KEY_URL = "url"; //$NON-NLS-1$
	private static final String KEY_VERSION = "version"; //$NON-NLS-1$
	private static final String NODE_REPOSITORIES = "repositories"; //$NON-NLS-1$
	private static final String DEFAULT_SUFFIX = "artifacts.xml"; //$NON-NLS-1$

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

	public ArtifactRepositoryManager() {
		//initialize repositories lazily
	}

	public void addRepository(IArtifactRepository repository) {
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

	public IArtifactRequest createDownloadRequest(IArtifactKey key, IPath destination) {
		return new FileDownloadRequest(key, destination);
	}

	private Object createExecutableExtension(IExtension extension, String element) throws ProvisionException {
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

	public IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination) {
		return createMirrorRequest(key, destination, null, null);
	}

	public IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination, Properties destinationDescriptorProperties, Properties destinationRepositoryProperties) {
		return new MirrorRequest(key, destination, destinationDescriptorProperties, destinationRepositoryProperties);
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

	public IArtifactRepository createRepository(URL location, String name, String type) throws ProvisionException {
		try {
			loadRepository(location, (IProgressMonitor) null);
			fail(location, ProvisionException.REPOSITORY_EXISTS);
		} catch (ProvisionException e) {
			//expected - fall through and create a new repository
		}
		IExtension extension = RegistryFactory.getRegistry().getExtension(Activator.REPO_PROVIDER_XPT, type);
		if (extension == null)
			fail(location, ProvisionException.REPOSITORY_UNKNOWN_TYPE);
		IArtifactRepositoryFactory factory = (IArtifactRepositoryFactory) createExecutableExtension(extension, EL_FACTORY);
		if (factory == null)
			fail(location, ProvisionException.REPOSITORY_FAILED_READ);
		IArtifactRepository result = factory.create(location, name, type);
		if (result == null)
			fail(location, ProvisionException.REPOSITORY_FAILED_READ);
		clearNotFound(result.getLocation());
		return result;
	}

	private IExtension[] findMatchingRepositoryExtensions(String suffix) {
		IConfigurationElement[] elt = RegistryFactory.getRegistry().getConfigurationElementsFor(Activator.REPO_PROVIDER_XPT);
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
	 * Return an encoded repository key that is suitable for using
	 * as the name of a preference node.
	 */
	private String getKey(IArtifactRepository repository) {
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
	 * Return the root preference node where we store the repository information.
	 */
	private Preferences getPreferences() {
		return new ConfigurationScope().getNode(Activator.ID).node(NODE_REPOSITORIES);
	}

	public IArtifactRepository getRepository(URL location) {
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			for (Iterator it = repositories.values().iterator(); it.hasNext();) {
				RepositoryInfo info = (RepositoryInfo) it.next();
				if (URLUtil.sameURL(info.location, location)) {
					if (info.repository == null)
						return null;
					IArtifactRepository repo = (IArtifactRepository) info.repository.get();
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
	 * @see org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager#getRepositoryProperty(java.net.URL, java.lang.String)
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

	public IArtifactRepository loadRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		// TODO do something with the monitor
		IArtifactRepository result = getRepository(location);
		if (result != null)
			return result;
		if (checkNotFound(location))
			fail(location, ProvisionException.REPOSITORY_NOT_FOUND);
		String[] suffixes = getAllSuffixes();
		SubMonitor sub = SubMonitor.convert(monitor, suffixes.length * 100);
		for (int i = 0; i < suffixes.length; i++) {
			result = loadRepository(location, suffixes[i], sub.newChild(100));
			if (result != null) {
				addRepository(result);
				return result;
			}
		}
		rememberNotFound(location);
		fail(location, ProvisionException.REPOSITORY_NOT_FOUND);
		return null;
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

	private IArtifactRepository loadRepository(URL location, String suffix, SubMonitor monitor) {
		IExtension[] providers = findMatchingRepositoryExtensions(suffix);
		// Loop over the candidates and return the first one that successfully loads
		monitor.beginTask("", providers.length * 10); //$NON-NLS-1$
		for (int i = 0; i < providers.length; i++)
			try {
				IArtifactRepositoryFactory factory = (IArtifactRepositoryFactory) createExecutableExtension(providers[i], EL_FACTORY);
				if (factory != null)
					return factory.load(location, monitor.newChild(10));
			} catch (CoreException e) {
				if (e.getStatus().getCode() != ProvisionException.REPOSITORY_NOT_FOUND)
					log("Unable to load repository: " + location, e); //$NON-NLS-1$
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

	/*
	 * Add the given repository object to the preferences and save.
	 */
	private void remember(IArtifactRepository repository) {
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

	private void restoreDownloadCache() {
		// TODO while recreating, we may want to have proxies on repo instead of the real repo object to limit what is activated.
		AgentLocation location = (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
		if (location == null)
			// TODO should do something here since we are failing to restore.
			return;
		try {
			SimpleArtifactRepository cache = (SimpleArtifactRepository) createRepository(location.getArtifactRepositoryURL(), "download cache", TYPE_SIMPLE_REPOSITORY); //$NON-NLS-1$
			addRepository(cache);
			cache.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
		} catch (ProvisionException e) {
			LogHelper.log(e);
		}
	}

	/*
	 * Load the list of repositories from the preferences.
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
		// now that we have loaded everything, remember them
		saveToPreferences();
	}

	private void restoreFromSystemProperty() {
		String locationString = Activator.getContext().getProperty("eclipse.p2.artifactRepository"); //$NON-NLS-1$
		if (locationString != null) {
			StringTokenizer tokenizer = new StringTokenizer(locationString, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				try {
					addRepository(new URL(tokenizer.nextToken()));
				} catch (MalformedURLException e) {
					log("Error while restoring repository " + locationString, e); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Restores the repository list.
	 */
	protected void restoreRepositories() {
		synchronized (repositoryLock) {
			repositories = new HashMap();
			restoreDownloadCache();
			restoreFromSystemProperty();
			restoreFromPreferences();
		}
	}

	/*
	 * Save the list of repositories to the file-system.
	 */
	private void saveToPreferences() {
		try {
			getPreferences().flush();
		} catch (BackingStoreException e) {
			log("Error while saving repositories in preferences", e); //$NON-NLS-1$
		}
	}
}
