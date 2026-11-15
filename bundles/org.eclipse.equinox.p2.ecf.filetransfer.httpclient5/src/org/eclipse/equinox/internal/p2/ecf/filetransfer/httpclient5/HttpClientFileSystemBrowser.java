/****************************************************************************
 * Copyright (c) 2019 Composent, Inc., IBM and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *    Henrich Kraemer - bug 263869, testHttpsReceiveFile fails using HTTP proxy
 *    Thomas Joiner - HttpClient 4 implementation
 *    Yatta Solutions - HttpClient 4.5 implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.ecf.provider.filetransfer.httpclient5;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.eclipse.core.runtime.Assert;
import org.eclipse.ecf.core.security.Callback;
import org.eclipse.ecf.core.security.CallbackHandler;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.core.security.NameCallback;
import org.eclipse.ecf.core.security.ObjectCallback;
import org.eclipse.ecf.core.security.UnsupportedCallbackException;
import org.eclipse.ecf.core.util.Proxy;
import org.eclipse.ecf.core.util.ProxyAddress;
import org.eclipse.ecf.core.util.Trace;
import org.eclipse.ecf.filetransfer.BrowseFileTransferException;
import org.eclipse.ecf.filetransfer.IRemoteFile;
import org.eclipse.ecf.filetransfer.IRemoteFileSystemListener;
import org.eclipse.ecf.filetransfer.IRemoteFileSystemRequest;
import org.eclipse.ecf.filetransfer.identity.IFileID;
import org.eclipse.ecf.internal.provider.filetransfer.DebugOptions;
import org.eclipse.ecf.internal.provider.filetransfer.httpclient5.Activator;
import org.eclipse.ecf.internal.provider.filetransfer.httpclient5.DefaultNTLMProxyHandler;
import org.eclipse.ecf.internal.provider.filetransfer.httpclient5.ECFHttpClientFactory;
import org.eclipse.ecf.internal.provider.filetransfer.httpclient5.HttpClientProxyCredentialProvider;
import org.eclipse.ecf.internal.provider.filetransfer.httpclient5.IHttpClientFactory;
import org.eclipse.ecf.internal.provider.filetransfer.httpclient5.INTLMProxyHandler;
import org.eclipse.ecf.internal.provider.filetransfer.httpclient5.Messages;
import org.eclipse.ecf.internal.provider.filetransfer.httpclient5.NTLMProxyDetector;
import org.eclipse.ecf.provider.filetransfer.browse.AbstractFileSystemBrowser;
import org.eclipse.ecf.provider.filetransfer.browse.URLRemoteFile;
import org.eclipse.ecf.provider.filetransfer.util.JREProxyHelper;
import org.eclipse.ecf.provider.filetransfer.util.ProxySetupHelper;
import org.eclipse.osgi.util.NLS;

/**
 *
 */
@SuppressWarnings("restriction")
public class HttpClientFileSystemBrowser extends AbstractFileSystemBrowser {

	private static final String CONTENT_LENGTH_HEADER = "Content-Length"; //$NON-NLS-1$

	protected static final int DEFAULT_CONNECTION_TIMEOUT = HttpClientOptions.BROWSE_DEFAULT_CONNECTION_TIMEOUT;

	private static final String USERNAME_PREFIX = "Username:"; //$NON-NLS-1$

	private JREProxyHelper proxyHelper = null;

	protected String username = null;

	protected String password = null;

	protected CloseableHttpClient httpClient = null;

	private RequestConfig.Builder requestConfigBuilder;

	private HttpClientProxyCredentialProvider credentialsProvider;

	protected volatile HttpHead headMethod;

	/**
	 * This is the response returned by {@link HttpClient} when it executes
	 * {@link #headMethod}.
	 * @since 5.0
	 */
	protected volatile CloseableHttpResponse httpResponse;

	/**
	 * This is the context used to retain information about the request that
	 * the {@link HttpClient} gathers during the request.
	 * @since 5.0
	 */
	protected volatile HttpClientContext httpContext;

	/**
	 * @param httpClient http client
	 * @param directoryOrFileID directory or file id
	 * @param listener listener
	 * @param directoryOrFileURL directory or file id
	 * @param connectContext connect context
	 * @param proxy proxy
	 * @since 5.0
	 */
	public HttpClientFileSystemBrowser(CloseableHttpClient httpClient, IFileID directoryOrFileID, IRemoteFileSystemListener listener, URL directoryOrFileURL, IConnectContext connectContext, Proxy proxy) {
		super(directoryOrFileID, listener, directoryOrFileURL, connectContext, proxy);
		Assert.isNotNull(httpClient);
		this.httpClient = httpClient;

		credentialsProvider = new HttpClientProxyCredentialProvider() {

			@Override
			protected Proxy getECFProxy() {
				return getProxy();
			}

			@Override
			protected boolean allowNTLMAuthentication() {
				DefaultNTLMProxyHandler.setSeenNTLM();
				return ECFHttpClientFactory.getNTLMProxyHandler(httpContext).allowNTLMAuthentication(null);
			}

		};
		IHttpClientFactory httpClientFactory = Activator.getDefault().getHttpClientFactory();
		CredentialsProvider contextCredentialsProvider = ECFHttpClientFactory.modifyCredentialsProvider(credentialsProvider);
		httpContext = httpClientFactory.newClientContext();
		httpContext.setCredentialsProvider(contextCredentialsProvider);
		this.proxyHelper = new JREProxyHelper();

	}

	class HttpClientRemoteFileSystemRequest extends RemoteFileSystemRequest {
		HttpClientRemoteFileSystemRequest() {
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter == null) {
				return null;
			}
			if (adapter.isInstance(this)) {
				return adapter.cast(this);
			}
			return null;
		}

		@Override
		public void cancel() {
			HttpClientFileSystemBrowser.this.cancel();
		}
	}

	@Override
	protected IRemoteFileSystemRequest createRemoteFileSystemRequest() {
		return new HttpClientRemoteFileSystemRequest();
	}

	@Override
	protected void cancel() {
		if (isCanceled()) {
			return; // break job cancel recursion
		}
		setCanceled(getException());
		super.cancel();
		if (headMethod != null) {
			if (!headMethod.isAborted()) {
				headMethod.abort();
			}
		}
	}

	@Override
	protected void setupProxies() {
		// If it's been set directly (via ECF API) then this overrides platform settings
		if (proxy == null) {
			try {
				// give SOCKS priority see https://bugs.eclipse.org/bugs/show_bug.cgi?id=295030#c61
				proxy = ProxySetupHelper.getSocksProxy(directoryOrFile);
				if (proxy == null) {
					proxy = ProxySetupHelper.getProxy(directoryOrFile.toExternalForm());
				}
			} catch (NoClassDefFoundError e) {
				// If the proxy API is not available a NoClassDefFoundError will be thrown here.
				// If that happens then we just want to continue on.
				Activator.logNoProxyWarning(e);

			}
		}
		if (proxy != null)
			setupProxy(proxy);
	}

	@Override
	protected void cleanUp() {
		clearProxy();
		headMethod = null;
		requestConfigBuilder = null;

		super.cleanUp();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.provider.filetransfer.browse.AbstractFileSystemBrowser#runRequest()
	 */
	@Override
	protected void runRequest() throws Exception {
		Trace.entering(Activator.PLUGIN_ID, DebugOptions.METHODS_ENTERING, this.getClass(), "runRequest"); //$NON-NLS-1$

		String urlString = directoryOrFile.toString();

		requestConfigBuilder = Activator.getDefault().getHttpClientFactory().newRequestConfig(httpContext, null);
		requestConfigBuilder.setConnectionRequestTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);

		setupProxies();

		// setup authentication
		setupAuthentication(urlString);

		headMethod = new HttpHead(urlString);
		headMethod.setConfig(requestConfigBuilder.build());

		int maxAge = Integer.getInteger("org.eclipse.ecf.http.cache.max-age", 0).intValue(); //$NON-NLS-1$
		// set max-age for cache control to 0 for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=249990
		// fix the fix for bug 249990 with bug 410813
		if (maxAge == 0) {
			headMethod.addHeader(HttpHeaders.CACHE_CONTROL, "max-age=0"); //$NON-NLS-1$
		} else if (maxAge > 0) {
			headMethod.addHeader(HttpHeaders.CACHE_CONTROL, "max-age=" + maxAge); //$NON-NLS-1$
		}

		long lastModified = 0;
		long fileLength = -1;

		int code = -1;
		try {
			Trace.trace(Activator.PLUGIN_ID, "browse=" + urlString); //$NON-NLS-1$

			httpResponse = httpClient.execute(headMethod, httpContext);
			code = httpResponse.getCode();

			Trace.trace(Activator.PLUGIN_ID, "browse resp=" + code); //$NON-NLS-1$

			// Check for NTLM proxy in response headers
			// This check is to deal with bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=252002
			boolean ntlmProxyFound = NTLMProxyDetector.detectNTLMProxy(httpContext);
			if (ntlmProxyFound)
				getNTLMProxyHandler(httpContext).handleNTLMProxy(getProxy(), code);

			if (NTLMProxyDetector.detectSPNEGOProxy(httpContext))
				getNTLMProxyHandler(httpContext).handleSPNEGOProxy(getProxy(), code);

			if (code == HttpURLConnection.HTTP_OK) {
				Header contentLength = httpResponse.getLastHeader(CONTENT_LENGTH_HEADER);
				if (contentLength != null) {
					fileLength = Integer.parseInt(contentLength.getValue());
				}

				lastModified = getLastModifiedTimeFromHeader();
			} else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
				throw new BrowseFileTransferException(NLS.bind("File not found: {0}", urlString), code); //$NON-NLS-1$
			} else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new BrowseFileTransferException(Messages.HttpClientRetrieveFileTransfer_Unauthorized, code);
			} else if (code == HttpURLConnection.HTTP_FORBIDDEN) {
				throw new BrowseFileTransferException("Forbidden", code); //$NON-NLS-1$
			} else if (code == HttpURLConnection.HTTP_PROXY_AUTH) {
				throw new BrowseFileTransferException(Messages.HttpClientRetrieveFileTransfer_Proxy_Auth_Required, code);
			} else {
				throw new BrowseFileTransferException(NLS.bind(Messages.HttpClientRetrieveFileTransfer_ERROR_GENERAL_RESPONSE_CODE, Integer.valueOf(code)), code);
			}
			remoteFiles = new IRemoteFile[1];
			remoteFiles[0] = new URLRemoteFile(lastModified, fileLength, fileID);
		} catch (Exception e) {
			Trace.throwing(Activator.PLUGIN_ID, DebugOptions.EXCEPTIONS_THROWING, this.getClass(), "runRequest", e); //$NON-NLS-1$
			BrowseFileTransferException ex = (BrowseFileTransferException) ((e instanceof BrowseFileTransferException) ? e : new BrowseFileTransferException(NLS.bind(Messages.HttpClientRetrieveFileTransfer_EXCEPTION_COULD_NOT_CONNECT, urlString), e, code));
			throw ex;
		} finally {
			if (httpResponse != null) {
				httpResponse.close();
			}
		}
	}

	private INTLMProxyHandler getNTLMProxyHandler(HttpContext httpContext) {
		Object value = httpContext.getAttribute(ECFHttpClientFactory.NTLM_PROXY_HANDLER_ATTR);
		if (value instanceof INTLMProxyHandler) {
			return (INTLMProxyHandler) value;
		}
		return Activator.getDefault().getNTLMProxyHandler();
	}

	private long getLastModifiedTimeFromHeader() throws IOException {
		Header lastModifiedHeader = httpResponse.getLastHeader(HttpHeaders.LAST_MODIFIED);
		if (lastModifiedHeader == null)
			return 0L;
		String lastModifiedString = lastModifiedHeader.getValue();
		long lastModified = 0;
		if (lastModifiedString != null) {
			try {
				lastModified = DateUtils.parseDate(lastModifiedString).getTime();
			} catch (Exception e) {
				throw new IOException(Messages.HttpClientRetrieveFileTransfer_EXCEPITION_INVALID_LAST_MODIFIED_FROM_SERVER);
			}
		}
		return lastModified;
	}

	Proxy getProxy() {
		return proxy;
	}

	/**
	 * Retrieves the credentials for requesting the file.
	 * @return the {@link Credentials} necessary to retrieve the file
	 * @throws UnsupportedCallbackException if the callback fails
	 * @throws IOException if IO fails
	 * @since 5.0
	 */
	protected Credentials getFileRequestCredentials() throws UnsupportedCallbackException, IOException {
		if (connectContext == null)
			return null;
		final CallbackHandler callbackHandler = connectContext.getCallbackHandler();
		if (callbackHandler == null)
			return null;
		final NameCallback usernameCallback = new NameCallback(USERNAME_PREFIX);
		final ObjectCallback passwordCallback = new ObjectCallback();
		callbackHandler.handle(new Callback[] {usernameCallback, passwordCallback});
		username = usernameCallback.getName();
		password = (String) passwordCallback.getObject();
		return new UsernamePasswordCredentials(username, password==null?null:password.toCharArray());
	}

	protected void setupAuthentication(String urlString) throws UnsupportedCallbackException, IOException {
		Credentials credentials = null;
		if (username == null) {
			credentials = getFileRequestCredentials();
		}

		if (credentials != null && username != null) {
			final AuthScope authScope = new AuthScope(HttpClientRetrieveFileTransfer.getHostFromURL(urlString), HttpClientRetrieveFileTransfer.getPortFromURL(urlString));
			Trace.trace(Activator.PLUGIN_ID, "browse credentials=" + credentials); //$NON-NLS-1$
			credentialsProvider.setCredentials(authScope, credentials);
		}
	}

	@Override
	protected void setupProxy(Proxy proxy) {
		if (proxy.getType().equals(Proxy.Type.HTTP)) {
			final ProxyAddress address = proxy.getAddress();
			requestConfigBuilder.setProxy(new HttpHost(address.getHostName(), address.getPort()));
		} else if (proxy.getType().equals(Proxy.Type.SOCKS)) {
			Trace.trace(Activator.PLUGIN_ID, "browse socksproxy=" + proxy.getAddress()); //$NON-NLS-1$
			requestConfigBuilder.setProxy(null);
			proxyHelper.setupProxy(proxy);
		}
	}

	/**
	 * This method will clear out the proxy information (so that if this is
	 * reused for a request without a proxy, it will work correctly).
	 * @since 5.0
	 */
	protected void clearProxy() {
		if (requestConfigBuilder != null) {
			requestConfigBuilder.setProxy(null);
		}
	}

}
