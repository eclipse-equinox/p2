/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.metadata.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.osgi.util.NLS;

public class SimpleMetadataRepositoryFactory implements IMetadataRepositoryFactory {

	private static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	private static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	private static final String PROTOCOL_FILE = "file"; //$NON-NLS-1$

	public IMetadataRepository create(URL location, String name, String type, Map properties) {
		if (location.getProtocol().equals("file")) //$NON-NLS-1$
			return new LocalMetadataRepository(location, name, properties);
		return new URLMetadataRepository(location, name, properties);
	}

	/**
	 * Returns a file in the local file system that contains the contents of the
	 * metadata repository at the given location.
	 */
	private File getLocalFile(URL location, IProgressMonitor monitor) throws IOException, ProvisionException {
		File localFile = null;
		URL jarLocation = URLMetadataRepository.getActualLocation(location, JAR_EXTENSION);
		URL xmlLocation = URLMetadataRepository.getActualLocation(location, XML_EXTENSION);
		// If the repository is local, we can return the repository file directly
		if (PROTOCOL_FILE.equals(xmlLocation.getProtocol())) {
			//look for a compressed local file
			localFile = new File(jarLocation.getPath());
			if (localFile.exists())
				return localFile;
			//look for an uncompressed local file
			localFile = new File(xmlLocation.getPath());
			if (localFile.exists())
				return localFile;
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, msg, null));
		}
		//file is not local, create a cache of the repository metadata
		localFile = Activator.getCacheManager().createCache(location, monitor);
		if (localFile == null) {
			//there is no remote file in either form
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, msg, null));
		}
		return localFile;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.IMetadataRepositoryFactory#validate(java.net.URL, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus validate(URL location, IProgressMonitor monitor) {
		try {
			validateAndLoad(location, false, monitor);
		} catch (ProvisionException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.IMetadataRepositoryFactory#load(java.net.URL, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IMetadataRepository load(URL location, IProgressMonitor monitor) throws ProvisionException {
		return validateAndLoad(location, true, monitor);
	}

	protected IMetadataRepository validateAndLoad(URL location, boolean doLoad, IProgressMonitor monitor) throws ProvisionException {
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
					if (jarEntry == null)
						throw new FileNotFoundException(NLS.bind(Messages.repoMan_invalidLocation, location));
				}
				//parse the repository descriptor file
				sub.setWorkRemaining(100);
				if (doLoad) {
					InputStream descriptorStream = jarStream != null ? jarStream : inStream;
					IMetadataRepository result = new MetadataRepositoryIO().read(localFile.toURL(), descriptorStream, sub.newChild(100));
					if (result instanceof LocalMetadataRepository)
						((LocalMetadataRepository) result).initializeAfterLoad(location);
					if (result instanceof URLMetadataRepository)
						((URLMetadataRepository) result).initializeAfterLoad(location);
					if (Tracing.DEBUG_METADATA_PARSING) {
						time += System.currentTimeMillis();
						Tracing.debug(debugMsg + "time (ms): " + time); //$NON-NLS-1$ 
					}
					return result;
				}
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
		return null;
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
