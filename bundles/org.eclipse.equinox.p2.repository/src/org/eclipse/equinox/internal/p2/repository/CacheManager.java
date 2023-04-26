/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc - additional implementation
 *     Sonatype Inc - additional implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import java.io.*;
import java.net.*;
import java.util.HashSet;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.osgi.util.NLS;

/**
 * A class to manage metadata cache files. Creating the cache files will place
 * the file in the AgentData location in a cache directory.
 *
 * Using the bus listeners will allow the manager to listen for repository
 * events. When a repository is removed, it will remove the cache file if one
 * was created for the repository.
 */
public class CacheManager {
	/**
	 * Service name for the internal cache manager service.
	 */
	public static final String SERVICE_NAME = CacheManager.class.getName();

	private final IAgentLocation agentLocation;

	private final Transport transport;

	/**
	 * IStateful implementation of BufferedOutputStream. Class is used to get the status from
	 * a download operation.
	 */
	private static class StatefulStream extends BufferedOutputStream implements IStateful {
		private IStatus status;

		public StatefulStream(OutputStream stream) {
			super(stream);
		}

		@Override
		public IStatus getStatus() {

			return status;
		}

		@Override
		public void setStatus(IStatus aStatus) {
			status = aStatus;
		}

	}

	public CacheManager(IAgentLocation agentLocation, Transport transport) {
		this.agentLocation = agentLocation;
		this.transport = transport;
	}

	private static SynchronousProvisioningListener busListener;
	private static final String DOWNLOADING = "downloading"; //$NON-NLS-1$
	private static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	private static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$

	private final HashSet<String> knownPrefixes = new HashSet<>(5);

	/**
	 * Returns a hash of the repository location.
	 */
	private int computeHash(URI repositoryLocation) {
		return repositoryLocation.hashCode();
	}

	public File createCacheFromFile(URI remoteFile, IProgressMonitor monitor) throws ProvisionException, IOException {
		if (!isURL(remoteFile)) {
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.CacheManager_CannotLoadNonUrlLocation, remoteFile), null));
		}

		SubMonitor submonitor = SubMonitor.convert(monitor, 1000);
		try {
			File cacheFile = getCacheFile(remoteFile);

			boolean stale = true;
			long lastModified = cacheFile.lastModified();
			long lastModifiedRemote = 0L;

			// bug 269588 - server may return 0 when file exists, so extra flag is needed
			try {
				lastModifiedRemote = transport.getLastModified(remoteFile, submonitor.newChild(1));
				if (lastModifiedRemote <= 0)
					LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Server returned lastModified <= 0 for " + remoteFile)); //$NON-NLS-1$
			} catch (AuthenticationFailedException e) {
				// it is not meaningful to continue - the credentials are for the server
				// do not pass the exception - it gives no additional meaningful user information
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_AUTHENTICATION, NLS.bind(Messages.CacheManager_AuthenticationFaileFor_0, remoteFile), null));
			} catch (FileNotFoundException e) {
				throw new FileNotFoundException(NLS.bind(Messages.CacheManager_Repository_not_found, remoteFile));
			} catch (CoreException e) {
				IStatus status = e.getStatus();
				if (status == null)
					throw new ProvisionException(
							new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND,
									NLS.bind(Messages.CacheManager_FailedCommunicationWithRepo_0, remoteFile), e));
				else if (status.getException() instanceof FileNotFoundException)
					throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID,
							ProvisionException.REPOSITORY_NOT_FOUND, status.getMessage(), status.getException()));
				else if (status.getException() != null) {
					Throwable ex = status.getException();
					if (ex.getClass() == java.net.SocketTimeoutException.class)
						throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, NLS.bind(Messages.CacheManager_FailedCommunicationWithRepo_0, remoteFile), ex));
				}
				throw new ProvisionException(status);
			}

			stale = lastModifiedRemote != lastModified;
			if (!stale)
				return cacheFile;

			// The cache is stale or missing, so we need to update it from the remote location
			updateCache(cacheFile, remoteFile, lastModifiedRemote, submonitor);
			return cacheFile;
		} finally {
			submonitor.done();
		}

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
	 * @throws OperationCanceledException - if user canceled
	 */
	public File createCache(URI repositoryLocation, String prefix, IProgressMonitor monitor) throws IOException, ProvisionException {
		if (!isURL(repositoryLocation)) {
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.CacheManager_CannotLoadNonUrlLocation, repositoryLocation), null));
		}
		SubMonitor submonitor = SubMonitor.convert(monitor, 1000);
		try {
			knownPrefixes.add(prefix);
			File cacheFile = getCache(repositoryLocation, prefix);
			URI jarLocation = URIUtil.append(repositoryLocation, prefix + JAR_EXTENSION);
			URI xmlLocation = URIUtil.append(repositoryLocation, prefix + XML_EXTENSION);
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
				lastModifiedRemote = getLastModified(jarLocation, submonitor.newChild(1));
				if (lastModifiedRemote <= 0)
					LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Server returned lastModified <= 0 for " + jarLocation)); //$NON-NLS-1$
			} catch (AuthenticationFailedException e) {
				// it is not meaningful to continue - the credentials are for the server
				// do not pass the exception - it gives no additional meaningful user information
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_AUTHENTICATION, NLS.bind(Messages.CacheManager_AuthenticationFaileFor_0, repositoryLocation), null));
			} catch (CoreException e) {
				useJar = false;
				// give up on a timeout - if we did not get a 404 on the jar, we will just prolong the pain
				// by (almost certainly) also timing out on the xml.
				if (e.getStatus() != null && e.getStatus().getException() != null) {
					Throwable ex = e.getStatus().getException();
					if (ex.getClass() == java.net.SocketTimeoutException.class)
						throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, NLS.bind(Messages.CacheManager_FailedCommunicationWithRepo_0, repositoryLocation), ex));
				}
			} catch (OperationCanceledException e) {
				// must pass this on
				throw e;
			} catch (Exception e) {
				// not ideal, just skip the jar on error, and try the xml instead - report errors for
				// the xml.
				useJar = false;
			}
			if (submonitor.isCanceled())
				throw new OperationCanceledException();

			if (useJar) {
				// There is a jar, and it should be used - cache is stale if it is xml based or
				// if older (irrespective of jar or xml).
				// Bug 269588 - also stale if remote reports 0
				stale = lastModifiedRemote != lastModified || (name != null && name.endsWith(XML_EXTENSION) || lastModifiedRemote <= 0);
			} else {
				// Also need to check remote XML file, and handle cancel, and errors
				// (Status is reported based on finding the XML file as giving up on certain errors
				// when checking for the jar may not be correct).
				try {
					lastModifiedRemote = getLastModified(xmlLocation, submonitor.newChild(1));
					// if lastModifiedRemote is 0 - something is wrong in the communication stack, as
					// a FileNotFound exception should have been thrown.
					// bug 269588 - server may return 0 when file exists - site is not correctly configured
					if (lastModifiedRemote <= 0)
						LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Server returned lastModified <= 0 for " + xmlLocation)); //$NON-NLS-1$

				} catch (FileNotFoundException e) {
					throw new FileNotFoundException(NLS.bind(Messages.CacheManager_Neither_0_nor_1_found, jarLocation, xmlLocation));
				} catch (AuthenticationFailedException e) {
					// do not pass the exception, it provides no additional meaningful user information
					throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_AUTHENTICATION, NLS.bind(Messages.CacheManager_AuthenticationFaileFor_0, repositoryLocation), null));
				} catch (CoreException e) {
					IStatus status = e.getStatus();
					if (status == null)
						throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.CacheManager_FailedCommunicationWithRepo_0, repositoryLocation), e));
					else if (status.getException() instanceof FileNotFoundException)
						throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, status.getMessage(), status.getException()));
					throw new ProvisionException(status);

				}
				// There is an xml, and it should be used - cache is stale if it is jar based or
				// if older (irrespective of jar or xml).
				// bug 269588 - server may return 0 when file exists - assume it is stale
				stale = lastModifiedRemote != lastModified || (name != null && name.endsWith(JAR_EXTENSION) || lastModifiedRemote <= 0);
				useExtension = XML_EXTENSION;
				remoteFile = xmlLocation;
			}

			if (!stale)
				return cacheFile;

			// The cache is stale or missing, so we need to update it from the remote location
			cacheFile = new File(getCacheDirectory(), prefix + hashCode + useExtension);
			updateCache(cacheFile, remoteFile, lastModifiedRemote, submonitor);
			return cacheFile;
		} finally {
			submonitor.done();
		}
	}

	private long getLastModified(URI location, IProgressMonitor monitor) throws AuthenticationFailedException, FileNotFoundException, CoreException {
		CoreException exception = null;
		long lastModifiedRemote = -1L;
		do {
			try {
				lastModifiedRemote = transport.getLastModified(location, monitor);
			} catch (CoreException e) {
				if (e.getStatus() == null) {
					throw e;
				}
				exception = e;
			}
		} while (exception != null && exception.getStatus() != null && exception.getStatus().getCode() == IArtifactRepository.CODE_RETRY);
		if (exception != null)
			throw exception;
		return lastModifiedRemote;
	}

	/**
	 * Deletes the local cache file(s) for the given repository
	 * @param repositoryLocation
	 */
	void deleteCache(URI repositoryLocation) {
		for (String prefix : knownPrefixes) {
			File[] cacheFiles = getCacheFiles(repositoryLocation, prefix);
			for (File cacheFile : cacheFiles) {
				// delete the cache file if it exists
				safeDelete(cacheFile);
				// delete a resumable download if it exists
				safeDelete(new File(new File(cacheFile.getParentFile(), DOWNLOADING), cacheFile.getName()));
			}
		}
	}

	/**
	 * Determines the local file path of the repository's cache file.
	 * @param repositoryLocation The location to compute the cache for
	 * @param prefix The prefix to use for this location
	 * @return A {@link File} pointing to the cache file or <code>null</code> if
	 * the cache file does not exist.
	 */
	protected File getCache(URI repositoryLocation, String prefix) {
		File[] files = getCacheFiles(repositoryLocation, prefix);
		if (files[0].exists())
			return files[0];
		return files[1].exists() ? files[1] : null;
	}

	/**
	 * Returns the file corresponding to the data area to be used by the cache manager.
	 */
	protected File getCacheDirectory() {
		return URIUtil.toFile(agentLocation.getDataArea(Activator.ID + "/cache/")); //$NON-NLS-1$
	}

	/**
	 * Determines the local file paths of the repository's potential cache files.
	 * @param repositoryLocation The location to compute the cache for
	 * @param prefix The prefix to use for this location
	 * @return A {@link File} array with the cache files for JAR and XML extensions.
	 */
	private File[] getCacheFiles(URI repositoryLocation, String prefix) {
		File[] files = new File[2];
		File dataAreaFile = getCacheDirectory();
		int hashCode = computeHash(repositoryLocation);
		files[0] = new File(dataAreaFile, prefix + hashCode + JAR_EXTENSION);
		files[1] = new File(dataAreaFile, prefix + hashCode + XML_EXTENSION);
		return files;
	}

	private File getCacheFile(URI url) {
		File dataAreaFile = getCacheDirectory();
		int hashCode = computeHash(url);
		return new File(dataAreaFile, Integer.toString(hashCode));
	}

	private static boolean isURL(URI location) {
		try {
			new URL(location.toASCIIString());
		} catch (MalformedURLException e) {
			return false;
		}
		return true;
	}

	/**
	 * Adds a {@link SynchronousProvisioningListener} to the event bus for
	 * deleting cache files when the corresponding repository is deleted.
	 */
	private void registerRepoEventListener(IProvisioningEventBus eventBus) {
		if (busListener == null) {
			busListener = o -> {
				if (o instanceof RepositoryEvent) {
					RepositoryEvent event = (RepositoryEvent) o;
					if (RepositoryEvent.REMOVED == event.getKind() && IRepository.TYPE_METADATA == event.getRepositoryType()) {
						deleteCache(event.getRepositoryLocation());
					}
				}
			};
		}
		// the bus could have disappeared and is now back again - so do this every time
		eventBus.addListener(busListener);
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

	public void setEventBus(IProvisioningEventBus newBus) {
		registerRepoEventListener(newBus);
	}

	public void unsetEventBus(IProvisioningEventBus oldBus) {
		unregisterRepoEventListener(oldBus);
	}

	/**
	 * Removes the {@link SynchronousProvisioningListener} that cleans up the
	 * cache file from the event bus.
	 */
	private void unregisterRepoEventListener(IProvisioningEventBus bus) {
		if (bus != null && busListener != null)
			bus.removeListener(busListener);
	}

	protected void updateCache(File cacheFile, URI remoteFile, long lastModifiedRemote, SubMonitor submonitor) throws FileNotFoundException, IOException, ProvisionException {
		cacheFile.getParentFile().mkdirs();
		File downloadDir = new File(cacheFile.getParentFile(), DOWNLOADING);
		if (!downloadDir.exists())
			downloadDir.mkdir();
		File tempFile = new File(downloadDir, cacheFile.getName());
		// Ensure that the file from a previous download attempt is removed
		if (tempFile.exists())
			safeDelete(tempFile);

		tempFile.createNewFile();

		StatefulStream stream = null;
		try {
			stream = new StatefulStream(new FileOutputStream(tempFile));
		} catch (Exception e) {
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e));
		}
		IStatus result = null;
		try {
			submonitor.setWorkRemaining(1000);
			result = transport.download(remoteFile, stream, submonitor.newChild(1000));
			while (result.getCode() == IArtifactRepository.CODE_RETRY) {
				result = transport.download(remoteFile, stream, submonitor.newChild(1000));
			}
		} catch (OperationCanceledException e) {
			// need to pick up the status - a new operation canceled exception is thrown at the end
			// as status will be CANCEL.
			result = stream.getStatus();
		} finally {
			stream.close();
			// If there was any problem fetching the file, delete the temp file
			if (result == null || !result.isOK())
				safeDelete(tempFile);
		}
		if (result.isOK()) {
			if (cacheFile.exists())
				safeDelete(cacheFile);
			if (tempFile.renameTo(cacheFile)) {
				if (lastModifiedRemote != -1 && lastModifiedRemote != 0) {
					//local cache file should have the same lastModified as the server's file. bug 324200
					cacheFile.setLastModified(lastModifiedRemote);
				}
				return;
			}
			result = new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.CacheManage_ErrorRenamingCache, new Object[] {remoteFile.toString(), tempFile.getAbsolutePath(), cacheFile.getAbsolutePath()}));
		}

		if (result.getSeverity() == IStatus.CANCEL || submonitor.isCanceled())
			throw new OperationCanceledException();
		throw new ProvisionException(result);
	}
}
