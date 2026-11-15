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
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

@SuppressWarnings("restriction")
public interface IHttpClientModifier {
	HttpClientBuilder modifyClient(HttpClientBuilder builder);

	CredentialsProvider modifyCredentialsProvider(CredentialsProvider credentialsProvider);

	HttpClientContext modifyContext(HttpClientContext context);

	RequestConfig.Builder modifyRequestConfig(RequestConfig.Builder config, HttpClientContext context, Map<?, ?> options);
}
