/****************************************************************************
 * Copyright (c) 2019, 2022 Yatta Solutions and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   Yatta Solutions - initial API and implementation
 *   Christoph Läubrich - adapt to java http client
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.equinox.internal.p2.ecf.filetransfer.httpclientjava.impl;

import java.net.HttpURLConnection;
import java.util.Map;

import org.eclipse.equinox.internal.p2.ecf.filetransfer.core.util.Proxy;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.BrowseFileTransferException;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.IncomingFileTransferException;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.httpclientjava.HttpClientOptions;

public class DefaultNTLMProxyHandler implements INTLMProxyHandler {
	private static boolean seenNTLM;

	public static boolean seenNTLM() {
		return seenNTLM;
	}

	public static void setSeenNTLM() {
		seenNTLM = true;
	}

	public static void resetSeenNTLM() {
		seenNTLM = false;
	}

	@Override
	public boolean allowNTLMAuthentication(Map<?, ?> connectOptions) {
		seenNTLM = true;
		if (connectOptions != null && connectOptions.get(HttpClientOptions.FORCE_NTLM_PROP) != null) {
			return true;
		}
		return (System.getProperty(HttpClientOptions.FORCE_NTLM_PROP) != null);
	}

	@Override
	public void handleNTLMProxy(Proxy proxy, int code) throws IncomingFileTransferException {
		seenNTLM = true;
		if (code >= HttpURLConnection.HTTP_BAD_REQUEST) {
			throw new IncomingFileTransferException("HttpClient Provider is not configured to support NTLM proxy authentication.", //$NON-NLS-1$
					HttpClientOptions.NTLM_PROXY_RESPONSE_CODE);
		}
	}

	@Override
	public void handleSPNEGOProxy(Proxy proxy, int code) throws BrowseFileTransferException {
		if (code >= HttpURLConnection.HTTP_BAD_REQUEST) {
			throw new BrowseFileTransferException("HttpClient Provider does not support the use of SPNEGO proxy authentication."); //$NON-NLS-1$
		}
	}

}
