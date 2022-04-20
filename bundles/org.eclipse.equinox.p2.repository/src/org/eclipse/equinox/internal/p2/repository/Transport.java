/*******************************************************************************
 *  Copyright (c) 2007, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - transport split
 *     Christoph Läubrich - Issue #6 - Deprecate Transport.download(URI, OutputStream, long, IProgressMonitor)
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import java.io.*;
import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;

public abstract class Transport {

	public static final String SERVICE_NAME = Transport.class.getName();

	/**
	 * Perform a download, writing into the target output stream. Progress is
	 * reported on the monitor. If the <code>target</code> is an instance of
	 * {@link IStateful} the resulting status is also set on the target. An
	 * IStateful target is updated with status even if this methods throws
	 * {@link OperationCanceledException}.
	 *
	 * @returns IStatus, that is a {@link DownloadStatus} on success.
	 * @param toDownload URI of file to download
	 * @param target     OutputStream where result is written
	 * @param startPos   the starting position of the download, or -1 for from start
	 * @param monitor    where progress should be reported
	 * @throws OperationCanceledException if the operation was canceled.
	 * @deprecated this method is actually never called from P2 code and thus
	 *             deprecated, callers should migrate to
	 *             {@link #download(URI, OutputStream, IProgressMonitor)},
	 *             implementors should simply remove this and rely on the default
	 *             implementation
	 */
	@Deprecated(forRemoval = true)
	public IStatus download(URI toDownload, OutputStream target, long startPos, IProgressMonitor monitor) {
		if (startPos <= 0) {
			return download(toDownload, target, monitor);
		}
		throw new UnsupportedOperationException(
				"positional downloads are actually never called from P2 code and thus disabled by default, please use the method without a position instead"); //$NON-NLS-1$
	}

	/**
	 * Perform a download, writing into the target output stream. Progress is reported on the
	 * monitor. If the <code>target</code> is an instance of {@link IStateful} the resulting status
	 * is also set on the target.
	 *
	 * @returns IStatus, that is a {@link DownloadStatus} on success.
	 * @param toDownload URI of file to download
	 * @param target OutputStream where result is written
	 * @param monitor where progress should be reported
	 * @throws OperationCanceledException if the operation was canceled.
	 */
	public abstract IStatus download(URI toDownload, OutputStream target, IProgressMonitor monitor);

	/**
	 * Perform a stream download, writing into an InputStream that is returned. Performs authentication if needed.
	 *
	 * @returns InputStream a stream with the content from the toDownload URI, or null
	 * @param toDownload URI of file to download
	 * @param monitor monitor checked for cancellation
	 * @throws OperationCanceledException if the operation was canceled.
	 * @throws AuthenticationFailedException if authentication failed, or too many attempt were made
	 * @throws FileNotFoundException if the toDownload was reported as non existing
	 * @throws CoreException on errors
	 */
	public abstract InputStream stream(URI toDownload, IProgressMonitor monitor) throws FileNotFoundException, CoreException, AuthenticationFailedException;

	/**
	 * Returns the last modified date for a URI. A last modified of 0 typically indicates that
	 * the server response is wrong, but should not be interpreted as a file not found.
	 * @param toDownload
	 * @param monitor
	 * @throws OperationCanceledException if the operation was canceled by the user.
	 * @return last modified date (possibly 0)
	 */
	public abstract long getLastModified(URI toDownload, IProgressMonitor monitor) throws CoreException, FileNotFoundException, AuthenticationFailedException;

}
