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
import org.eclipse.equinox.p2.core.repository.RepositoryCreationException;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryFactory;

public class SimpleMetadataRepositoryFactory implements IMetadataRepositoryFactory {

	public IMetadataRepository load(URL location) {
		if (location == null)
			return null;
		try {
			InputStream descriptorStream = new BufferedInputStream(URLMetadataRepository.getActualLocation(location).openStream());
			try {
				IMetadataRepository result = MetadataRepositoryIO.read(descriptorStream);
				if (result instanceof LocalMetadataRepository)
					((LocalMetadataRepository) result).initializeAfterLoad(location);
				if (result instanceof URLMetadataRepository)
					((URLMetadataRepository) result).initializeAfterLoad(location);
				return result;
			} catch (RepositoryCreationException e) {
				// TODO Auto-generated catch block
				return null;
			} finally {
				if (descriptorStream != null)
					descriptorStream.close();
			}
		} catch (IOException e) {
			//TODO: log and throw? 
		}
		return null;
	}

	public IMetadataRepository create(URL location, String name, String type) {
		try {
			if (location.getProtocol().equals("file")) //$NON-NLS-1$
				return new LocalMetadataRepository(location, name);
			return new URLMetadataRepository(location, name);
		} catch (RepositoryCreationException e) {
			// if the exception has no cause then it was just a missing repo so we'll return null 
			return null;
		}
	}

	public void restore(AbstractMetadataRepository repository, URL location) {
		AbstractMetadataRepository source = (AbstractMetadataRepository) load(location);
		repository.description = source.description;
		repository.name = source.name;
		repository.properties = source.properties;
		repository.provider = source.provider;
		repository.type = source.type;
		repository.version = source.version;
		repository.units = source.units;
	}
}
