/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.io.*;
import java.net.URL;
import java.util.EventObject;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * A class to manage metadata cache files. Creating the cache files will place
 * the file in the AgentData location in a cache directory.
 * 
 * Using the bus listeners will allow the manager to listen for repository
 * events. When a repository is removed, it will remove the cache file if one
 * was created for the repository.
 */
public class CacheManager {
	private static SynchronousProvisioningListener busListener;
	private static final String CONTENT_FILENAME = "content"; //$NON-NLS-1$
	private static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	private static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$

	/**
	 * Returns a hash of the URL.
	 */
	private int computeHash(URL repositoryLocation) {
		//don't use URL#hashCode because it performs DNS lookups
		return repositoryLocation.toExternalForm().hashCode();
	}

	/**
	 * Returns a local cache file with the contents of the given remote location,
	 * or <code>null</code> if a local cache could not be created.
	 * 
	 * @param repositoryLocation
	 * @param monitor - a progress monitor
	 * @return A {@link File} object pointing to the cache file or <code>null</code>
	 * if the location is not a repository.
	 * @throws IOException
	 * @throws ProvisionException
	 */
	public File createCache(URL repositoryLocation, IProgressMonitor monitor) throws IOException, ProvisionException {
		File cacheFile = getCache(repositoryLocation);
		URL jarLocation = URLMetadataRepository.getActualLocation(repositoryLocation, JAR_EXTENSION);
		URL xmlLocation = URLMetadataRepository.getActualLocation(repositoryLocation, XML_EXTENSION);
		AgentLocation agentLocation = (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
		URL dataArea = agentLocation.getDataArea(Activator.ID + "/cache/"); //$NON-NLS-1$
		File dataAreaFile = URLUtil.toFile(dataArea);
		int hashCode = computeHash(repositoryLocation);
		if (cacheFile == null || isCacheStale(repositoryLocation, cacheFile)) {
			long lastModifiedRemote = getTransport().getLastModified(jarLocation);
			URL remoteFile;
			if (lastModifiedRemote != 0) {
				cacheFile = new File(dataAreaFile, CONTENT_FILENAME + hashCode + JAR_EXTENSION);
				remoteFile = jarLocation;
			} else {
				lastModifiedRemote = getTransport().getLastModified(xmlLocation);
				if (lastModifiedRemote == 0)
					// no jar or xml file found
					return null;
				cacheFile = new File(dataAreaFile, CONTENT_FILENAME + hashCode + XML_EXTENSION);
				remoteFile = xmlLocation;
			}
			cacheFile.getParentFile().mkdirs();
			OutputStream metadata = new BufferedOutputStream(new FileOutputStream(cacheFile));
			try {
				IStatus result = getTransport().download(remoteFile.toExternalForm(), metadata, monitor);
				if (!result.isOK()) {
					throw new ProvisionException(result);
				}
			} finally {
				metadata.close();
			}
		}
		return cacheFile;
	}

	/**
	 * Deletes the local cache file for the given repository
	 * @param repositoryLocation
	 */
	void deleteCache(URL repositoryLocation) {
		File cacheFile = getCache(repositoryLocation);
		if (cacheFile != null)
			safeDelete(cacheFile);
	}

	/**
	 * Determines the local filepath of the repository's cache file.
	 * @param repositoryLocation
	 * @return A {@link File} pointing to the cache file or <code>null</code> if
	 * the cache file does not exist.
	 */
	private File getCache(URL repositoryLocation) {
		AgentLocation agentLocation = (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
		URL dataArea = agentLocation.getDataArea(Activator.ID + "/cache/"); //$NON-NLS-1$
		File dataAreaFile = URLUtil.toFile(dataArea);
		int hashCode = computeHash(repositoryLocation);
		File cacheFile = new File(dataAreaFile, CONTENT_FILENAME + hashCode + JAR_EXTENSION);
		if (!cacheFile.exists()) {
			cacheFile = new File(dataAreaFile, CONTENT_FILENAME + hashCode + XML_EXTENSION);
			if (!cacheFile.exists())
				return null;
		}
		return cacheFile;
	}

	private Object getService(BundleContext ctx, String name) {
		ServiceReference reference = ctx.getServiceReference(name);
		if (reference == null)
			return null;
		Object result = ctx.getService(reference);
		ctx.ungetService(reference);
		return result;
	}

	private ECFMetadataTransport getTransport() {
		return ECFMetadataTransport.getInstance();
	}

	/**
	 * Checks if the repository's local cache file is out of date.
	 * @param repositoryLocation The remote location of the file
	 * @param cacheFile The current local cache of the remote location
	 * @return <code>true</code> if the cache file is out of date, <code>false</code>
	 * if the cache file is in sync with the repository. The cache file is
	 * considered stale if there is no local cache file.
	 */
	private boolean isCacheStale(URL repositoryLocation, File cacheFile) {
		long lastModified = cacheFile.lastModified();
		String name = cacheFile.getName();
		URL metadataLocation = null;
		if (name.endsWith(XML_EXTENSION)) {
			metadataLocation = URLMetadataRepository.getActualLocation(repositoryLocation, XML_EXTENSION);
		} else if (name.endsWith(JAR_EXTENSION)) {
			metadataLocation = URLMetadataRepository.getActualLocation(repositoryLocation, JAR_EXTENSION);
		}
		long lastModifiedRemote = 0;
		try {
			lastModifiedRemote = getTransport().getLastModified(metadataLocation);
		} catch (ProvisionException e) {
			// cache is stale
			return true;
		}
		return lastModifiedRemote > lastModified ? true : false;
	}

	/**
	 * Adds a {@link SynchronousProvisioningListener} to the event bus for
	 * deleting cache files when the corresponding repository is deleted.
	 */
	public void registerRepoEventListener() {
		IProvisioningEventBus eventBus = (IProvisioningEventBus) getService(Activator.getContext(), IProvisioningEventBus.SERVICE_NAME);
		if (eventBus == null) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "ProvisioningEventBus could not be obtained. Metadata caches may not be cleaned up properly.")); //$NON-NLS-1$
			return;
		}
		if (busListener == null) {
			busListener = new SynchronousProvisioningListener() {
				public void notify(EventObject o) {
					if (o instanceof RepositoryEvent) {
						RepositoryEvent event = (RepositoryEvent) o;
						if (RepositoryEvent.REMOVED == event.getKind() && IRepository.TYPE_METADATA == event.getRepositoryType()) {
							deleteCache(event.getRepositoryLocation());
						}
					}
				}
			};
			eventBus.addListener(busListener);
		}
	}

	private boolean safeDelete(File file) {
		if (file.exists()) {
			if (!file.delete()) {
				file.deleteOnExit();
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes the {@link SynchronousProvisioningListener} that cleans up the
	 * cache file from the event bus.
	 */
	public void unregisterRepoEventListener() {
		IProvisioningEventBus eventBus = (IProvisioningEventBus) getService(Activator.getContext(), IProvisioningEventBus.SERVICE_NAME);
		if (eventBus != null && busListener != null) {
			eventBus.removeListener(busListener);
			busListener = null;
		}
	}
}
