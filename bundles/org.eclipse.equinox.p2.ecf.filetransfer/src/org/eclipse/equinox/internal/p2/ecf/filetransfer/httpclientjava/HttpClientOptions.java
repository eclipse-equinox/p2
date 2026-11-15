/****************************************************************************
 * Copyright (c) 2019, 2020 Yatta Solutions and others.
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
package org.eclipse.ecf.provider.filetransfer.httpclientjava;

/**
 * @see org.eclipse.ecf.provider.filetransfer.httpclient4.HttpClientOptions
 */
public interface HttpClientOptions {
	// Use configuration properties backward compatible to HttpClient4 provider
	String BROWSE_CONNECTION_TIMEOUT_PROP = "org.eclipse.ecf.provider.filetransfer.httpclient4.browse.connectTimeout"; //$NON-NLS-1$
	String RETRIEVE_READ_TIMEOUT_PROP = "org.eclipse.ecf.provider.filetransfer.httpclient4.retrieve.readTimeout"; //$NON-NLS-1$
	String RETRIEVE_CONNECTION_TIMEOUT_PROP = "org.eclipse.ecf.provider.filetransfer.httpclient4.retrieve.connectTimeout"; //$NON-NLS-1$

	// This configuration option is only available for the HttpClient45/httpclient5 provider because only newer Apache HttpComponents
	// version supports setting the TTL
	String RETRIEVE_CONNECTION_TTL = "org.eclipse.ecf.provider.filetransfer.httpclient5.retrieve.connectionTTL"; //$NON-NLS-1$

	int RETRIEVE_DEFAULT_CONNECTION_TIMEOUT = Integer.parseInt(System.getProperty(RETRIEVE_CONNECTION_TIMEOUT_PROP, "120000")); //$NON-NLS-1$
	int RETRIEVE_DEFAULT_CONNECTION_TTL = Integer.parseInt(System.getProperty(RETRIEVE_CONNECTION_TTL, "1000")); //$NON-NLS-1$
	int RETRIEVE_DEFAULT_READ_TIMEOUT = Integer.parseInt(System.getProperty(RETRIEVE_READ_TIMEOUT_PROP, "120000")); //$NON-NLS-1$
	int BROWSE_DEFAULT_CONNECTION_TIMEOUT = Integer.parseInt(System.getProperty(BROWSE_CONNECTION_TIMEOUT_PROP, "120000")); //$NON-NLS-1$

	int NTLM_PROXY_RESPONSE_CODE = 477;
	String FORCE_NTLM_PROP = "org.eclipse.ecf.provider.filetransfer.httpclient4.options.ForceNTLMProxy"; //$NON-NLS-1$

	/**
	 * @since 2.1
	 */
	String HTTPCLIENT_SSLCONTEXT_PROTOCOL = System.getProperty("org.eclipse.ecf.provider.filetransfer.httpclient.sslcontext.protocol"); //$NON-NLS-1$

	/**
	 * @since 2.1
	 */
	String HTTPCLIENT_SSLCONTEXT_PROVIDER = System.getProperty("org.eclipse.ecf.provider.filetransfer.httpclient.sslcontext.provider"); //$NON-NLS-1$
}
