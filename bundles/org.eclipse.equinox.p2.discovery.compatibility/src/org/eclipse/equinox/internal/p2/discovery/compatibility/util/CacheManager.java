/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc - additional implementation
 *     Sonatype, Inc. - additional implementation and p2 discovery support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery.compatibility.util;

import java.io.*;
import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.discovery.compatibility.Activator;
import org.eclipse.equinox.internal.p2.discovery.compatibility.Messages;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.osgi.util.NLS;

/**
 * A class to manage discovery cache files. Creating the cache files will place
 * the file in the plugin state location in a cache directory.
 */
public class CacheManager {

	private static final String PREFIX = "discovery"; //$NON-NLS-1$

	private static final String DOWNLOADING = "downloading"; //$NON-NLS-1$

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

		public IStatus getStatus() {

			return status;
		}

		public void setStatus(IStatus aStatus) {
			status = aStatus;
		}

	}

	public CacheManager(Transport transport) {
		this.transport = transport;
	}

	/**
	 * Returns a hash of the location.
	 */
	private int computeHash(URI location) {
		return location.hashCode();
	}

	/**
	 * Returns a local cache file with the contents of the given remote location,
	 * or <code>null</code> if a local cache could not be created.
	 * 
	 * @param location the remote location to be cached
	 * @param monitor a progress monitor
	 * @return A {@link File} object pointing to the cache file or <code>null</code>
	 * @throws FileNotFoundException if neither jar nor xml index file exists at given location 
	 * @throws AuthenticationFailedException if jar not available and xml causes authentication fail
	 * @throws IOException on general IO errors
	 * @throws ProvisionException on any error (e.g. user cancellation, unknown host, malformed address, connection refused, etc.)
	 * @throws OperationCanceledException - if user cancelled
	 */
	public File createCache(URI location, IProgressMonitor monitor) throws IOException, ProvisionException {

		SubMonitor submonitor = SubMonitor.convert(monitor, 1000);
		try {
			File cacheFile = getCache(location);

			boolean stale = true;
			long lastModified = 0L;

			if (cacheFile != null) {
				lastModified = cacheFile.lastModified();
			}
			// get last modified on jar
			long lastModifiedRemote = 0L;
			// bug 269588 - server may return 0 when file exists, so extra flag is needed
			try {
				lastModifiedRemote = transport.getLastModified(location, submonitor.newChild(1));
				if (lastModifiedRemote <= 0)
					LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Server returned lastModified <= 0 for " + location)); //$NON-NLS-1$
			} catch (AuthenticationFailedException e) {
				// it is not meaningful to continue - the credentials are for the server
				// do not pass the exception - it gives no additional meaningful user information
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_AUTHENTICATION, NLS.bind(Messages.CacheManager_AuthenticationFaileFor_0, location), null));
			} catch (CoreException e) {
				throw new ProvisionException(e.getStatus());
			} catch (OperationCanceledException e) {
				// must pass this on
				throw e;
			}
			if (submonitor.isCanceled())
				throw new OperationCanceledException();
			stale = lastModifiedRemote > lastModified || lastModifiedRemote <= 0;

			if (!stale)
				return cacheFile;

			// The cache is stale or missing, so we need to update it from the remote location
			cacheFile = getCacheFile(location);
			updateCache(cacheFile, location, lastModifiedRemote, submonitor);
			return cacheFile;
		} finally {
			submonitor.done();
		}
	}

	/**
	 * Deletes the local cache file(s) for the given location
	 * @param location
	 */
	void deleteCache(URI location) {
		File cacheFile = getCache(location);
		// delete the cache file if it exists
		safeDelete(cacheFile);
		// delete a resumable download if it exists
		safeDelete(new File(new File(cacheFile.getParentFile(), DOWNLOADING), cacheFile.getName()));
	}

	/**
	 * Determines the local file paths of the locations potential cache file.
	 * @param location The location to compute the cache for
	 * @return A {@link File} array with the cache files for JAR and XML extensions.
	 */
	private File getCache(URI location) {
		File cacheFile = getCacheFile(location);
		return cacheFile.exists() ? cacheFile : null;
	}

	private File getCacheFile(URI location) {
		return new File(getCacheDirectory(), PREFIX + computeHash(location));
	}

	/**
	 * Returns the file corresponding to the data area to be used by the cache manager.
	 */
	protected File getCacheDirectory() {
		return Activator.getDefault().getStateLocation().append("cache").toFile(); //$NON-NLS-1$
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
			if (tempFile.renameTo(cacheFile))
				return;
			result = new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.CacheManage_ErrorRenamingCache, new Object[] {remoteFile.toString(), tempFile.getAbsolutePath(), cacheFile.getAbsolutePath()}));
		}

		if (result.getSeverity() == IStatus.CANCEL || submonitor.isCanceled())
			throw new OperationCanceledException();
		throw new ProvisionException(result);
	}
}
