/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
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
		String name;
		IMetadataRepository repository;
		URL url;
	}

	private static final String FACTORY = "factory"; //$NON-NLS-1$

	private static final String KEY_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String KEY_NAME = "name"; //$NON-NLS-1$
	private static final String KEY_PROVIDER = "provider"; //$NON-NLS-1$
	private static final String KEY_TYPE = "type"; //$NON-NLS-1$
	private static final String KEY_URL = "url"; //$NON-NLS-1$
	private static final String KEY_VERSION = "version"; //$NON-NLS-1$
	private static final String NODE_REPOSITORIES = "repositories"; //$NON-NLS-1$

	private Map repositories = null;
	//lock object to be held when referring repositories field
	private final Object repositoryLock = new Object();

	public MetadataRepositoryManager() {
		//initialize repositories lazily
	}

	public void addRepository(IMetadataRepository repository) {
		RepositoryInfo info = new RepositoryInfo();
		info.repository = repository;
		info.name = repository.getName();
		info.description = repository.getDescription();
		info.url = repository.getLocation();
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			repositories.put(getKey(repository), info);
		}
		// save the given repository in the preferences.
		remember(repository);
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

	// TODO This method really should not be here.  There could be lots of different kinds of
	// repositories and many different ways to create them.
	// for now discriminate by the type of URL but this is bogus.
	public IMetadataRepository createRepository(URL location, String name, String type) {
		IMetadataRepository result = loadRepository(location, (IProgressMonitor) null);
		if (result != null)
			return result;
		IExtension extension = RegistryFactory.getRegistry().getExtension(Activator.REPO_PROVIDER_XPT, type);
		if (extension == null)
			return null;
		IMetadataRepositoryFactory factory = (IMetadataRepositoryFactory) createExecutableExtension(extension, FACTORY);
		if (factory == null)
			return null;
		result = factory.create(location, name, type);
		if (result != null)
			addRepository(result);
		return result;
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

	public URL[] getKnownRepositories() {
		synchronized (repositoryLock) {
			if (repositories == null)
				restoreRepositories();
			URL[] result = new URL[repositories.size()];
			int i = 0;
			for (Iterator it = repositories.values().iterator(); it.hasNext(); i++) {
				RepositoryInfo info = (RepositoryInfo) it.next();
				result[i] = info.url;
			}
			return result;
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
				if (URLUtil.sameURL(info.url, location))
					return info.repository;
			}
			return null;
		}
	}

	public IMetadataRepository loadRepository(URL location, IProgressMonitor progress) {
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
		return null;
	}

	/**
	 * Try to load a pre-existing repo at the given location
	 */
	// TODO this method should do some repo type discovery something like is done with
	// the artifact repos.  For now just discriminate on the type of URL
	private IMetadataRepository loadRepository(URL location, String suffix) {
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

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.query.IQueryable#query(org.eclipse.equinox.p2.query.Query, org.eclipse.equinox.p2.query.Collector, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		URL[] locations = getKnownRepositories();
		SubMonitor sub = SubMonitor.convert(monitor, locations.length * 10);
		for (int i = 0; i < locations.length; i++) {
			IMetadataRepository repository = loadRepository(locations[i], sub.newChild(9));
			if (repository != null)
				repository.query(query, collector, sub.newChild(1));
		}
		sub.done();
		return collector;
	}

	/*
	 * Save the list of repositories in the preference store.
	 */
	private void remember(IMetadataRepository repository) {
		Preferences node = getPreferences().node(getKey(repository));
		String value = repository.getLocation().toExternalForm();
		node.put(KEY_URL, value);
		value = repository.getDescription();
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
		saveToPreferences();
	}

	public boolean removeRepository(URL toRemove) {
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
				info.url = new URL(locationString);
				info.name = child.get(KEY_NAME, null);
				info.description = child.get(KEY_DESCRIPTION, null);
				repositories.put(getKey(info.url), info);
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
				info.url = new URL(pathString);
				repositories.put(getKey(info.url), info);
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
