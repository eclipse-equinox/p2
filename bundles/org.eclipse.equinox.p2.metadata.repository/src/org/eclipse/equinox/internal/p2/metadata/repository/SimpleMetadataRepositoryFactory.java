/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype Inc - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import org.eclipse.equinox.internal.p2.repository.CacheManager;

import java.io.*;
import java.net.URI;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory;
import org.eclipse.osgi.util.NLS;

public class SimpleMetadataRepositoryFactory extends MetadataRepositoryFactory {

	private static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	private static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	private static final String PROTOCOL_FILE = "file"; //$NON-NLS-1$

	public IMetadataRepository create(URI location, String name, String type, Map<String, String> properties) {
		if (location.getScheme().equals("file")) //$NON-NLS-1$
			return new LocalMetadataRepository(getAgent(), location, name, properties);
		return new URLMetadataRepository(getAgent(), location, name, properties);
	}

	/**
	 * Returns a file in the local file system that contains the contents of the
	 * metadata repository at the given location.
	 */
	private File getLocalFile(URI location, IProgressMonitor monitor) throws IOException, ProvisionException {
		File localFile = null;
		URI jarLocation = URLMetadataRepository.getActualLocation(location, JAR_EXTENSION);
		URI xmlLocation = URLMetadataRepository.getActualLocation(location, XML_EXTENSION);
		// If the repository is local, we can return the repository file directly
		if (PROTOCOL_FILE.equals(xmlLocation.getScheme())) {
			//look for a compressed local file
			localFile = URIUtil.toFile(jarLocation);
			if (localFile.exists())
				return localFile;
			//look for an uncompressed local file
			localFile = URIUtil.toFile(xmlLocation);
			if (localFile.exists())
				return localFile;
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, msg, null));
		}
		// file is not local, create a cache of the repository metadata
		CacheManager cache = (CacheManager) getAgent().getService(CacheManager.SERVICE_NAME);
		if (cache == null)
			throw new IllegalArgumentException("Cache manager service not available"); //$NON-NLS-1$
		localFile = cache.createCache(location, URLMetadataRepository.CONTENT_FILENAME, monitor);
		if (localFile == null) {
			// there is no remote file in either form - this should not really happen as
			// createCache should bail out with exception if something is wrong. This is an internal
			// error.
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, Messages.repoMan_internalError, null));
		}
		return localFile;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory#load(java.net.URL, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IMetadataRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
		long time = 0;
		final String debugMsg = "Validating and loading metadata repository "; //$NON-NLS-1$
		if (Tracing.DEBUG_METADATA_PARSING) {
			Tracing.debug(debugMsg + location);
			time = -System.currentTimeMillis();
		}
		SubMonitor sub = SubMonitor.convert(monitor, 400);
		try {
			File localFile = getLocalFile(location, sub.newChild(300));
			InputStream inStream = new BufferedInputStream(new FileInputStream(localFile));
			JarInputStream jarStream = null;
			try {
				//if reading from a jar, obtain a stream on the entry with the actual contents
				if (localFile.getAbsolutePath().endsWith(JAR_EXTENSION)) {
					jarStream = new JarInputStream(inStream);
					JarEntry jarEntry = jarStream.getNextJarEntry();
					String entryName = URLMetadataRepository.CONTENT_FILENAME + URLMetadataRepository.XML_EXTENSION;
					while (jarEntry != null && (!entryName.equals(jarEntry.getName()))) {
						jarEntry = jarStream.getNextJarEntry();
					}
					//if there is a jar but the entry is missing or invalid, treat this as an invalid repository
					if (jarEntry == null)
						throw new IOException(NLS.bind(Messages.repoMan_invalidLocation, location));
				}
				//parse the repository descriptor file
				sub.setWorkRemaining(100);
				InputStream descriptorStream = jarStream != null ? jarStream : inStream;
				IMetadataRepository result = new MetadataRepositoryIO(getAgent()).read(localFile.toURL(), descriptorStream, sub.newChild(100));
				if (result != null && (flags & IRepositoryManager.REPOSITORY_HINT_MODIFIABLE) > 0 && !result.isModifiable())
					return null;
				if (result instanceof LocalMetadataRepository)
					((LocalMetadataRepository) result).initializeAfterLoad(location);
				if (result instanceof URLMetadataRepository)
					((URLMetadataRepository) result).initializeAfterLoad(location);
				if (Tracing.DEBUG_METADATA_PARSING) {
					time += System.currentTimeMillis();
					Tracing.debug(debugMsg + "time (ms): " + time); //$NON-NLS-1$ 
				}
				return result;
			} finally {
				safeClose(jarStream);
				safeClose(inStream);
			}
		} catch (FileNotFoundException e) {
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, msg, e));
		} catch (IOException e) {
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	/**
	 * Closes a stream, ignoring any secondary exceptions
	 */
	private void safeClose(InputStream stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (IOException e) {
			//ignore
		}
	}
}
