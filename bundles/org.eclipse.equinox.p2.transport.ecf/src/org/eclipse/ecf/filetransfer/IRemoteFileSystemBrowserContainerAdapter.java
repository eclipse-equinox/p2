/****************************************************************************
 * Copyright (c) 2007 Composent, Inc. and others.
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

package org.eclipse.ecf.filetransfer;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.core.util.Proxy;
import org.eclipse.ecf.filetransfer.identity.IFileID;

/**
 * Remote file system browser adapter.  This adapter can be retrieved from a container
 * for exposing remote file system browsing capabilities.
 */
public interface IRemoteFileSystemBrowserContainerAdapter extends IAdaptable {

	/**
	 * Get the {@link Namespace} instance for creating IFileIDs that represent remote files or directories.
	 * 
	 * @return Namespace for remote files or directories.  Will not be <code>null</code>.
	 */
	public Namespace getBrowseNamespace();

	/**
	 * Send a request for file or directory information for given directoryOrFileID.
	 * @param directoryOrFileID the IFileID representing/specifying the remote directory or file to access.
	 * @param listener the listener that will be notified asynchronously when a response to this request is received.  Must not be
	 * <code>null</code>.  
	 * @return IRemoteFileSystemRequest the request instance.
	 * @throws RemoteFileSystemException if browse request cannot be accomplished
	 */
	public IRemoteFileSystemRequest sendBrowseRequest(IFileID directoryOrFileID, IRemoteFileSystemListener listener) throws RemoteFileSystemException;

	/**
	 * Set connect context for authentication upon subsequent
	 * {@link #sendBrowseRequest(IFileID, IRemoteFileSystemListener)}. This
	 * method should be called with a non-null connectContext in order to allow
	 * authentication to occur during call to
	 * {@link #sendBrowseRequest(IFileID, IRemoteFileSystemListener)}.
	 * 
	 * @param connectContext
	 *            the connect context to use for authenticating during
	 *            subsequent call to
	 *            {@link #sendBrowseRequest(IFileID, IRemoteFileSystemListener)}.
	 *            If <code>null</code>, then no authentication will be
	 *            attempted.
	 */
	public void setConnectContextForAuthentication(IConnectContext connectContext);

	/**
	 * Set proxy for use upon subsequent
	 * {@link #sendBrowseRequest(IFileID, IRemoteFileSystemListener)}. This
	 * method should be called with a non-null proxy to allow the given proxy to
	 * be used in subsequent calls to
	 * {@link #sendBrowseRequest(IFileID, IRemoteFileSystemListener)}.
	 * 
	 * @param proxy
	 *            the proxy to use for subsequent calls to
	 *            {@link #sendBrowseRequest(IFileID, IRemoteFileSystemListener)}.
	 *            If <code>null</code>, then no proxy will be used.
	 */
	public void setProxy(Proxy proxy);

}
