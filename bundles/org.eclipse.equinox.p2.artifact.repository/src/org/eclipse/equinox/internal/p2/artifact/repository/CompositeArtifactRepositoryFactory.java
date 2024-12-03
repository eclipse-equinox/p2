/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
 *     Sonatype Inc - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.*;
import java.net.URI;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryIO;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryState;
import org.eclipse.equinox.internal.p2.repository.CacheManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;
import org.eclipse.osgi.util.NLS;

public class CompositeArtifactRepositoryFactory extends ArtifactRepositoryFactory {

	private static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	private static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	private static final String PROTOCOL_FILE = "file"; //$NON-NLS-1$

	private IArtifactRepositoryManager getManager() {
		if (getAgent() != null)
			return getAgent().getService(IArtifactRepositoryManager.class);
		return null;
	}

	/**
	 * Returns a file in the local file system that contains the contents of the
	 * metadata repository at the given location.
	 */
	private File getLocalFile(URI location, IProgressMonitor monitor) throws IOException, ProvisionException {
		File localFile = null;
		URI jarLocation = CompositeArtifactRepository.getActualLocation(location, true);
		URI xmlLocation = CompositeArtifactRepository.getActualLocation(location, false);
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
		//file is not local, create a cache of the repository metadata
		CacheManager cache = getAgent().getService(CacheManager.class);
		if (cache == null)
			throw new IllegalArgumentException("Cache manager service not available"); //$NON-NLS-1$
		localFile = cache.createCache(location, CompositeArtifactRepository.CONTENT_FILENAME, monitor);
		if (localFile == null) {
			//there is no remote file in either form
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, msg, null));
		}
		return localFile;
	}

	@Override
	public IArtifactRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
		long time = 0;
		final String debugMsg = "Restoring artifact repository "; //$NON-NLS-1$
		if (Tracing.DEBUG_METADATA_PARSING) {
			Tracing.debug(debugMsg + location);
			time = -System.currentTimeMillis();
		}

		SubMonitor sub = SubMonitor.convert(monitor, 400);
		try {
			//non local repos are not modifiable
			if (!PROTOCOL_FILE.equals(location.getScheme()) && (flags & IRepositoryManager.REPOSITORY_HINT_MODIFIABLE) > 0)
				return null;

			File localFile = getLocalFile(location, sub.newChild(100));
			InputStream inStream = new BufferedInputStream(new FileInputStream(localFile));
			JarInputStream jarStream = null;
			try {
				//if reading from a jar, obtain a stream on the entry with the actual contents
				if (localFile.getAbsolutePath().endsWith(JAR_EXTENSION)) {
					jarStream = new JarInputStream(inStream);
					JarEntry jarEntry = jarStream.getNextJarEntry();
					String entryName = CompositeArtifactRepository.CONTENT_FILENAME + XML_EXTENSION;
					while (jarEntry != null && (!entryName.equals(jarEntry.getName()))) {
						jarEntry = jarStream.getNextJarEntry();
					}

					//there is a jar but the entry is missing or invalid, so treat this as an invalid repository
					if (jarEntry == null)
						throw new IOException(NLS.bind(Messages.io_invalidLocation, location.getPath()));
				}
				//parse the repository descriptor file
				sub.setWorkRemaining(300);
				InputStream descriptorStream = jarStream != null ? jarStream : inStream;
				CompositeRepositoryIO io = new CompositeRepositoryIO();
				CompositeRepositoryState resultState = io.read(localFile.toURL(), descriptorStream, CompositeArtifactRepository.PI_REPOSITORY_TYPE, sub.newChild(100));
				if (resultState.getLocation() == null)
					resultState.setLocation(location);
				// Spending half the time in creating the repo is due to the loading of the children that happens during that period
				CompositeArtifactRepository result = new CompositeArtifactRepository(getManager(), resultState, sub.newChild(200));
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
		}
	}

	@Override
	public IArtifactRepository create(URI location, String name, String type, Map<String, String> properties) {
		return new CompositeArtifactRepository(getManager(), location, name, properties);
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
