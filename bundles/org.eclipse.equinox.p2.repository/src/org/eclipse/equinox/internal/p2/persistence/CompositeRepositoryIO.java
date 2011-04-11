/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.persistence;

import java.io.*;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.osgi.util.NLS;

/**
 * This class reads and writes repository metadata (e.g. table of contents files) 
 * for composite artifact and metadata repositories.
 * <p>
 * Note: This class is not used for reading or writing the actual composite repositories.
 */
public class CompositeRepositoryIO {

	/**
	 * Writes the given repository to the stream.
	 * This method performs buffering, and closes the stream when finished.
	 */
	public void write(CompositeRepositoryState repository, OutputStream output, String type) {
		OutputStream bufferedOutput = null;
		try {
			try {
				bufferedOutput = new BufferedOutputStream(output);
				CompositeWriter repositoryWriter = new CompositeWriter(bufferedOutput, type);
				repositoryWriter.write(repository);
			} finally {
				if (bufferedOutput != null) {
					bufferedOutput.close();
				}
			}
		} catch (IOException ioe) {
			// TODO shouldn't this throw a core exception?
			ioe.printStackTrace();
		}
	}

	/**
	 * Reads the composite repository from the given stream,
	 * and returns the contained array of abstract composite repositories.
	 * 
	 * This method performs buffering, and closes the stream when finished.
	 */
	public CompositeRepositoryState read(URL location, InputStream input, String type, IProgressMonitor monitor) throws ProvisionException {
		BufferedInputStream bufferedInput = null;
		try {
			try {
				bufferedInput = new BufferedInputStream(input);
				CompositeParser repositoryParser = new CompositeParser(Activator.getContext(), Activator.ID, type);
				repositoryParser.setErrorContext(location.toExternalForm());
				repositoryParser.parse(input);
				IStatus result = repositoryParser.getStatus();
				switch (result.getSeverity()) {
					case IStatus.CANCEL :
						throw new OperationCanceledException();
					case IStatus.ERROR :
						throw new ProvisionException(result);
					case IStatus.WARNING :
					case IStatus.INFO :
						LogHelper.log(result);
				}
				CompositeRepositoryState repositoryState = repositoryParser.getRepositoryState();
				if (repositoryState == null)
					throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, Messages.io_parseError, null));
				return repositoryState;
			} finally {
				if (bufferedInput != null)
					bufferedInput.close();
			}
		} catch (IOException ioe) {
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, ioe));
		}
	}
}
