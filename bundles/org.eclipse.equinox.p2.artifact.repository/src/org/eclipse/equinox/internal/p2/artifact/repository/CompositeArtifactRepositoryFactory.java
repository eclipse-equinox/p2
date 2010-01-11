/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;

import org.eclipse.equinox.p2.core.ProvisionException;

import java.io.*;
import java.net.URI;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryIO;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryState;
import org.eclipse.equinox.internal.p2.repository.RepositoryTransport;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.osgi.util.NLS;

public class CompositeArtifactRepositoryFactory extends ArtifactRepositoryFactory {

	private IArtifactRepositoryManager getManager() {
		if (getAgent() != null)
			return (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		return null;
	}

	public IArtifactRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
		final String PROTOCOL_FILE = "file"; //$NON-NLS-1$
		long time = 0;
		final String debugMsg = "Restoring artifact repository "; //$NON-NLS-1$
		if (Tracing.DEBUG_METADATA_PARSING) {
			Tracing.debug(debugMsg + location);
			time = -System.currentTimeMillis();
		}
		File localFile = null;
		boolean local = false;
		try {
			SubMonitor sub = SubMonitor.convert(monitor, 300);
			OutputStream artifacts = null;
			// try with compressed
			boolean compress = true;
			if (PROTOCOL_FILE.equals(location.getScheme())) {
				local = true;
				localFile = URIUtil.toFile(CompositeArtifactRepository.getActualLocation(location, true));
				if (!localFile.exists()) {
					localFile = URIUtil.toFile(CompositeArtifactRepository.getActualLocation(location, false));
					compress = false;
				}
			} else {
				//not local, return null if the caller wanted a modifiable repo
				if ((flags & IRepositoryManager.REPOSITORY_HINT_MODIFIABLE) > 0) {
					return null;
				}

				//download to local temp file
				localFile = File.createTempFile(CompositeArtifactRepository.CONTENT_FILENAME, CompositeArtifactRepository.XML_EXTENSION);
				try {
					artifacts = new BufferedOutputStream(new FileOutputStream(localFile));
					IStatus status = getTransport().download(CompositeArtifactRepository.getActualLocation(location, compress), artifacts, sub.newChild(100));
					if (!status.isOK()) {
						// retry uncompressed
						compress = false;
						status = getTransport().download(CompositeArtifactRepository.getActualLocation(location, compress), artifacts, sub.newChild(100));
						if (!status.isOK())
							throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, status.getMessage(), null));
					}
				} finally {
					if (artifacts != null)
						artifacts.close();
				}
			}
			InputStream descriptorStream = null;
			try {
				descriptorStream = new BufferedInputStream(new FileInputStream(localFile));
				if (compress) {
					URI actualLocation = CompositeArtifactRepository.getActualLocation(location, false);
					JarInputStream jInStream = new JarInputStream(descriptorStream);
					JarEntry jarEntry = jInStream.getNextJarEntry();
					String filename = URIUtil.lastSegment(actualLocation);
					while (jarEntry != null && filename != null && !(filename.equals(jarEntry.getName()))) {
						jarEntry = jInStream.getNextJarEntry();
					}
					if (jarEntry == null) {
						//there is a jar but the entry is missing or invalid, so treat this as an invalid repository
						throw new IOException(NLS.bind(Messages.io_invalidLocation, actualLocation.getPath()));
					}
					descriptorStream = jInStream;
				}
				CompositeRepositoryIO io = new CompositeRepositoryIO();
				CompositeRepositoryState resultState = io.read(localFile.toURL(), descriptorStream, CompositeArtifactRepository.PI_REPOSITORY_TYPE, sub.newChild(100));
				if (resultState.getLocation() == null)
					resultState.setLocation(location);
				CompositeArtifactRepository result = new CompositeArtifactRepository(getManager(), resultState);
				if (Tracing.DEBUG_METADATA_PARSING) {
					time += System.currentTimeMillis();
					Tracing.debug(debugMsg + "time (ms): " + time); //$NON-NLS-1$ 
				}
				return result;
			} finally {
				if (descriptorStream != null)
					descriptorStream.close();
			}
		} catch (FileNotFoundException e) {
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, msg, e));
		} catch (IOException e) {
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} finally {
			if (!local && localFile != null && !localFile.delete())
				localFile.deleteOnExit();
		}
	}

	public IArtifactRepository create(URI location, String name, String type, Map<String, String> properties) {
		return new CompositeArtifactRepository(getManager(), location, name, properties);
	}

	private Transport getTransport() {
		return RepositoryTransport.getInstance();
	}
}
