/****************************************************************************
 * Copyright (c) 2019 IBM, Composent Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *    Thomas Joiner - HttpClient 4 implementation
 *    Yatta Solutions - HttpClient 4.5 implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.provider.filetransfer.httpclient5;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.Assert;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.core.util.Proxy;
import org.eclipse.ecf.filetransfer.IRemoteFileSystemListener;
import org.eclipse.ecf.filetransfer.IRemoteFileSystemRequest;
import org.eclipse.ecf.filetransfer.RemoteFileSystemException;
import org.eclipse.ecf.filetransfer.identity.IFileID;
import org.eclipse.ecf.filetransfer.service.IRemoteFileSystemBrowser;
import org.eclipse.ecf.filetransfer.service.IRemoteFileSystemBrowserFactory;
import org.eclipse.ecf.internal.provider.filetransfer.httpclient5.Activator;
import org.eclipse.ecf.provider.filetransfer.identity.FileTransferNamespace;
import org.eclipse.osgi.util.NLS;

public class HttpClientBrowseFileTransferFactory implements IRemoteFileSystemBrowserFactory {

	@Override
	public IRemoteFileSystemBrowser newInstance() {
		return new IRemoteFileSystemBrowser() {

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

				HttpClientFileSystemBrowser browser = new HttpClientFileSystemBrowser(Activator.getDefault().getBrowseHttpClient(), directoryOrFileId, listener, url, connectContext, proxy);
				return browser.sendBrowseRequest();
			}

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

		};

	}
}
