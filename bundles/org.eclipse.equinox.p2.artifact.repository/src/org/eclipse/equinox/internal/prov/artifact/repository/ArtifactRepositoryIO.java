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

import com.thoughtworks.xstream.XStream;
import java.io.*;
import org.eclipse.equinox.prov.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.prov.core.repository.RepositoryCreationException;

/**
 * This class reads and writes artifact repository 
 * (eg. table of contents files);
 * 
 * This class is not used for reading or writing the actual artifacts.
 * 
 * The implementation currently uses XStream.
 */
class ArtifactRepositoryIO {

	/**
	 * Reads the artifact repository from the given stream,
	 * and returns the contained array of abstract artifact repositories.
	 * 
	 * This method performs buffering, and closes the stream when finished.
	 */
	public static IArtifactRepository read(InputStream input) throws RepositoryCreationException {
		XStream stream = new XStream();
		BufferedInputStream bufferedInput = null;
		try {
			try {
				bufferedInput = new BufferedInputStream(input);
				return (IArtifactRepository) stream.fromXML(bufferedInput);
			} finally {
				if (bufferedInput != null)
					bufferedInput.close();
			}
		} catch (IOException e) {
			throw new RepositoryCreationException(e);
		}
	}

	/**
	 * Writes the given artifact repository to the stream.
	 * This method performs buffering, and closes the stream when finished.
	 */
	public static void write(IArtifactRepository repository, OutputStream output) {
		XStream stream = new XStream();
		OutputStream bufferedOutput = null;
		try {
			try {
				bufferedOutput = new BufferedOutputStream(output);
				stream.toXML(repository, bufferedOutput);
			} finally {
				if (bufferedOutput != null) {
					bufferedOutput.close();
				}
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
