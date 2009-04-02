/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
import java.net.URI;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ecf.filetransfer.UserCancelledException;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.internal.p2.repository.RepositoryTransport;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.osgi.util.NLS;
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
	private static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	private static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$

	private final HashSet knownPrefixes = new HashSet(5);

	/**
	 * Returns a hash of the URL.
	 */
	private int computeHash(URI repositoryLocation) {
		return repositoryLocation.hashCode();
	}

	/**
	 * Returns a local cache file with the contents of the given remote location,
	 * or <code>null</code> if a local cache could not be created.
	 * 
	 * @param repositoryLocation The remote location to be cached
	 * @param prefix The prefix to use when creating the cache file
	 * @param monitor a progress monitor
	 * @return A {@link File} object pointing to the cache file or <code>null</code>
	 * if the location is not a repository.
	 * @throws FileNotFoundException if neither jar nor xml index file exists at given location 
	 * @throws AuthenticationFailedException if jar not available and xml causes authentication fail
	 * @throws IOException on general IO errors
	 * @throws ProvisionException on any error (e.g. user cancellation, unknown host, malformed address, connection refused, etc.)
	 */
	public File createCache(URI repositoryLocation, String prefix, IProgressMonitor monitor) throws IOException, ProvisionException {

		SubMonitor submonitor = SubMonitor.convert(monitor, 1000);
		try {
			knownPrefixes.add(prefix);
			File cacheFile = getCache(repositoryLocation, prefix);
			URI jarLocation = URIUtil.append(repositoryLocation, prefix + JAR_EXTENSION);
			URI xmlLocation = URIUtil.append(repositoryLocation, prefix + XML_EXTENSION);
			AgentLocation agentLocation = (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
			URL dataArea = agentLocation.getDataArea(Activator.ID + "/cache/"); //$NON-NLS-1$
			File dataAreaFile = URLUtil.toFile(dataArea);
			int hashCode = computeHash(repositoryLocation);

			// Knowing if cache is stale is complicated by the fact that a jar could have been 
			// produced after an xml index (and vice versa), and by the need to capture any
			// errors, as these needs to be reported to the user as something meaningful - instead of
			// just a general "can't read repository".
			// (Previous impl of stale checking ignored errors, and caused multiple round-trips)
			boolean stale = true;
			long lastModified = 0L;
			String name = null;
			String useExtension = JAR_EXTENSION;
			URI remoteFile = jarLocation;

			if (cacheFile != null) {
				lastModified = cacheFile.lastModified();
				name = cacheFile.getName();
			}
			// get last modified on jar
			long lastModifiedRemote = 0L;
			// bug 269588 - server may return 0 when file exists, so extra flag is needed
			boolean useJar = true;
			try {
				lastModifiedRemote = getTransport().getLastModified(jarLocation, submonitor.newChild(1));
				if (lastModifiedRemote <= 0)
					LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Server returned lastModified <= 0 for " + jarLocation)); //$NON-NLS-1$

			} catch (Exception e) {
				// not ideal, just skip the jar on error, and try the xml instead - report errors for
				// the xml.
				useJar = false;
			}
			if (monitor.isCanceled())
				throw new OperationCanceledException();

			if (useJar) {
				// There is a jar, and it should be used - cache is stale if it is xml based or
				// if older (irrespective of jar or xml).
				// Bug 269588 - also stale if remote reports 0
				stale = lastModifiedRemote > lastModified || (name != null && name.endsWith(XML_EXTENSION) || lastModifiedRemote <= 0);
			} else {
				// Also need to check remote XML file, and handle cancel, and errors
				// (Status is reported based on finding the XML file as giving up on certain errors
				// when checking for the jar may not be correct).
				try {
					lastModifiedRemote = getTransport().getLastModified(xmlLocation, submonitor.newChild(1));
					// if lastModifiedRemote is 0 - something is wrong in the communication stack, as 
					// a FileNotFound exception should have been thrown.
					// bug 269588 - server may return 0 when file exists - site is not correctly configured
					if (lastModifiedRemote <= 0)
						LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Server returned lastModified <= 0 for " + xmlLocation)); //$NON-NLS-1$

				} catch (UserCancelledException e) {
					throw new ProvisionException(Status.CANCEL_STATUS);
				} catch (FileNotFoundException e) {
					throw new FileNotFoundException(NLS.bind(Messages.CacheManager_Neither_0_nor_1_found, jarLocation, xmlLocation));
				} catch (AuthenticationFailedException e) {
					throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.CacheManager_AuthenticationFaileFor_0, repositoryLocation), e));
				} catch (CoreException e) {
					throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.CacheManager_FailedCommunicationWithRepo_0, repositoryLocation), e));

				}
				// There is an xml, and it should be used - cache is stale if it is jar based or
				// if older (irrespective of jar or xml).
				// bug 269588 - server may return 0 when file exists - assume it is stale
				stale = lastModifiedRemote > lastModified || (name != null && name.endsWith(JAR_EXTENSION) || lastModifiedRemote <= 0);
				useExtension = XML_EXTENSION;
				remoteFile = xmlLocation;
			}

			if (!stale)
				return cacheFile;

			// Need to update cache
			cacheFile = new File(dataAreaFile, prefix + hashCode + useExtension);
			cacheFile.getParentFile().mkdirs();
			OutputStream metadata = new BufferedOutputStream(new FileOutputStream(cacheFile));
			IStatus result;
			try {
				submonitor.setWorkRemaining(1000);
				result = getTransport().download(remoteFile, metadata, submonitor.newChild(1000));
			} finally {
				metadata.close();
			}
			if (!result.isOK()) {
				//don't leave a partial cache file lying around
				// TODO: HENRIK Handle resume
				cacheFile.delete();
				throw new ProvisionException(result);
			}
			return cacheFile;

		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	/**
	 * Deletes the local cache file for the given repository
	 * @param repositoryLocation
	 */
	void deleteCache(URI repositoryLocation) {
		for (Iterator it = knownPrefixes.iterator(); it.hasNext();) {
			String prefix = (String) it.next();
			File cacheFile = getCache(repositoryLocation, prefix);
			if (cacheFile != null)
				safeDelete(cacheFile);
		}
	}

	/**
	 * Determines the local file path of the repository's cache file.
	 * @param repositoryLocation The location to compute the cache for
	 * @param prefix The prefix to use for this location
	 * @return A {@link File} pointing to the cache file or <code>null</code> if
	 * the cache file does not exist.
	 */
	private File getCache(URI repositoryLocation, String prefix) {
		AgentLocation agentLocation = (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
		URL dataArea = agentLocation.getDataArea(Activator.ID + "/cache/"); //$NON-NLS-1$
		File dataAreaFile = URLUtil.toFile(dataArea);
		int hashCode = computeHash(repositoryLocation);
		File cacheFile = new File(dataAreaFile, prefix + hashCode + JAR_EXTENSION);
		if (!cacheFile.exists()) {
			cacheFile = new File(dataAreaFile, prefix + hashCode + XML_EXTENSION);
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

	private RepositoryTransport getTransport() {
		return RepositoryTransport.getInstance();
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
