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
import org.apache.hc.client5.http.config.RequestConfig.Builder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

@SuppressWarnings("restriction")
public class HttpClientModifierAdapter implements IHttpClientModifier {

	@Override
	public HttpClientBuilder modifyClient(HttpClientBuilder builder) {
		return builder;
	}

	@Override
	public CredentialsProvider modifyCredentialsProvider(CredentialsProvider credentialsProvider) {
		return credentialsProvider;
	}

	@Override
	public HttpClientContext modifyContext(HttpClientContext context) {
		return context;
	}

	@Override
	public Builder modifyRequestConfig(Builder config, HttpClientContext context, Map<?, ?> options) {
		return config;
	}

}
