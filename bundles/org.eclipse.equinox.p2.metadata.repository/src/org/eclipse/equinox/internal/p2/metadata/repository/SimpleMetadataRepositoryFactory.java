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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.core.repository.RepositoryCreationException;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.spi.p2.core.repository.AbstractRepository;
import org.eclipse.equinox.spi.p2.metadata.repository.IMetadataRepositoryFactory;

public class SimpleMetadataRepositoryFactory implements IMetadataRepositoryFactory {

	public IMetadataRepository create(URL location, String name, String type) {
		try {
			if (location.getProtocol().equals("file")) //$NON-NLS-1$
				return new LocalMetadataRepository(location, name);
			return new URLMetadataRepository(location, name);
		} catch (RepositoryCreationException e) {
			log(e);
			// if the exception has no cause then it was just a missing repo so we'll return null 
			return null;
		}
	}

	public IMetadataRepository load(URL location) {
		if (location == null)
			return null;
		try {
			InputStream descriptorStream = new BufferedInputStream(URLMetadataRepository.getActualLocation(location).openStream());
			try {
				IMetadataRepository result = new MetadataRepositoryIO().read(descriptorStream);
				if (result instanceof LocalMetadataRepository)
					((LocalMetadataRepository) result).initializeAfterLoad(location);
				if (result instanceof URLMetadataRepository)
					((URLMetadataRepository) result).initializeAfterLoad(location);
				return result;
			} catch (RepositoryCreationException e) {
				// TODO: distinguish between nonexistent input file and other creation problems.
				log(e);
			} finally {
				if (descriptorStream != null)
					descriptorStream.close();
			}
		} catch (IOException e) {
			log(e);
		}
		return null;
	}

	private void log(Exception e) {
		LogHelper.log(new Status(IStatus.ERROR, Activator.PI_METADATA_REPOSITORY, "Error loading repository", e));
	}

	public void restore(AbstractRepository repository, URL location) {
		AbstractRepository source = (AbstractRepository) load(location);
		if (repository.getClass() != source.getClass())
			throw new IllegalArgumentException("Repository type mismatch");
		if (repository instanceof LocalMetadataRepository)
			((LocalMetadataRepository) repository).revertToBackup((LocalMetadataRepository) source);
		else if (repository instanceof URLMetadataRepository)
			((URLMetadataRepository) repository).revertToBackup((URLMetadataRepository) source);
	}
}
