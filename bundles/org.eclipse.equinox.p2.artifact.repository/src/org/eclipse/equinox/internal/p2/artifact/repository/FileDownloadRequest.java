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
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.osgi.util.NLS;

public class FileDownloadRequest extends ArtifactRequest {
	public static FileDownloadRequest[] NO_REQUEST = new FileDownloadRequest[0];

	private File destination; // The fully qualified path where the file should be written

	public FileDownloadRequest(IArtifactKey downloadKey, IPath downloadPath) {
		super(downloadKey);
		destination = downloadPath.toFile();
	}

	public void perform(IProgressMonitor monitor) {
		try {
			OutputStream destinationStream = null;
			try {
				destinationStream = new BufferedOutputStream(new FileOutputStream(destination));
				setResult(source.getArtifact(descriptor, destinationStream, null));
			} finally {
				if (destinationStream != null)
					destinationStream.close();
			}
		} catch (IOException e) {
			setResult(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.FileDownloadError, descriptor, destination), e));
		};
	}
}
