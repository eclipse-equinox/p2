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
import org.eclipse.equinox.internal.p2.core.helpers.Utils;
import org.eclipse.equinox.p2.core.repository.RepositoryCreationException;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.spi.p2.metadata.repository.IMetadataRepositoryFactory;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class MetadataRepositoryManager implements IMetadataRepositoryManager {
	private static final String FACTORY = "factory"; //$NON-NLS-1$

	private static final String NODE_REPOSITORIES = "repositories"; //$NON-NLS-1$
	private static final String KEY_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String KEY_NAME = "name"; //$NON-NLS-1$
	private static final String KEY_PROVIDER = "provider"; //$NON-NLS-1$
	private static final String KEY_TYPE = "type"; //$NON-NLS-1$
	private static final String KEY_URL = "url"; //$NON-NLS-1$
	private static final String KEY_VERSION = "version"; //$NON-NLS-1$

	private List repositories = Collections.synchronizedList(new ArrayList());

	public MetadataRepositoryManager() {
		restoreRepositories();
	}

	public void addRepository(IMetadataRepository repository) {
		repositories.add(repository);
		// save the given repository in the preferences.
		remember(repository);
	}

	/*
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
			String url = child.get(KEY_URL, null);
			if (url == null)
				continue;
			try {
				IMetadataRepository repository = loadRepository(new URL(url), (IProgressMonitor) null);
				// If we could not restore the repo then remove it from the preferences.
				if (repository == null)
					child.removeNode();
			} catch (MalformedURLException e) {
				log("Error while restoring repository: " + url, e); //$NON-NLS-1$
			} catch (BackingStoreException e) {
				log("Error while restoring repository: " + url, e); //$NON-NLS-1$
			}
		}
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
		saveRepositoryList();
	}

	/*
	 * Return a string key suitable based on the given repository which
	 * is suitable for use as a preference node name.
	 */
	private String getKey(IMetadataRepository repository) {
		return repository.getLocation().toExternalForm().replace('/', '_');
	}

	public IMetadataRepository loadRepository(URL location, IProgressMonitor progress) {
		// TODO do some thing with the monitor
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

	private String[] getAllSuffixes() {
		IConfigurationElement[] elements = RegistryFactory.getRegistry().getConfigurationElementsFor(Activator.REPO_PROVIDER_XPT);
		ArrayList result = new ArrayList(elements.length);
		for (int i = 0; i < elements.length; i++)
			if (elements[i].getName().equals("filter"))
				result.add(elements[i].getAttribute("suffix"));
		return (String[]) result.toArray(new String[result.size()]);
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
		try {
			IMetadataRepositoryFactory factory = (IMetadataRepositoryFactory) createExecutableExtension(extension, FACTORY);
			if (factory == null)
				return null;
			result = factory.create(location, name, type);
			if (result != null)
				addRepository(result);
			return result;
		} catch (CoreException e) {
			return null;
		}
	}

	/**
	 * Try to load a pre-existing repo at the given location
	 */
	// TODO this method should do some repo type discovery something like is done with
	// the artifact repos.  For now just discriminate on the type of URL
	private IMetadataRepository loadRepository(URL location, String suffix) {
		IExtension[] providers = findMatchingRepositoryExtensions(suffix);
		// Loop over the candidates and return the first one that successfully loads
		for (int i = 0; i < providers.length; i++)
			try {
				IMetadataRepositoryFactory factory = (IMetadataRepositoryFactory) createExecutableExtension(providers[i], FACTORY);
				if (factory != null)
					return factory.load(location);
			} catch (CoreException e) {
				log("Error loading repository extension: " + providers[i].getUniqueIdentifier(), e); //$NON-NLS-1$
			}
		return null;
	}

	public IMetadataRepository[] getKnownRepositories() {
		return (IMetadataRepository[]) repositories.toArray(new IMetadataRepository[repositories.size()]);
	}

	public IMetadataRepository getRepository(URL location) {
		if (repositories == null)
			restoreRepositories();
		for (Iterator iterator = repositories.iterator(); iterator.hasNext();) {
			IMetadataRepository match = (IMetadataRepository) iterator.next();
			if (Utils.sameURL(match.getLocation(), location))
				return match;
		}
		return null;
	}

	protected void log(String message, Throwable t) {
		LogHelper.log(new Status(IStatus.ERROR, Activator.PI_METADATA_REPOSITORY, message, t));
	}

	public void removeRepository(IMetadataRepository toRemove) {
		repositories.remove(toRemove);
		// remove the repository from the preference store
		try {
			getPreferences().node(getKey(toRemove)).removeNode();
			saveRepositoryList();
		} catch (BackingStoreException e) {
			log("Error saving preferences", e); //$NON-NLS-1$
		}
	}

	public void restoreRepositories() {
		//TODO we may want to have proxies on repo instead of the real repo object to limit what is activated.
		URL path = null;
		//		try {
		//			AgentLocation location = (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
		//			if (location == null)
		//				// TODO should do something here since we are failing to restore.
		//				return;
		//			path = location.getMetadataRepositoryURL();
		//			repositories.add(new MetadataCache(path));
		//		} catch (RepositoryCreationException e) {
		//			log("Error while restoring repository " + path, e);
		//		}
		try {
			String locationString = Activator.getContext().getProperty("eclipse.p2.metadataRepository");
			if (locationString != null) {
				StringTokenizer tokenizer = new StringTokenizer(locationString, ",");
				while (tokenizer.hasMoreTokens()) {
					try {
						path = new URL(tokenizer.nextToken());
						loadRepository(path, (IProgressMonitor) null);
					} catch (MalformedURLException e) {
						throw new RepositoryCreationException(e);
					}
				}
			}
		} catch (RepositoryCreationException e) {
			log("Error while restoring repository " + path, e);
		}
		// load the list which is stored in the preferences
		restoreFromPreferences();
	}

	/*
	 * Return the preference node which is the root for where we store the repository information.
	 */
	private Preferences getPreferences() {
		return new ConfigurationScope().getNode(Activator.PI_METADATA_REPOSITORY).node(NODE_REPOSITORIES);
	}

	/*
	 * Save the repository list in the file-system
	 */
	private void saveRepositoryList() {
		try {
			getPreferences().flush();
		} catch (BackingStoreException e) {
			log("Error while saving repositories in preferences", e);
		}
	}

	private Object createExecutableExtension(IExtension extension, String element) throws CoreException {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i].getName().equals(element))
				return elements[i].createExecutableExtension("class");
		}
		throw new CoreException(new Status(IStatus.ERROR, Activator.ID, "Malformed extension"));
	}

	private IExtension[] findMatchingRepositoryExtensions(String suffix) {
		IConfigurationElement[] elt = RegistryFactory.getRegistry().getConfigurationElementsFor(Activator.REPO_PROVIDER_XPT);
		int count = 0;
		for (int i = 0; i < elt.length; i++) {
			if (elt[i].getName().equals("filter")) {
				if (!elt[i].getAttribute("suffix").equals(suffix)) {
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
}
