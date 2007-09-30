/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Prashant Deva - Bug 194674 [prov] Provide write access to metadata repository
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import com.thoughtworks.xstream.XStream;
import java.io.*;
import org.eclipse.equinox.p2.core.repository.RepositoryCreationException;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;

/**
 * This class reads and writes provisioning metadata.
 * The implementation currently uses XStream.
 */
class MetadataRepositoryIO {

	/**
	 * Reads metadata from the given stream, and returns the contained array
	 * of abstract metadata repositories.
	 * This method performs buffering, and closes the stream when finished.
	 */
	public static IMetadataRepository read(InputStream input) throws RepositoryCreationException {
		XStream stream = new XStream();
		BufferedInputStream bufferedInput = null;
		try {
			try {
				bufferedInput = new BufferedInputStream(input);
				return (IMetadataRepository) stream.fromXML(bufferedInput);
			} finally {
				if (bufferedInput != null)
					bufferedInput.close();
			}
		} catch (IOException e) {
			throw new RepositoryCreationException(e);
		}
	}

	public static void write(AbstractMetadataRepository repository, OutputStream output) {
		XStream stream = new XStream();
		OutputStream bufferedOutput = null;
		try {
			try {
				bufferedOutput = new BufferedOutputStream(output);
				stream.toXML(repository, bufferedOutput);
			} finally {
				if (bufferedOutput != null)
					bufferedOutput.close();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
