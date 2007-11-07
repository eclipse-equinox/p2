/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.spi.p2.artifact.repository.IArtifactRepositoryFactory;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

// TODO Need to react to repository going away 
// TODO the current assumption that the "location" is the dir/root limits us to 
// having just one repo in a given URL..  
public class ArtifactRepositoryManager implements IArtifactRepositoryManager {
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

	private List repositories = Collections.synchronizedList(new ArrayList());

	public ArtifactRepositoryManager() {
		restoreRepositories();
	}

	public void addRepository(IArtifactRepository repository) {
		repositories.add(repository);
		// save the given repository in the preferences.
		remember(repository);
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
		throw new CoreException(new Status(IStatus.ERROR, Activator.ID, "Malformed extension"));
	}

	public IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination) {
		return new MirrorRequest(key, destination);
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

	private void forget(IArtifactRepository toRemove) throws BackingStoreException {
		getPreferences().node(getKey(toRemove)).removeNode();
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
		return repository.getLocation().toExternalForm().replace('/', '_');
	}

	public IArtifactRepository[] getKnownRepositories() {
		if (repositories == null)
			restoreRepositories();
		return (IArtifactRepository[]) repositories.toArray(new IArtifactRepository[repositories.size()]);
	}

	/*
	 * Return the root preference node where we store the repository information.
	 */
	private Preferences getPreferences() {
		return new ConfigurationScope().getNode(Activator.ID).node(NODE_REPOSITORIES);
	}

	public IArtifactRepository getRepository(URL location) {
		if (repositories == null)
			restoreRepositories();
		for (Iterator iterator = repositories.iterator(); iterator.hasNext();) {
			IArtifactRepository match = (IArtifactRepository) iterator.next();
			if (Utils.sameURL(match.getLocation(), location))
				return match;
		}
		return null;
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
		saveRepositoryList();
	}

	public void removeRepository(IArtifactRepository toRemove) {
		if (toRemove != null)
			repositories.remove(toRemove);
		// remove the repository from the preferences
		try {
			forget(toRemove);
			saveRepositoryList();
		} catch (BackingStoreException e) {
			log("Error saving preferences", e); //$NON-NLS-1$
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
			String url = child.get(KEY_URL, null);
			if (url == null)
				continue;
			try {
				IArtifactRepository repository = loadRepository(new URL(url), (IProgressMonitor) null);
				// If we could not restore the repo then remove it from the preferences.
				if (repository == null)
					child.removeNode();
			} catch (MalformedURLException e) {
				log("Error while restoring repository: " + url, e); //$NON-NLS-1$
			} catch (BackingStoreException e) {
				log("Error restoring repositories from preferences", e); //$NON-NLS-1$
			}
		}
		// now that we have loaded everything, remember them
		saveRepositoryList();
	}

	private void restoreRepositories() {
		repositories = new ArrayList();
		// TODO while recreating, we may want to have proxies on repo instead of the real repo object to limit what is activated.
		AgentLocation location = (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
		if (location == null)
			// TODO should do something here since we are failing to restore.
			return;
		SimpleArtifactRepository cache = (SimpleArtifactRepository) createRepository(location.getArtifactRepositoryURL(), "download cache", "org.eclipse.equinox.p2.artifact.repository.simpleRepository"); //$NON-NLS-1$ //$NON-NLS-2$
		cache.tagAsImplementation();

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
		// restore the persisted list of repositories
		restoreFromPreferences();
	}

	/*
	 * Save the list of repositories to the file-system.
	 */
	private void saveRepositoryList() {
		try {
			getPreferences().flush();
		} catch (BackingStoreException e) {
			log("Error while saving repositories in preferences", e); //$NON-NLS-1$
		}
	}
}
