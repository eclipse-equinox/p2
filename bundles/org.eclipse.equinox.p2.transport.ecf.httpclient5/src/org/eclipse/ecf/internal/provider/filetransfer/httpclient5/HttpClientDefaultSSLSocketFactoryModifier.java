/****************************************************************************
 * Copyright (c) 2019 Composent, Inc., IBM and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *  Composent, Inc. - initial API and implementation
 *  Henrich Kraemer - bug 263869, testHttpsReceiveFile fails using HTTP proxy
 *  Yatta Solutions - HttpClient 4.5 implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.ecf.internal.provider.filetransfer.httpclient5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class HttpClientDefaultSSLSocketFactoryModifier {
	public static final String DEFAULT_SSL_PROTOCOL = "https.protocols"; //$NON-NLS-1$

	private SSLContext sslContext = null;

	private String defaultProtocolNames = System.getProperty(DEFAULT_SSL_PROTOCOL);

	private static final String[] jreProtocols = new String[] {"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	public HttpClientDefaultSSLSocketFactoryModifier() {
		// empty
	}

	public synchronized SSLSocketFactory getSSLSocketFactory() throws IOException {
		if (null == sslContext) {
			try {
				sslContext = getSSLContext(defaultProtocolNames);
			} catch (Exception e) {
				IOException ioe = new IOException();
				ioe.initCause(e);
				throw ioe;
			}
		}
		return (sslContext == null) ? (SSLSocketFactory) SSLSocketFactory.getDefault() : sslContext.getSocketFactory();
	}

	public synchronized SSLContext getSSLContext(String protocols) {
		SSLContext resultContext = null;
		if (protocols != null) {

			String[] httpsProtocols = protocols.split(","); //$NON-NLS-1$
			// trim to make sure
			for (int i = 0; i < httpsProtocols.length; i++)
				httpsProtocols[i] = httpsProtocols[i].trim();
			// Now put into defaultProtocolsList in order of jreProtocols
			List<String> splitProtocolsList = Arrays.asList(httpsProtocols);
			List<String> defaultProtocolsList = new ArrayList<String>();
			for (String jreProtocol : jreProtocols) {
				if (splitProtocolsList.contains(jreProtocol)) {
					defaultProtocolsList.add(jreProtocol);
				}
			}
			// In order of jre protocols, attempt to create and init SSLContext
			for (String protocol : defaultProtocolsList) {
				try {
					resultContext = SSLContext.getInstance(protocol);
					resultContext.init(null, new TrustManager[] {new HttpClientSslTrustManager()}, null);
					break;
				} catch (Exception e) {
					// just continue to look for SSLContexts with the next
					// protocolName
				}
			}
		}
		return resultContext;
	}
}
