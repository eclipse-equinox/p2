/****************************************************************************
 * Copyright (c) 2007, 2026 Composent, Inc. and others.
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
import org.eclipse.equinox.internal.p2.transport.ecf.core.security.IConnectContext;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFileSystemListener;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFileSystemRequest;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.RemoteFileSystemException;

/**
 * Remote file system browser service interface.
 */
public interface IRemoteFileSystemBrowser {

	/**
	 * Send request to browse the given remote file or directory.
	 *
	 * @param directoryOrFile the location to browse
	 * @param listener        the listener that receives the result
	 * @return the request, which can be used to cancel it
	 * @throws RemoteFileSystemException if the request cannot be sent
	 */
	public IRemoteFileSystemRequest sendBrowseRequest(URI directoryOrFile, IRemoteFileSystemListener listener) throws RemoteFileSystemException;

	/**
	 * Set the connect context used for authentication.
	 *
	 * @param connectContext the context, may be <code>null</code>
	 */
	public void setConnectContextForAuthentication(IConnectContext connectContext);
}
