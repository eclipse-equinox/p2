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
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.io.*;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.spi.p2.metadata.repository.IMetadataRepositoryFactory;
import org.eclipse.osgi.util.NLS;

public class SimpleMetadataRepositoryFactory implements IMetadataRepositoryFactory {

	private static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	private static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	private static final String CONTENT_FILENAME = "content"; //$NON-NLS-1$

	public IMetadataRepository create(URL location, String name, String type) {
		if (location.getProtocol().equals("file")) //$NON-NLS-1$
			return new LocalMetadataRepository(location, name);
		return new URLMetadataRepository(location, name);
	}

	public IMetadataRepository load(URL location, IProgressMonitor monitor) throws ProvisionException {
		long time = 0;
		boolean compress = true;
		final String debugMsg = "Restoring metadata repository "; //$NON-NLS-1$
		File temp = null;
		if (Tracing.DEBUG_METADATA_PARSING) {
			Tracing.debug(debugMsg + location);
			time = -System.currentTimeMillis();
		}
		try {
			temp = File.createTempFile(CONTENT_FILENAME, JAR_EXTENSION);
			SubMonitor sub = SubMonitor.convert(monitor, 300);
			OutputStream metadata = new BufferedOutputStream(new FileOutputStream(temp));
			IStatus status = getTransport().download(URLMetadataRepository.getActualLocation(location, JAR_EXTENSION).toExternalForm(), metadata, sub);
			URL actualFile = URLMetadataRepository.getActualLocation(location);
			if (!status.isOK()) {
				// retry uncompressed
				metadata.close();
				if (!temp.delete()) {
					temp.deleteOnExit();
				}
				temp = File.createTempFile(CONTENT_FILENAME, XML_EXTENSION);
				metadata = new BufferedOutputStream(new FileOutputStream(temp));
				compress = false;
				status = getTransport().download(URLMetadataRepository.getActualLocation(location, XML_EXTENSION).toExternalForm(), metadata, sub);
				if (!status.isOK())
					return null;
			}
			if (metadata != null) {
				metadata.close();
			}
			InputStream inStream = new BufferedInputStream(new FileInputStream(temp));
			if (compress) {
				JarInputStream jInStream = new JarInputStream(inStream);
				JarEntry jarEntry = jInStream.getNextJarEntry();
				String entryName = new Path(actualFile.getPath()).lastSegment();
				while (jarEntry != null && (!entryName.equals(jarEntry.getName()))) {
					jarEntry = jInStream.getNextJarEntry();
				}
				if (jarEntry == null) {
					throw new FileNotFoundException(actualFile.getPath().toString());
				}
				inStream = jInStream;
			}
			InputStream descriptorStream = new BufferedInputStream(inStream);
			try {
				IMetadataRepository result = new MetadataRepositoryIO().read(temp.toURL(), descriptorStream, sub.newChild(100));
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
			if (temp != null && !temp.delete()) {
				temp.deleteOnExit();
			}
		}
	}

	private ECFMetadataTransport getTransport() {
		return ECFMetadataTransport.getInstance();
	}
}
