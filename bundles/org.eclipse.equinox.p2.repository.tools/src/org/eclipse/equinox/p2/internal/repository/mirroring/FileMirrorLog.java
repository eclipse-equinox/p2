/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.mirroring;

import java.io.*;
import java.util.Date;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

public class FileMirrorLog implements IArtifactMirrorLog {

	private static final String INDENT = "\t"; //$NON-NLS-1$
	private static final String SEPARATOR = System.lineSeparator();
	private BufferedWriter out;
	private boolean consoleMessage = false;
	private int minSeverity = IStatus.OK;
	private boolean hasRoot = false;

	public FileMirrorLog(String location, int minSeverity, String root) {
		this.minSeverity = minSeverity;
		try {
			File log = new File(location);
			if (log.getParentFile().exists() || log.getParentFile().mkdirs()) {
				out = new BufferedWriter(new FileWriter(log, true));
				if (root != null) {
					log(root + " - " + new Date()); //$NON-NLS-1$
					hasRoot = true;
				}
			} else {
				throw new IOException(Messages.exception_unableToCreateParentDir);
			}
		} catch (IOException e) {
			exceptionOccurred(null, e);
		}
	}

	@Override
	public void log(IArtifactDescriptor descriptor, IStatus status) {
		if (status.getSeverity() >= minSeverity) {
			log(descriptor.toString());
			log(status, INDENT);
		}
	}

	@Override
	public void log(IStatus status) {
		log(status, ""); //$NON-NLS-1$
	}

	/*
	 * Write a status to the log, indenting it based on status depth.
	 * 
	 * @param status the status to log
	 * 
	 * @param depth the depth of the status
	 */
	private void log(IStatus status, String prefix) {
		if (status.getSeverity() >= minSeverity) {
			// Write status to log
			log(prefix + status.getMessage());

			// Write exception to log if applicable
			String exceptionMessage = status.getException() != null ? status.getException().getMessage() : null;
			if (exceptionMessage != null) {
				log(prefix + exceptionMessage);
			}

			// Write the children of the status to the log
			IStatus[] nestedStatus = status.getChildren();
			if (nestedStatus != null) {
				for (IStatus s : nestedStatus) {
					log(s, prefix + INDENT);
				}
			}
		}
	}

	/*
	 * Write a message to the log
	 * 
	 * @param message the message to write
	 */
	private void log(String message) {
		try {
			out.write((hasRoot ? INDENT : "") + message + SEPARATOR); //$NON-NLS-1$
		} catch (IOException e) {
			exceptionOccurred((hasRoot ? INDENT : "") + message, e); //$NON-NLS-1$
		}
	}

	@Override
	public void close() {
		try {
			if (out != null) {
				out.close();
			}
		} catch (IOException e) {
			exceptionOccurred(null, e);
		}
	}

	/*
	 * Show an error message if this the first time, and print status messages.
	 */
	private void exceptionOccurred(String message, Exception e) {
		if (!consoleMessage) {
			System.err.println(Messages.MirrorLog_Exception_Occurred);
			e.printStackTrace(System.err);
			System.err.println(Messages.MirrorLog_Console_Log);
			consoleMessage = true;
		}
		if (message != null) {
			System.out.println(message);
		}
	}
}
