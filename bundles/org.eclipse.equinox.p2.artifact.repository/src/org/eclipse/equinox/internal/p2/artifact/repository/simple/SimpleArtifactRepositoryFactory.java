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
package org.eclipse.equinox.internal.p2.artifact.repository.simple;

import java.io.*;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.repository.ECFTransport;
import org.eclipse.equinox.internal.p2.artifact.repository.Transport;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.core.repository.RepositoryCreationException;
import org.eclipse.equinox.spi.p2.artifact.repository.IArtifactRepositoryFactory;

public class SimpleArtifactRepositoryFactory implements IArtifactRepositoryFactory {

	public IArtifactRepository load(URL location) {
		if (location == null)
			return null;
		File temp = null;
		try {
			// TODO This temporary file stuff is not very elegant. 
			OutputStream artifacts = null;
			temp = File.createTempFile("artifacts", ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
			// try with gzip
			boolean gzip = true;
			try {
				artifacts = new BufferedOutputStream(new FileOutputStream(temp));
				IStatus status = getTransport().download(SimpleArtifactRepository.getActualLocation(location, gzip).toExternalForm(), artifacts, null);
				if (!status.isOK()) {
					// retry unzipped
					gzip = false;
					status = getTransport().download(SimpleArtifactRepository.getActualLocation(location, gzip).toExternalForm(), artifacts, null);
					if (!status.isOK())
						return null;
				}
			} finally {
				if (artifacts != null)
					artifacts.close();
			}
			InputStream descriptorStream = null;
			try {
				descriptorStream = new BufferedInputStream(new FileInputStream(temp));
				if (gzip)
					descriptorStream = new GZIPInputStream(descriptorStream);
				SimpleArtifactRepositoryIO io = new SimpleArtifactRepositoryIO();
				SimpleArtifactRepository result = (SimpleArtifactRepository) io.read(descriptorStream);
				result.initializeAfterLoad(location);
				return result;
			} catch (RepositoryCreationException e) {
				return null;
			} finally {
				if (descriptorStream != null)
					descriptorStream.close();
			}
		} catch (IOException e) {
			// TODO: should this distinguish between non-existent file
			//		 and other IO exceptions.
		} finally {
			if (temp != null && !temp.delete())
				temp.deleteOnExit();
		}
		return null;
	}

	public IArtifactRepository create(URL location, String name, String type) {
		return new SimpleArtifactRepository(name, location);
	}

	private Transport getTransport() {
		return ECFTransport.getInstance();
	}
}
