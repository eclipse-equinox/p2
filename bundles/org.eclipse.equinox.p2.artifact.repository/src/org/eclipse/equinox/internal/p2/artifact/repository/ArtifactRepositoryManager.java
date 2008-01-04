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
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.spi.p2.artifact.repository.IArtifactRepositoryFactory;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

// TODO Need to react to repository going away 
// TODO the current assumption that the "location" is the dir/root limits us to 
// having just one repository in a given URL..  
public class ArtifactRepositoryManager implements IArtifactRepositoryManager {
	static class RepositoryInfo {
		String description;
		URL location;
		String name;
		SoftReference repository;
	}

	private static final String ATTR_FILTER = "filter"; //$NON-NLS-1$
	private static final String ATTR_SUFFIX = "suffix"; //$NON-NLS-1$
	private static final String EL_FACTORY = "factory"; //$NON-NLS-1$

	private static final String KEY_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String KEY_NAME = "name"; //$NON-NLS-1$
	private static final String KEY_PROVIDER = "provider"; //$NON-NLS-1$
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

	public ArtifactRepositoryManager() {
		//initialize repositories lazily
	}

	public void addRepository(IArtifactRepository repository) {
		RepositoryInfo info = new RepositoryInfo();
		info.repository = new SoftReference(repository);
		info.name = repository.getName();
		info.description = repository.getDescription();
		info.location = repository.getLocation();
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

	private Object createExecutableExtension(IExtension extension, String element) throws CoreException {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i].getName().equals(element))
				return elements[i].createExecutableExtension("class"); //$NON-NLS-1$
		}
		throw new CoreException(new Status(IStatus.ERROR, Activator.ID, "Malformed extension")); //$NON-NLS-1$
	}

	public IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination) {
		return createMirrorRequest(key, destination, null, null);
	}

	public IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination, Properties destinationDescriptorProperties, Properties destinationRepositoryProperties) {
		return new MirrorRequest(key, destination, destinationDescriptorProperties, destinationRepositoryProperties);
	}

	public IArtifactRepository createRepository(URL location, String name, String type) {
		IArtifactRepository result = loadRepository(location, (IProgressMonitor) null);
		if (result != null)
			return result;
		IExtension extension = RegistryFactory.getRegistry().getExtension(Activator.REPO_PROVIDER_XPT, type);
		if (extension == null)
			return null;
		try {
			IArtifactRepositoryFactory factory = (IArtifactRepositoryFactory) createExecutableExtension(extension, EL_FACTORY);
			if (factory == null)
				return null;
			result = factory.create(location, name, type);
			if (result != null)
				addRepository(result);
			return result;
		} catch (CoreException e) {
			log("Failed to load artifact repository extension: " + location, e); //$NON-NLS-1$
			return null;
		}
	}

	private IExtension[] findMatchingRepositoryExtensions(String suffix) {
		IConfigurationElement[] elt = RegistryFactory.getRegistry().getConfigurationElementsFor(Activator.REPO_PROVIDER_XPT);
		int count = 0;
		for (int i = 0; i < elt.length; i++) {
			if (ATTR_FILTER.equals(elt[i].getName())) {
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
		for (int i = 0; i < elements.length; i++)
			if (elements[i].getName().equals(ATTR_FILTER))
				result.add(elements[i].getAttribute(ATTR_SUFFIX));
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

	public URL[] getKnownRepositories() {
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			URL[] result = new URL[repositories.size()];
			int i = 0;
			for (Iterator it = repositories.values().iterator(); it.hasNext(); i++) {
				RepositoryInfo info = (RepositoryInfo) it.next();
				result[i] = info.location;
			}
			return result;
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
					return (IArtifactRepository) info.repository.get();
				}
			}
			return null;
		}
	}

	public IArtifactRepository loadRepository(URL location, IProgressMonitor monitor) {
		// TODO do some thing with the monitor
		IArtifactRepository result = getRepository(location);
		if (result != null)
			return result;
		String[] suffixes = getAllSuffixes();
		for (int i = 0; i < suffixes.length; i++) {
			result = loadRepository(location, suffixes[i]);
			if (result != null) {
				addRepository(result);
				return result;
			}
		}
		return null;
	}

	private IArtifactRepository loadRepository(URL location, String suffix) {
		IExtension[] providers = findMatchingRepositoryExtensions(suffix);
		// Loop over the candidates and return the first one that successfully loads
		for (int i = 0; i < providers.length; i++)
			try {
				IArtifactRepositoryFactory factory = (IArtifactRepositoryFactory) createExecutableExtension(providers[i], EL_FACTORY);
				if (factory != null)
					return factory.load(location);
			} catch (CoreException e) {
				log("Unable to load repository: " + location, e); //$NON-NLS-1$
			}
		return null;
	}

	protected void log(String message, Throwable t) {
		LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message, t));
	}

	/*
	 * Add the given repository object to the preferences and save.
	 */
	private void remember(IArtifactRepository repository) {
		Preferences node = getPreferences().node(getKey(repository));
		String value = repository.getDescription();
		if (value != null)
			node.put(KEY_DESCRIPTION, value);
		value = repository.getName();
		if (value != null)
			node.put(KEY_NAME, value);
		value = repository.getProvider();
		if (value != null)
			node.put(KEY_PROVIDER, value);
		value = repository.getType();
		if (value != null)
			node.put(KEY_TYPE, value);
		value = repository.getVersion();
		if (value != null)
			node.put(KEY_VERSION, value);
		value = repository.getLocation().toExternalForm();
		node.put(KEY_URL, value);
		saveToPreferences();
	}

	/*
	 * Save the list of repositories in the preference store.
	 */
	private void remember(RepositoryInfo info) {
		Preferences node = getPreferences().node(getKey(info.location));
		node.put(KEY_URL, info.location.toExternalForm());
		if (info.description != null)
			node.put(KEY_DESCRIPTION, info.description);
		if (info.name != null)
			node.put(KEY_NAME, info.name);
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
		SimpleArtifactRepository cache = (SimpleArtifactRepository) createRepository(location.getArtifactRepositoryURL(), "download cache", "org.eclipse.equinox.p2.artifact.repository.simpleRepository"); //$NON-NLS-1$ //$NON-NLS-2$
		cache.tagAsImplementation();
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
			String type = child.get(KEY_TYPE, null);
			try {
				RepositoryInfo info = new RepositoryInfo();
				info.location = new URL(locationString);
				info.name = child.get(KEY_NAME, null);
				info.description = child.get(KEY_DESCRIPTION, null);
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
					loadRepository(new URL(tokenizer.nextToken()), (IProgressMonitor) null);
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
