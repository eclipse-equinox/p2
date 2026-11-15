/****************************************************************************
 * Copyright (c) 2019, 2022 Composent, Inc., IBM and others.
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
 *    Christoph Läubrich - Java HTTP client implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.ecf.provider.filetransfer.httpclientjava;

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
import org.eclipse.ecf.internal.provider.filetransfer.httpclientjava.Activator;
import org.eclipse.ecf.internal.provider.filetransfer.httpclientjava.AuthScope;
import org.eclipse.ecf.internal.provider.filetransfer.httpclientjava.Credentials;
import org.eclipse.ecf.internal.provider.filetransfer.httpclientjava.DefaultNTLMProxyHandler;
import org.eclipse.ecf.internal.provider.filetransfer.httpclientjava.ECFHttpClientFactory;
import org.eclipse.ecf.internal.provider.filetransfer.httpclientjava.HttpClientProxyCredentialProvider;
import org.eclipse.ecf.internal.provider.filetransfer.httpclientjava.HttpHost;
import org.eclipse.ecf.internal.provider.filetransfer.httpclientjava.IHttpClientContext;
import org.eclipse.ecf.internal.provider.filetransfer.httpclientjava.IHttpClientFactory;
import org.eclipse.ecf.internal.provider.filetransfer.httpclientjava.INTLMProxyHandler;
import org.eclipse.ecf.internal.provider.filetransfer.httpclientjava.Messages;
import org.eclipse.ecf.internal.provider.filetransfer.httpclientjava.NTLMProxyDetector;
import org.eclipse.ecf.internal.provider.filetransfer.httpclientjava.UsernamePasswordCredentials;
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

	// see https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3
	// per RFC there are three different formats:
	private static final List<ThreadLocal<DateFormat>> DATE_PATTERNS = List.of(//
			ThreadLocal.withInitial(() -> new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)), // RFC 1123
			ThreadLocal.withInitial(() -> new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss zzz", Locale.ENGLISH)), // RFC 1036
			ThreadLocal.withInitial(() -> new SimpleDateFormat("EEE MMMd HH:mm:ss yyyy", Locale.ENGLISH)) // ANSI C's asctime() format
	);

	public static final String LAST_MODIFIED_HEADER = "Last-Modified"; //$NON-NLS-1$

	public static final String CACHE_CONTROL_HEADER = "Cache-Control"; //$NON-NLS-1$

	public static final String CONTENT_LENGTH_HEADER = "Content-Length"; //$NON-NLS-1$

	protected static final int DEFAULT_CONNECTION_TIMEOUT = HttpClientOptions.BROWSE_DEFAULT_CONNECTION_TIMEOUT;

	private static final String USERNAME_PREFIX = "Username:"; //$NON-NLS-1$

	private JREProxyHelper proxyHelper = null;

	protected String username = null;

	protected String password = null;

	protected HttpClient httpClient = null;

	private HttpClientProxyCredentialProvider credentialsProvider;

	/**
	 * This is the context used to retain information about the request that the
	 * {@link HttpClient} gathers during the request.
	 * 
	 * @since 5.0
	 */
	protected volatile IHttpClientContext httpContext;

	private volatile CompletableFuture<HttpResponse<Void>> asyncRequest;

	/**
	 * @param httpClient         http client
	 * @param directoryOrFileID  directory or file id
	 * @param listener           listener
	 * @param directoryOrFileURL directory or file id
	 * @param connectContext     connect context
	 * @param proxy              proxy
	 * @since 5.0
	 */
	public HttpClientFileSystemBrowser(HttpClient httpClient, IFileID directoryOrFileID,
			IRemoteFileSystemListener listener, URL directoryOrFileURL, IConnectContext connectContext, Proxy proxy) {
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
		Authenticator contextCredentialsProvider = ECFHttpClientFactory.modifyCredentialsProvider(credentialsProvider);
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
		if (asyncRequest != null) {
			asyncRequest.cancel(true);
		}
	}

	@Override
	protected void setupProxies() {
		// If it's been set directly (via ECF API) then this overrides platform settings
		if (proxy == null) {
			try {
				// give SOCKS priority see
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=295030#c61
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
		asyncRequest = null;
		super.cleanUp();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.provider.filetransfer.browse.AbstractFileSystemBrowser#
	 * runRequest()
	 */
	@Override
	protected void runRequest() throws Exception {
		Trace.entering(Activator.PLUGIN_ID, DebugOptions.METHODS_ENTERING, this.getClass(), "runRequest"); //$NON-NLS-1$

		String urlString = directoryOrFile.toString();

		HttpRequest.Builder requestConfigBuilder = Activator.getDefault().getHttpClientFactory()
				.newRequestConfig(httpContext, null);
		requestConfigBuilder.timeout(Duration.ofMillis(DEFAULT_CONNECTION_TIMEOUT));
		requestConfigBuilder.uri(new URI(urlString));
		requestConfigBuilder.method("HEAD", BodyPublishers.noBody());
		setupProxies();

		// setup authentication
		setupAuthentication(urlString);

		int maxAge = Integer.getInteger("org.eclipse.ecf.http.cache.max-age", 0).intValue(); //$NON-NLS-1$
		// set max-age for cache control to 0 for bug
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=249990
		// fix the fix for bug 249990 with bug 410813
		requestConfigBuilder.header(CACHE_CONTROL_HEADER, "max-age=" + maxAge);
		
		// Add Basic Authentication to request config builder
		setupBasicAuthentication(requestConfigBuilder);

		HttpRequest request = requestConfigBuilder.build();

		long lastModified = 0;
		OptionalLong fileLength = OptionalLong.empty();

		int code = -1;
		try {
			Trace.trace(Activator.PLUGIN_ID, "browse=" + urlString); //$NON-NLS-1$

			asyncRequest = httpClient.sendAsync(request, BodyHandlers.discarding());
			HttpResponse<Void> response = asyncRequest.join();
			code = response.statusCode();

			Trace.trace(Activator.PLUGIN_ID, "browse resp=" + code); //$NON-NLS-1$

			// Check for NTLM proxy in response headers
			// This check is to deal with bug
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=252002
			boolean ntlmProxyFound = NTLMProxyDetector.detectNTLMProxy(httpContext);
			if (ntlmProxyFound)
				getNTLMProxyHandler(httpContext).handleNTLMProxy(getProxy(), code);

			if (NTLMProxyDetector.detectSPNEGOProxy(httpContext))
				getNTLMProxyHandler(httpContext).handleSPNEGOProxy(getProxy(), code);

			if (code == HttpURLConnection.HTTP_OK) {
				fileLength = response.headers().firstValueAsLong(CONTENT_LENGTH_HEADER);
				lastModified = getLastModifiedTimeFromHeader(response.headers());
			} else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
				throw new BrowseFileTransferException(NLS.bind("File not found: {0}", urlString), code); //$NON-NLS-1$
			} else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new BrowseFileTransferException(Messages.HttpClientRetrieveFileTransfer_Unauthorized, code);
			} else if (code == HttpURLConnection.HTTP_FORBIDDEN) {
				throw new BrowseFileTransferException("Forbidden", code); //$NON-NLS-1$
			} else if (code == HttpURLConnection.HTTP_PROXY_AUTH) {
				throw new BrowseFileTransferException(Messages.HttpClientRetrieveFileTransfer_Proxy_Auth_Required,
						code);
			} else {
				throw new BrowseFileTransferException(
						NLS.bind(Messages.HttpClientRetrieveFileTransfer_ERROR_GENERAL_RESPONSE_CODE,
								Integer.valueOf(code)),
						code);
			}
			remoteFiles = new IRemoteFile[1];
			remoteFiles[0] = new URLRemoteFile(lastModified, fileLength.orElse(-1), fileID);
		} catch (Exception e) {
			Throwable t = e;
			if (e instanceof CompletionException) {
				t = e.getCause();
			}
			Trace.throwing(Activator.PLUGIN_ID, DebugOptions.EXCEPTIONS_THROWING, this.getClass(), "runRequest", t); //$NON-NLS-1$
			BrowseFileTransferException ex = (BrowseFileTransferException) ((t instanceof BrowseFileTransferException)
					? t
					: new BrowseFileTransferException(
							NLS.bind(Messages.HttpClientRetrieveFileTransfer_EXCEPTION_COULD_NOT_CONNECT, urlString), t,
							code));
			throw ex;
		}
	}

	private void setupBasicAuthentication(Builder requestConfigBuilder) {
		if (username != null) {
			byte[] credentials = Base64.getEncoder().encode((username + ":" + password).getBytes(StandardCharsets.UTF_8));
			requestConfigBuilder.header("Authorization", "Basic " + new String(credentials, StandardCharsets.UTF_8));
		}
	}

	private INTLMProxyHandler getNTLMProxyHandler(IHttpClientContext httpContext) {
		Object value = httpContext.getAttribute(ECFHttpClientFactory.NTLM_PROXY_HANDLER_ATTR);
		if (value instanceof INTLMProxyHandler) {
			return (INTLMProxyHandler) value;
		}
		return Activator.getDefault().getNTLMProxyHandler();
	}

	public static long getLastModifiedTimeFromHeader(java.net.http.HttpHeaders httpHeaders) throws IOException {
		String lastModifiedHeader = httpHeaders.firstValue(LAST_MODIFIED_HEADER).orElse(null);
		if (lastModifiedHeader == null)
			return 0L;
		// first check if there are any quotes around and remove them
		if (lastModifiedHeader.length() > 1 && lastModifiedHeader.startsWith("'") && lastModifiedHeader.endsWith("'")) {
			lastModifiedHeader = lastModifiedHeader.substring(1, lastModifiedHeader.length() - 1);
		}
		// no check all date formats
		for (final ThreadLocal<DateFormat> dateFormat : DATE_PATTERNS) {
			try {
				return dateFormat.get().parse(lastModifiedHeader).getTime();
			} catch (ParseException e) {
				// try next one...
			}
		}
		// nothing found or exception...
		throw new IOException(Messages.HttpClientRetrieveFileTransfer_EXCEPITION_INVALID_LAST_MODIFIED_FROM_SERVER);
	}

	Proxy getProxy() {
		return proxy;
	}

	/**
	 * Retrieves the credentials for requesting the file.
	 * 
	 * @return the {@link Credentials} necessary to retrieve the file
	 * @throws UnsupportedCallbackException if the callback fails
	 * @throws IOException                  if IO fails
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
		callbackHandler.handle(new Callback[] { usernameCallback, passwordCallback });
		username = usernameCallback.getName();
		password = (String) passwordCallback.getObject();
		return new UsernamePasswordCredentials(username, password == null ? null : password.toCharArray());
	}

	protected void setupAuthentication(String urlString) throws UnsupportedCallbackException, IOException {
		Credentials credentials = null;
		if (username == null) {
			credentials = getFileRequestCredentials();
		}

		if (credentials != null && username != null) {
			final AuthScope authScope = new AuthScope(HttpClientRetrieveFileTransfer.getHostFromURL(urlString),
					HttpClientRetrieveFileTransfer.getPortFromURL(urlString));
			Trace.trace(Activator.PLUGIN_ID, "browse credentials=" + credentials); //$NON-NLS-1$
			credentialsProvider.setCredentials(authScope, credentials);
		}
	}

	@Override
	protected void setupProxy(Proxy proxy) {
		if (proxy.getType().equals(Proxy.Type.HTTP)) {
			final ProxyAddress address = proxy.getAddress();
			httpContext.setProxy(new HttpHost(address.getHostName(), address.getPort()));
		} else if (proxy.getType().equals(Proxy.Type.SOCKS)) {
			Trace.trace(Activator.PLUGIN_ID, "browse socksproxy=" + proxy.getAddress()); //$NON-NLS-1$
			httpContext.setProxy(null);
			proxyHelper.setupProxy(proxy);
		}
	}

	/**
	 * This method will clear out the proxy information (so that if this is reused
	 * for a request without a proxy, it will work correctly).
	 * 
	 * @since 5.0
	 */
	protected void clearProxy() {
		httpContext.setProxy(null);
	}

}
