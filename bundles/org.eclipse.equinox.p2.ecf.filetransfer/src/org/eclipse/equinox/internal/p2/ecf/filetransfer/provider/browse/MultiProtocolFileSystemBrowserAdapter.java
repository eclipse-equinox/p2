/****************************************************************************
 * Copyright (c) 2004, 2007 Composent, Inc. and others.
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

package org.eclipse.equinox.internal.p2.ecf.filetransfer.provider.browse;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.core.identity.IDFactory;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.core.identity.Namespace;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.core.security.IConnectContext;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.core.util.Proxy;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.IRemoteFileSystemBrowserContainerAdapter;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.IRemoteFileSystemListener;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.IRemoteFileSystemRequest;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.RemoteFileSystemException;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.identity.IFileID;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.service.IRemoteFileSystemBrowser;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.internal.Activator;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.internal.Messages;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.provider.identity.FileTransferNamespace;

/**
 * Multi protocol handler for remote file system browser. 
 */
public class MultiProtocolFileSystemBrowserAdapter implements IRemoteFileSystemBrowser {

	IConnectContext connectContext = null;
	Proxy proxy = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.p2.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#setConnectContextForAuthentication(org.eclipse.equinox.internal.p2.ecf.filetransfer.core.security.IConnectContext)
	 */
	public void setConnectContextForAuthentication(IConnectContext connectContext) {
		this.connectContext = connectContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.p2.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#setProxy(org.eclipse.equinox.internal.p2.ecf.filetransfer.core.util.Proxy)
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ecf.filetransfer.IRemoteFileSystemBrowserContainerAdapter#getDirectoryNamespace()
	 */
	public Namespace getBrowseNamespace() {
		return IDFactory.getDefault().getNamespaceByName(FileTransferNamespace.PROTOCOL);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ecf.filetransfer.IRemoteFileSystemBrowserContainerAdapter#sendDirectoryRequest(org.eclipse.equinox.internal.p2.ecf.filetransfer.identity.IFileID, org.eclipse.equinox.internal.p2.ecf.filetransfer.IRemoteFileSystemListener)
	 */
	public IRemoteFileSystemRequest sendBrowseRequest(IFileID directoryOrFileID, IRemoteFileSystemListener listener) throws RemoteFileSystemException {
		Assert.isNotNull(directoryOrFileID);
		Assert.isNotNull(listener);
		URL url;
		try {
			url = directoryOrFileID.getURL();
		} catch (final MalformedURLException e) {
			throw new RemoteFileSystemException(Messages.AbstractRetrieveFileTransfer_MalformedURLException);
		}

		IRemoteFileSystemBrowserContainerAdapter fileSystemBrowser = null;
		fileSystemBrowser = Activator.getDefault().getBrowseFileTransfer(url.getProtocol());

		if (fileSystemBrowser == null) {
			if (url.getProtocol().equalsIgnoreCase("file")) { //$NON-NLS-1$
				LocalFileSystemBrowser fsb = new LocalFileSystemBrowser(directoryOrFileID, listener);
				return fsb.sendBrowseRequest();
			}
			URLFileSystemBrowser ufsb = new URLFileSystemBrowser(directoryOrFileID, listener, url, connectContext, proxy);
			return ufsb.sendBrowseRequest();
		}

		// Set connect context
		fileSystemBrowser.setConnectContextForAuthentication(connectContext);
		// Set Proxy
		fileSystemBrowser.setProxy(proxy);

		return fileSystemBrowser.sendBrowseRequest(directoryOrFileID, listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == null)
			return null;
		final IAdapterManager adapterManager = Activator.getDefault().getAdapterManager();
		if (adapterManager == null)
			return null;
		return (T) adapterManager.loadAdapter(this, adapter.getName());
	}

}
