/****************************************************************************
 * Copyright (c) 2004, 2026 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.equinox.p2.transport.ecf.filetransfer.service;

import java.net.URI;
import java.util.Map;
import org.eclipse.equinox.internal.p2.transport.ecf.core.security.IConnectContext;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IFileTransferListener;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IncomingFileTransferException;

/**
 * Retrieve file transfer service interface.
 */
public interface IRetrieveFileTransfer {

	/**
	 * Send request to retrieve the given remote file, optionally starting at a
	 * given position.
	 *
	 * @param remoteFile       the location to retrieve
	 * @param startPosition    the position in bytes to start at, or -1 to
	 *                         retrieve the entire file
	 * @param transferListener the listener that receives the transfer events
	 * @param requestHeaders   additional request headers, may be <code>null</code>
	 * @throws IncomingFileTransferException if the request cannot be sent
	 */
	public void sendRetrieveRequest(URI remoteFile, long startPosition, IFileTransferListener transferListener, Map<String, String> requestHeaders) throws IncomingFileTransferException;

	/**
	 * Set the connect context used for authentication.
	 *
	 * @param connectContext the context, may be <code>null</code>
	 */
	public void setConnectContextForAuthentication(IConnectContext connectContext);
}
