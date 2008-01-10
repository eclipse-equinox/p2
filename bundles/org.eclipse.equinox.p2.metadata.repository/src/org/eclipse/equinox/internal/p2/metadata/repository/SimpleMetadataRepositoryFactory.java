/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.p2.core.repository.RepositoryCreationException;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.spi.p2.core.repository.AbstractRepository;
import org.eclipse.equinox.spi.p2.metadata.repository.IMetadataRepositoryFactory;

public class SimpleMetadataRepositoryFactory implements IMetadataRepositoryFactory {

	private static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	private static final String XML_EXTENSION = ".xml";

	public IMetadataRepository create(URL location, String name, String type) {
		if (location.getProtocol().equals("file")) //$NON-NLS-1$
			return new LocalMetadataRepository(location, name);
		return new URLMetadataRepository(location, name);
	}

	public IMetadataRepository load(URL location) {
		// load the jar
		IMetadataRepository result = load(location, JAR_EXTENSION);
		// compressed file is not available, load the xml
		if (result == null) {
			result = load(location, XML_EXTENSION);
		}
		return result;
	}

	private IMetadataRepository load(URL location, String type) {
		if (location == null)
			return null;
		long time = 0;
		final String debugMsg = "Restoring metadata repository "; //$NON-NLS-1$
		if (Tracing.DEBUG_METADATA_PARSING) {
			Tracing.debug(debugMsg + location);
			time = -System.currentTimeMillis();
		}
		try {
			URL actualFile = URLMetadataRepository.getActualLocation(location);
			InputStream inStream = URLMetadataRepository.getActualLocation(location, type).openStream();
			if (JAR_EXTENSION.equalsIgnoreCase(type)) {
				JarInputStream jInStream = new JarInputStream(inStream);
				JarEntry jarEntry = jInStream.getNextJarEntry();
				String entryName = new Path(actualFile.getPath()).lastSegment();
				while (jarEntry != null && (!entryName.equals(jarEntry.getName()))) {
					jarEntry = jInStream.getNextJarEntry();
				}
				if (jarEntry == null) {
					throw new FileNotFoundException("Repository not found in " + actualFile.getPath() + type); //$NON-NLS-1$
				}
				inStream = jInStream;
			}
			InputStream descriptorStream = new BufferedInputStream(inStream);
			try {
				IMetadataRepository result = new MetadataRepositoryIO().read(descriptorStream);
				if (result instanceof LocalMetadataRepository)
					((LocalMetadataRepository) result).initializeAfterLoad(location);
				if (result instanceof URLMetadataRepository)
					((URLMetadataRepository) result).initializeAfterLoad(location);
				if (Tracing.DEBUG_METADATA_PARSING) {
					time += System.currentTimeMillis();
					Tracing.debug(debugMsg + "time (ms): " + time); //$NON-NLS-1$ 
				}
				return result;
			} catch (RepositoryCreationException e) {
				log(debugMsg, e);
			} finally {
				if (descriptorStream != null)
					descriptorStream.close();
			}
		} catch (FileNotFoundException e) {
			//if the repository doesn't exist, then it's fine to return null
		} catch (IOException e) {
			log(debugMsg, e);
		}
		return null;
	}

	private void log(String message, Exception e) {
		LogHelper.log(new Status(IStatus.ERROR, Activator.PI_METADATA_REPOSITORY, message, e));
	}

	public void restore(AbstractRepository repository, URL location) {
		AbstractRepository source = (AbstractRepository) load(location);
		if (source == null)
			return;
		if (repository.getClass() != source.getClass())
			throw new IllegalArgumentException("Repository type mismatch"); //$NON-NLS-1$
		if (repository instanceof LocalMetadataRepository)
			((LocalMetadataRepository) repository).revertToBackup((LocalMetadataRepository) source);
		else if (repository instanceof URLMetadataRepository)
			((URLMetadataRepository) repository).revertToBackup((URLMetadataRepository) source);
	}
}
