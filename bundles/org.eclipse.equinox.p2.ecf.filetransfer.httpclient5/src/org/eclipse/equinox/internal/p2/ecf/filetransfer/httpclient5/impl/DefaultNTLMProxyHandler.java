/****************************************************************************
 * Copyright (c) 2019 Yatta Solutions and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   Yatta Solutions - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.internal.provider.filetransfer.httpclient5;

import java.util.Map;
import org.apache.hc.core5.http.HttpStatus;
import org.eclipse.ecf.core.util.Proxy;
import org.eclipse.ecf.filetransfer.BrowseFileTransferException;
import org.eclipse.ecf.filetransfer.IncomingFileTransferException;
import org.eclipse.ecf.provider.filetransfer.httpclient5.HttpClientOptions;

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
		if (code >= HttpStatus.SC_BAD_REQUEST) {
			throw new IncomingFileTransferException("HttpClient Provider is not configured to support NTLM proxy authentication.", //$NON-NLS-1$
					HttpClientOptions.NTLM_PROXY_RESPONSE_CODE);
		}
	}

	@Override
	public void handleSPNEGOProxy(Proxy proxy, int code) throws BrowseFileTransferException {
		if (code >= HttpStatus.SC_BAD_REQUEST) {
			throw new BrowseFileTransferException("HttpClient Provider does not support the use of SPNEGO proxy authentication."); //$NON-NLS-1$
		}
	}

}
