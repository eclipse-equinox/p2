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
package org.eclipse.equinox.internal.p2.transport.ecf.httpclient5.provider;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.internal.p2.transport.ecf.core.security.IConnectContext;
import org.eclipse.equinox.internal.p2.transport.ecf.httpclient5.Activator;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFileSystemListener;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFileSystemRequest;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.RemoteFileSystemException;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.service.IRemoteFileSystemBrowser;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.service.IRemoteFileSystemBrowserFactory;
import org.eclipse.osgi.util.NLS;

public class HttpClientBrowseFileTransferFactory implements IRemoteFileSystemBrowserFactory {

	@Override
	public IRemoteFileSystemBrowser newInstance() {
		return new IRemoteFileSystemBrowser() {

			private IConnectContext connectContext;

			@Override
			public IRemoteFileSystemRequest sendBrowseRequest(URI directoryOrFileId, IRemoteFileSystemListener listener) throws RemoteFileSystemException {
				Assert.isNotNull(directoryOrFileId);
				Assert.isNotNull(listener);
				URL url;
				try {
					url = directoryOrFileId.toURL();
				} catch (final MalformedURLException | IllegalArgumentException e) {
					throw new RemoteFileSystemException(NLS.bind("Exception creating URL for {0}", directoryOrFileId)); //$NON-NLS-1$
				}

				HttpClientFileSystemBrowser browser = new HttpClientFileSystemBrowser(Activator.getDefault().getBrowseHttpClient(), directoryOrFileId, listener, url, connectContext);
				return browser.sendBrowseRequest();
			}

			@Override
			public void setConnectContextForAuthentication(IConnectContext connectContext) {
				this.connectContext = connectContext;
			}

		};

	}
}
