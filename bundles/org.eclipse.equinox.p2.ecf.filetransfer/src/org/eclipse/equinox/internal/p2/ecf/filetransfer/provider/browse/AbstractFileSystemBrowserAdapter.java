/*******************************************************************************
 * Copyright (c) 2026 Aleksandar Kurtakov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Aleksandar Kurtakov - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ecf.filetransfer.provider.browse;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.*;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.core.identity.IDFactory;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.core.identity.Namespace;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.core.security.IConnectContext;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.core.util.Proxy;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.identity.IFileID;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.provider.identity.FileTransferNamespace;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.service.IRemoteFileSystemBrowser;
import org.eclipse.osgi.util.NLS;

/**
 * Base class for the {@link IRemoteFileSystemBrowser} of a file transfer
 * provider. It keeps the connect context and the proxy and creates a new
 * {@link AbstractFileSystemBrowser} for every browse request.
 */
public abstract class AbstractFileSystemBrowserAdapter implements IRemoteFileSystemBrowser {

	private Proxy proxy;
	private IConnectContext connectContext;

	@Override
	public Namespace getBrowseNamespace() {
		return IDFactory.getDefault().getNamespaceByName(FileTransferNamespace.PROTOCOL);
	}

	@Override
	public IRemoteFileSystemRequest sendBrowseRequest(IFileID directoryOrFileId, IRemoteFileSystemListener listener) throws RemoteFileSystemException {
		Assert.isNotNull(directoryOrFileId);
		Assert.isNotNull(listener);
		URL url;
		try {
			url = directoryOrFileId.getURL();
		} catch (final MalformedURLException e) {
			throw new RemoteFileSystemException(NLS.bind("Exception creating URL for {0}", directoryOrFileId)); //$NON-NLS-1$
		}
		return createBrowser(directoryOrFileId, listener, url, connectContext, proxy).sendBrowseRequest();
	}

	/**
	 * Creates the browser that handles a single browse request.
	 */
	protected abstract AbstractFileSystemBrowser createBrowser(IFileID directoryOrFileId, IRemoteFileSystemListener listener, URL url, IConnectContext context, Proxy proxyToUse);

	@Override
	public void setConnectContextForAuthentication(IConnectContext connectContext) {
		this.connectContext = connectContext;
	}

	@Override
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}
}
