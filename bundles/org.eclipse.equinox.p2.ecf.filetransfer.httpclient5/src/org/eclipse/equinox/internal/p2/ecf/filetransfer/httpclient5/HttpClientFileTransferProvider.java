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
package org.eclipse.equinox.internal.p2.ecf.filetransfer.httpclient5;

import java.net.URL;
import java.util.Set;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.IRemoteFileSystemListener;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.core.security.IConnectContext;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.core.util.Proxy;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.httpclient5.impl.Activator;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.identity.IFileID;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.provider.browse.AbstractFileSystemBrowser;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.provider.browse.AbstractFileSystemBrowserAdapter;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.service.*;
import org.osgi.service.component.annotations.Component;

/**
 * Provides http and https file transfer based on Apache HttpClient 5. It is
 * used instead of the default JRE based provider when the system property
 * <code>org.eclipse.equinox.p2.ecf.filetransfer.httpProvider</code> is set to
 * <code>httpclient5</code>.
 */
@Component(service = IFileTransferProvider.class)
public class HttpClientFileTransferProvider implements IFileTransferProvider {

	public static final String ID = "httpclient5"; //$NON-NLS-1$

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public int getPriority() {
		return 43;
	}

	@Override
	public Set<String> getProtocols() {
		return Set.of("http", "https"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public IRetrieveFileTransfer newRetrieveFileTransfer() {
		return new HttpClientRetrieveFileTransfer(Activator.getDefault().getRetrieveHttpClient());
	}

	@Override
	public IRemoteFileSystemBrowser newRemoteFileSystemBrowser() {
		return new AbstractFileSystemBrowserAdapter() {
			@Override
			protected AbstractFileSystemBrowser createBrowser(IFileID directoryOrFileId, IRemoteFileSystemListener listener, URL url, IConnectContext context, Proxy proxyToUse) {
				return new HttpClientFileSystemBrowser(Activator.getDefault().getBrowseHttpClient(), directoryOrFileId, listener, url, context, proxyToUse);
			}
		};
	}
}
