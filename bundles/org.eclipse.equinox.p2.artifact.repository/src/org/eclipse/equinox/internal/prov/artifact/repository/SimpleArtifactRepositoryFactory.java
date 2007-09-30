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
package org.eclipse.equinox.internal.prov.artifact.repository;

import java.io.*;
import java.net.URL;
import org.eclipse.equinox.prov.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.prov.artifact.repository.IArtifactRepositoryFactory;
import org.eclipse.equinox.prov.core.repository.RepositoryCreationException;

public class SimpleArtifactRepositoryFactory implements IArtifactRepositoryFactory {

	public IArtifactRepository load(URL location) {
		if (location == null)
			return null;
		try {
			InputStream descriptorStream = null;
			try {
				descriptorStream = new BufferedInputStream(SimpleArtifactRepository.getActualLocation(location).openStream());
				SimpleArtifactRepository result = (SimpleArtifactRepository) ArtifactRepositoryIO.read(descriptorStream);
				result.initializeAfterLoad(location);
				return result;
			} catch (RepositoryCreationException e) {
				// TODO Auto-generated catch block
				return null;
			} finally {
				if (descriptorStream != null)
					descriptorStream.close();
			}
		} catch (IOException e) {
		}
		return null;
	}

	public IArtifactRepository create(URL location, String name, String type) {
		return new SimpleArtifactRepository(name, location);
	}
}
