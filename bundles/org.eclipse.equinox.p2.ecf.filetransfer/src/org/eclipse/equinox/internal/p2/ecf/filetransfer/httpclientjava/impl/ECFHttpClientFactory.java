/****************************************************************************
 * Copyright (c) 2019, 2022 Yatta Solutions and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   Yatta Solutions - initial API and implementation
 *   Christoph Läubrich - adapt for java http client
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.equinox.internal.p2.ecf.filetransfer.httpclientjava.impl;

import java.net.Authenticator;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.core.security.SSLContextFactory;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.core.util.Trace;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.internal.DebugOptions;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.httpclientjava.HttpClientOptions;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.httpclientjava.HttpClientRetrieveFileTransfer;
import org.osgi.service.component.annotations.Component;

@SuppressWarnings({ "restriction" })
@Component
public class ECFHttpClientFactory implements IHttpClientFactory {
	public static final int DEFAULT_CONNECTION_TIMEOUT = HttpClientOptions.RETRIEVE_DEFAULT_CONNECTION_TIMEOUT;
	public static final int DEFAULT_CONNECTION_TTL = HttpClientOptions.RETRIEVE_DEFAULT_CONNECTION_TTL;
	public static final int DEFAULT_READ_TIMEOUT = HttpClientOptions.RETRIEVE_DEFAULT_READ_TIMEOUT;

	public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = HttpClientOptions.RETRIEVE_DEFAULT_CONNECTION_TIMEOUT;

	public static final String NTLM_PROXY_HANDLER_ATTR = INTLMProxyHandler.class.getName();

	@Override
	public HttpClient.Builder newClient() {
		HttpClient.Builder builder = HttpClient.newBuilder().followRedirects(Redirect.NORMAL);
		String sslContextProvider = HttpClientOptions.HTTPCLIENT_SSLCONTEXT_PROVIDER;
		String sslContextProtocol = HttpClientOptions.HTTPCLIENT_SSLCONTEXT_PROTOCOL;
		SSLContextFactory sslContextFactory = HttpClientJavaPlugin.getDefault().getSSLContextFactory();
		try {
			if (sslContextProvider == null) {
				if (sslContextProtocol == null) {
					builder.sslContext(sslContextFactory.getDefault());
				} else {
					builder.sslContext(sslContextFactory.getInstance(sslContextProtocol));
				}
			} else {
				if (sslContextProtocol == null)
					throw new NoSuchProviderException("Null protocol not supported for provider=" + sslContextProvider);
				builder.sslContext(sslContextFactory.getInstance(sslContextProtocol, sslContextProvider));
			}
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			HttpClientJavaPlugin.getDefault().log(new Status(IStatus.ERROR, HttpClientJavaPlugin.PLUGIN_ID,
					"Could not set SSLContext when creating jre HttpClient", e));
		}
		builder = HttpClientJavaPlugin.getDefault().runModifiers(builder, new ModifierRunner<HttpClient.Builder>() {
			@Override
			public HttpClient.Builder run(IHttpClientModifier modifier, HttpClient.Builder value) {
				return modifier.modifyClient(value);
			}

		});

		return builder;
	}

	@Override
	public IHttpClientContext newClientContext() {
		IHttpClientContext context = new IHttpClientContext() {

			// The java.net.http client takes the proxy from the default ProxySelector and
			// the credentials are sent preemptively, so these are only kept as attributes
			private Map<String, Object> values = new HashMap<>();

			@Override
			public void setAttribute(String key, Object value) {
				values.put(key, value);
			}

			@Override
			public void setCredentialsProvider(Authenticator contextCredentialsProvider) {
				values.put(Authenticator.class.getName(), contextCredentialsProvider);
			}

			@Override
			public Object getAttribute(String key) {
				return values.get(key);
			}

			@Override
			public void setProxy(HttpHost httpHost) {
				values.put(HttpHost.class.getName(), httpHost);
			}
		};
		INTLMProxyHandler ntlmProxyHandler = HttpClientJavaPlugin.getDefault().getNTLMProxyHandler();
		context.setAttribute(NTLM_PROXY_HANDLER_ATTR, ntlmProxyHandler);

		context = HttpClientJavaPlugin.getDefault().runModifiers(context, new ModifierRunner<IHttpClientContext>() {

			@Override
			public IHttpClientContext run(IHttpClientModifier modifier, IHttpClientContext value) {
				return modifier.modifyContext(value);
			}

		});

		return context;
	}

	@Override
	public HttpRequest.Builder newRequestConfig(final IHttpClientContext context, final Map<?, ?> localOptions) {
		HttpRequest.Builder builder = HttpRequest.newBuilder();
		int connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;
		int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
		int readTimeout = DEFAULT_READ_TIMEOUT;
		if (localOptions != null) {
			connectionRequestTimeout = getIntOption(HttpClientOptions.RETRIEVE_CONNECTION_TIMEOUT_PROP, localOptions,
					connectionRequestTimeout);
			connectionTimeout = getIntOption(HttpClientOptions.RETRIEVE_CONNECTION_TIMEOUT_PROP, localOptions,
					connectionTimeout);
			readTimeout = getIntOption(HttpClientOptions.RETRIEVE_READ_TIMEOUT_PROP, localOptions, readTimeout);
		}
		builder.timeout(Duration.ofMillis(connectionRequestTimeout));

		builder = HttpClientJavaPlugin.getDefault().runModifiers(builder, new ModifierRunner<HttpRequest.Builder>() {

			@Override
			public HttpRequest.Builder run(IHttpClientModifier modifier, HttpRequest.Builder value) {
				return modifier.modifyRequestConfig(value, context, localOptions);
			}

		});
		return builder;
	}

	private int getIntOption(String key, final Map<?, ?> localOptions, int defaultValue) {
		Object option = localOptions.get(key);
		if (option != null) {
			int value = defaultValue;
			if (option instanceof Number) {
				value = ((Number) option).intValue();
			} else if (option instanceof String) {
				try {
					value = Integer.parseInt((String) option);
				} catch (NumberFormatException e) {
					Trace.catching(HttpClientJavaPlugin.PLUGIN_ID, DebugOptions.EXCEPTIONS_CATCHING, HttpClientRetrieveFileTransfer.class, "getDnsHostName", e); //$NON-NLS-1$
				}
			}
			return value;
		}
		return defaultValue;
	}

	public static Authenticator modifyCredentialsProvider(Authenticator credentialsProvider) {
		return HttpClientJavaPlugin.getDefault().runModifiers(credentialsProvider, new ModifierRunner<Authenticator>() {

			@Override
			public Authenticator run(IHttpClientModifier modifier, Authenticator value) {
				return modifier.modifyCredentialsProvider(value);
			}

		});
	}

	public static INTLMProxyHandler getNTLMProxyHandler(IHttpClientContext httpContext) {
		Object value = httpContext == null ? null : httpContext.getAttribute(ECFHttpClientFactory.NTLM_PROXY_HANDLER_ATTR);
		if (value instanceof INTLMProxyHandler) {
			return (INTLMProxyHandler) value;
		}
		INTLMProxyHandler handler = HttpClientJavaPlugin.getDefault().getNTLMProxyHandler();
		if (handler != null && httpContext != null && value == null) {
			httpContext.setAttribute(ECFHttpClientFactory.NTLM_PROXY_HANDLER_ATTR, handler);
		}
		return handler;
	}

	protected static interface ModifierRunner<T> {
		T run(IHttpClientModifier modifier, T value);
	}
}
