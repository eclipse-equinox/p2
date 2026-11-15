/****************************************************************************
 * Copyright (c) 2004 Composent, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 * 				 Maarten Meijer - bug 237936, added gzip encoded transfer default
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.provider.filetransfer.retrieve;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ecf.core.security.Callback;
import org.eclipse.ecf.core.security.CallbackHandler;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.core.security.NameCallback;
import org.eclipse.ecf.core.security.ObjectCallback;
import org.eclipse.ecf.core.security.UnsupportedCallbackException;
import org.eclipse.ecf.core.util.Proxy;
import org.eclipse.ecf.filetransfer.IFileRangeSpecification;
import org.eclipse.ecf.filetransfer.IFileTransferPausable;
import org.eclipse.ecf.filetransfer.IRetrieveFileTransferOptions;
import org.eclipse.ecf.filetransfer.IncomingFileTransferException;
import org.eclipse.ecf.filetransfer.InvalidFileRangeSpecificationException;
import org.eclipse.ecf.internal.provider.filetransfer.Activator;
import org.eclipse.ecf.internal.provider.filetransfer.IURLConnectionModifier;
import org.eclipse.ecf.internal.provider.filetransfer.Messages;
import org.eclipse.ecf.provider.filetransfer.util.JREProxyHelper;
import org.eclipse.osgi.util.NLS;

public class UrlConnectionRetrieveFileTransfer extends AbstractRetrieveFileTransfer {

	private static final String USERNAME_PREFIX = Messages.UrlConnectionRetrieveFileTransfer_USERNAME_PROMPT;

	private static final int HTTP_RANGE_RESPONSE = 206;

	private static final int OK_RESPONSE_CODE = 200;

	private static final String JRE_CONNECT_TIMEOUT_PROPERTY = "sun.net.client.defaultConnectTimeout"; //$NON-NLS-1$

	// 10/26/2009:  Added being able to set with system property with name org.eclipse.ecf.provider.filetransfer.connectTimeout
	// for https://bugs.eclipse.org/bugs/show_bug.cgi?id=292995
	private static final String DEFAULT_CONNECT_TIMEOUT = System.getProperty("org.eclipse.ecf.provider.filetransfer.retrieve.connectTimeout", "15000"); //$NON-NLS-1$ //$NON-NLS-2$

	private static final String JRE_READ_TIMEOUT_PROPERTY = "sun.net.client.defaultReadTimeout"; //$NON-NLS-1$

	protected URLConnection urlConnection;

	protected int httpVersion = 1;

	protected int responseCode = -1;

	private String remoteFileName;

	protected String responseMessage = null;

	private JREProxyHelper proxyHelper = null;

	protected String username = null;

	protected String password = null;

	public UrlConnectionRetrieveFileTransfer() {
		super();
		proxyHelper = new JREProxyHelper();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.provider.filetransfer.retrieve.AbstractRetrieveFileTransfer
	 * #getRemoteFileName()
	 */
	public String getRemoteFileName() {
		return remoteFileName;
	}

	protected void connect() throws IOException {
		setupTimeouts();
		urlConnection = getRemoteFileURL().openConnection();
		try {
			SSLContext.getDefault();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// set cache to off if using jar protocol
		// this is for addressing bug
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=235933
		if (getRemoteFileURL().getProtocol().equalsIgnoreCase("jar")) { //$NON-NLS-1$
			urlConnection.setUseCaches(false);
		}
		IURLConnectionModifier connectionModifier = Activator.getDefault().getURLConnectionModifier();
		if (connectionModifier != null) {
			connectionModifier.setSocketFactoryForConnection(urlConnection);
		}
	}

	protected boolean isConnected() {
		return (urlConnection != null);
	}

	protected void setResumeRequestHeaderValues() throws IOException {
		if (this.bytesReceived <= 0 || this.fileLength <= this.bytesReceived)
			throw new IOException(Messages.UrlConnectionRetrieveFileTransfer_RESUME_START_ERROR);
		setRangeHeader("bytes=" + this.bytesReceived + "-"); //$NON-NLS-1$ //$NON-NLS-2$
		int maxAge = Integer.getInteger("org.eclipse.ecf.http.cache.max-age", 0).intValue(); //$NON-NLS-1$
		// set max-age for cache control to 0 for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=249990
		// fix the fix for bug 249990 with bug 410813
		if (maxAge == 0) {
			urlConnection.setRequestProperty("Cache-Control", "max-age=0"); //$NON-NLS-1$//$NON-NLS-2$
		} else if (maxAge > 0) {
			urlConnection.setRequestProperty("Cache-Control", "max-age=" + maxAge); //$NON-NLS-1$//$NON-NLS-2$
		}
		setRequestHeaderValuesFromOptions();
	}

	private void setRequestHeaderValuesFromOptions() {
		Map localOptions = getOptions();
		if (localOptions != null) {
			Object o = localOptions.get(IRetrieveFileTransferOptions.REQUEST_HEADERS);
			if (o != null && o instanceof Map) {
				Map requestHeaders = (Map) o;
				for (Iterator i = requestHeaders.keySet().iterator(); i.hasNext();) {
					Object n = i.next();
					Object v = requestHeaders.get(n);
					if (n != null && n instanceof String && v != null && v instanceof String)
						urlConnection.addRequestProperty((String) n, (String) v);
				}
			}
		}
	}

	protected void setRequestHeaderValues() throws InvalidFileRangeSpecificationException {
		final IFileRangeSpecification rangeSpec = getFileRangeSpecification();
		if (rangeSpec != null && isHTTP()) {
			final long startPosition = rangeSpec.getStartPosition();
			final long endPosition = rangeSpec.getEndPosition();
			if (startPosition < 0)
				throw new InvalidFileRangeSpecificationException(Messages.UrlConnectionRetrieveFileTransfer_RESUME_START_POSITION_LESS_THAN_ZERO, rangeSpec);
			if (endPosition != -1L && endPosition <= startPosition)
				throw new InvalidFileRangeSpecificationException(Messages.UrlConnectionRetrieveFileTransfer_RESUME_ERROR_END_POSITION_LESS_THAN_START, rangeSpec);
			setRangeHeader("bytes=" + startPosition + "-" + ((endPosition == -1L) ? "" : ("" + endPosition))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		// Add http 1.1 'Connection: close' header in order to potentially avoid
		// server issue described here
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=234916#c13
		// See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=247197
		// also see http 1.1 rfc section 14-10 in
		// http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
		urlConnection.setRequestProperty("Connection", "close"); //$NON-NLS-1$ //$NON-NLS-2$
		int maxAge = Integer.getInteger("org.eclipse.ecf.http.cache.max-age", 0).intValue(); //$NON-NLS-1$
		// set max-age for cache control to 0 for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=249990
		// fix the fix for bug 249990 with bug 410813
		if (maxAge == 0) {
			urlConnection.setRequestProperty("Cache-Control", "max-age=0"); //$NON-NLS-1$//$NON-NLS-2$
		} else if (maxAge > 0) {
			urlConnection.setRequestProperty("Cache-Control", "max-age=" + maxAge); //$NON-NLS-1$//$NON-NLS-2$
		}
		setRequestHeaderValuesFromOptions();
	}

	private void setRangeHeader(String value) {
		urlConnection.setRequestProperty("Range", value); //$NON-NLS-1$
	}

	public int getResponseCode() {
		if (responseCode != -1)
			return responseCode;
		if (isHTTP()) {
			String response = urlConnection.getHeaderField(0);
			if (response == null) {
				responseCode = -1;
				httpVersion = 1;
				return responseCode;
			}
			if (!response.startsWith("HTTP/")) //$NON-NLS-1$
				return -1;
			response = response.trim();
			final int mark = response.indexOf(" ") + 1; //$NON-NLS-1$
			if (mark == 0)
				return -1;
			if (response.charAt(mark - 2) != '1')
				httpVersion = 0;
			int last = mark + 3;
			if (last > response.length())
				last = response.length();
			responseCode = Integer.parseInt(response.substring(mark, last));
			if (last + 1 <= response.length())
				responseMessage = response.substring(last + 1);
		} else {
			responseCode = OK_RESPONSE_CODE;
			responseMessage = "OK"; //$NON-NLS-1$
		}

		return responseCode;

	}

	private boolean isHTTP() {
		final String protocol = getRemoteFileURL().getProtocol();
		if (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https")) //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		return false;
	}

	private boolean isHTTP11() {
		return (isHTTP() && httpVersion >= 1);
	}

	protected void getResponseHeaderValues() throws IOException {
		if (!isConnected())
			throw new ConnectException(Messages.UrlConnectionRetrieveFileTransfer_CONNECT_EXCEPTION_NOT_CONNECTED);
		if (getResponseCode() == -1)
			throw new IOException(Messages.UrlConnectionRetrieveFileTransfer_EXCEPTION_INVALID_SERVER_RESPONSE);
		setLastModifiedTime(urlConnection.getLastModified());
		setFileLength(urlConnection.getContentLength());

		String contentDispositionValue = urlConnection.getHeaderField(HttpHelper.CONTENT_DISPOSITION_HEADER);
		if (contentDispositionValue != null) {
			remoteFileName = HttpHelper.getRemoteFileNameFromContentDispositionHeader(contentDispositionValue);
		}

		if (remoteFileName == null) {
			String pathStr = urlConnection.getURL().getPath();
			if (pathStr != null) {
				IPath path = Path.fromPortableString(pathStr);
				if (path.segmentCount() > 0)
					remoteFileName = path.lastSegment();
			}
			if (remoteFileName == null)
				remoteFileName = super.getRemoteFileName();
		}
	}

	protected void getResumeResponseHeaderValues() throws IOException {
		if (!isConnected())
			throw new ConnectException(Messages.UrlConnectionRetrieveFileTransfer_CONNECT_EXCEPTION_NOT_CONNECTED);
		if (getResponseCode() != HTTP_RANGE_RESPONSE)
			throw new IOException(Messages.UrlConnectionRetrieveFileTransfer_INVALID_SERVER_RESPONSE_TO_PARTIAL_RANGE_REQUEST);
		if (lastModifiedTime != urlConnection.getLastModified())
			throw new IOException(Messages.UrlConnectionRetrieveFileTransfer_EXCEPTION_FILE_MODIFIED_SINCE_LAST_ACCESS);
	}

	/**
	 * @param proxy2
	 *            the ECF proxy to setup
	 */
	protected void setupProxy(final Proxy proxy2) {
		proxyHelper.setupProxy(proxy2);
	}

	protected void setupAuthentication() throws IOException, UnsupportedCallbackException {
		if (connectContext == null)
			return;
		final CallbackHandler callbackHandler = connectContext.getCallbackHandler();
		if (callbackHandler == null)
			return;
		final NameCallback usernameCallback = new NameCallback(USERNAME_PREFIX);
		final ObjectCallback passwordCallback = new ObjectCallback();
		// Call callback with username and password callbacks
		callbackHandler.handle(new Callback[] {usernameCallback, passwordCallback});
		username = usernameCallback.getName();
		Object o = passwordCallback.getObject();
		if (!(o instanceof String))
			throw new UnsupportedCallbackException(passwordCallback, Messages.UrlConnectionRetrieveFileTransfer_UnsupportedCallbackException);
		password = (String) passwordCallback.getObject();
		// Now set authenticator to our authenticator with user and password
		Authenticator.setDefault(new UrlConnectionAuthenticator());
	}

	class UrlConnectionAuthenticator extends Authenticator {
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.net.Authenticator#getPasswordAuthentication()
		 */
		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(username, password.toCharArray());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#
	 * setConnectContextForAuthentication
	 * (org.eclipse.ecf.core.security.IConnectContext)
	 */
	public void setConnectContextForAuthentication(IConnectContext connectContext) {
		super.setConnectContextForAuthentication(connectContext);
		this.username = null;
		this.password = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.provider.filetransfer.retrieve.AbstractRetrieveFileTransfer
	 * #openStreams()
	 */
	protected void openStreams() throws IncomingFileTransferException {
		int code = -1;
		try {
			setupAuthentication();
			connect();
			setRequestHeaderValues();
			// Make actual GET request
			// need to get response header about encoding before setting stream
			setCompressionRequestHeader();
			setInputStream(getDecompressedStream());
			code = getResponseCode();
			responseHeaders = getResponseHeaders();
			if (isHTTP()) {
				if (code == HttpURLConnection.HTTP_PARTIAL || code == HttpURLConnection.HTTP_OK) {
					fireReceiveStartEvent();
				} else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
					throw new IncomingFileTransferException(NLS.bind("File not found: {0}", getRemoteFileURL().toString()), code, responseHeaders); //$NON-NLS-1$
				} else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
					throw new IncomingFileTransferException("Unauthorized", code, responseHeaders); //$NON-NLS-1$
				} else if (code == HttpURLConnection.HTTP_FORBIDDEN) {
					throw new IncomingFileTransferException("Forbidden", code, responseHeaders); //$NON-NLS-1$
				} else if (code == HttpURLConnection.HTTP_PROXY_AUTH) {
					throw new IncomingFileTransferException("Proxy authentication required", code, responseHeaders); //$NON-NLS-1$
				} else {
					throw new IncomingFileTransferException(NLS.bind("General connection error with response code={0}", Integer.valueOf(code)), code, responseHeaders); //$NON-NLS-1$
				}
			} else {
				fireReceiveStartEvent();
			}
		} catch (final FileNotFoundException e) {
			throw new IncomingFileTransferException(NLS.bind("File not found: {0}", getRemoteFileURL().toString()), 404); //$NON-NLS-1$
		} catch (final Exception e) {
			IncomingFileTransferException except = (e instanceof IncomingFileTransferException) ? (IncomingFileTransferException) e : new IncomingFileTransferException(NLS.bind(Messages.UrlConnectionRetrieveFileTransfer_EXCEPTION_COULD_NOT_CONNECT, getRemoteFileURL().toString()), e, code, responseHeaders);
			hardClose();
			throw except;
		}
	}

	private Map getResponseHeaders() {
		if (responseHeaders != null)
			return responseHeaders;
		if (urlConnection == null)
			return null;
		Map headerFields = urlConnection.getHeaderFields();
		if (headerFields == null)
			return null;
		Map result = new HashMap();
		for (Iterator i = headerFields.keySet().iterator(); i.hasNext();) {
			String name = (String) i.next();
			List listValue = (List) headerFields.get(name);
			String val = null;
			if (listValue != null && listValue.size() > 0) {
				val = (String) ((listValue.size() > 1) ? listValue.get(listValue.size() - 1) : listValue.get(0));
			}
			if (name != null && val != null)
				result.put(name, val);
		}
		return Collections.unmodifiableMap(result);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.provider.filetransfer.retrieve.AbstractRetrieveFileTransfer
	 * #hardClose()
	 */
	protected void hardClose() {
		super.hardClose();
		urlConnection = null;
		responseCode = -1;
		if (proxyHelper != null) {
			proxyHelper.dispose();
			proxyHelper = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.provider.filetransfer.retrieve.AbstractRetrieveFileTransfer
	 * #doPause()
	 */
	protected boolean doPause() {
		if (isPaused() || !isConnected() || isDone())
			return false;
		this.paused = true;
		return this.paused;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.provider.filetransfer.retrieve.AbstractRetrieveFileTransfer
	 * #doResume()
	 */
	protected boolean doResume() {
		if (!isPaused() || isConnected())
			return false;
		return openStreamsForResume();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.provider.filetransfer.retrieve.AbstractRetrieveFileTransfer
	 * #getAdapter(java.lang.Class)
	 */
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == null)
			return null;
		if (adapter.equals(IFileTransferPausable.class) && isHTTP11())
			return adapter.cast(this);
		return super.getAdapter(adapter);
	}

	protected String getConnectTimeout() {
		String result = DEFAULT_CONNECT_TIMEOUT;
		Map localOptions = getOptions();
		if (localOptions != null) {
			// See if the connect timeout option is present, if so set
			Object o = localOptions.get(IRetrieveFileTransferOptions.CONNECT_TIMEOUT);
			if (o != null) {
				if (o instanceof Integer) {
					result = ((Integer) o).toString();
				} else if (o instanceof String) {
					result = (String) o;
				}
				return result;
			}
			o = localOptions.get("org.eclipse.ecf.provider.filetransfer.httpclient.retrieve.connectTimeout"); //$NON-NLS-1$
			if (o != null) {
				if (o instanceof Integer) {
					result = ((Integer) o).toString();
				} else if (o instanceof String) {
					result = (String) o;
				}
			}
		}
		return result;
	}

	private void setupTimeouts() {
		String existingTimeout = System.getProperty(JRE_CONNECT_TIMEOUT_PROPERTY);
		if (existingTimeout == null) {
			System.setProperty(JRE_CONNECT_TIMEOUT_PROPERTY, getConnectTimeout());
		}
		existingTimeout = System.getProperty(JRE_READ_TIMEOUT_PROPERTY);
		if (existingTimeout == null) {
			System.setProperty(JRE_READ_TIMEOUT_PROPERTY, "" + getSocketReadTimeout()); //$NON-NLS-1$
		}
	}

	/**
	 * @return <code>true</code> if streams successfully, <code>false</code>
	 *         otherwise.
	 */
	private boolean openStreamsForResume() {
		final URL theURL = getRemoteFileURL();
		int code = -1;
		try {
			remoteFileURL = new URL(theURL.toString());
			setupAuthentication();
			connect();
			setResumeRequestHeaderValues();
			// Make actual GET request
			setInputStream(urlConnection.getInputStream());
			code = getResponseCode();
			responseHeaders = getResponseHeaders();
			if (code == HttpURLConnection.HTTP_PARTIAL || code == HttpURLConnection.HTTP_OK) {
				getResumeResponseHeaderValues();
				this.paused = false;
				fireReceiveResumedEvent();
				return true;
			} else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
				throw new IncomingFileTransferException(NLS.bind("File not found: {0}", getRemoteFileURL().toString()), code, responseHeaders); //$NON-NLS-1$
			} else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new IncomingFileTransferException("Unauthorized", code, responseHeaders); //$NON-NLS-1$
			} else if (code == HttpURLConnection.HTTP_FORBIDDEN) {
				throw new IncomingFileTransferException("Forbidden", code, responseHeaders); //$NON-NLS-1$
			} else if (code == HttpURLConnection.HTTP_PROXY_AUTH) {
				throw new IncomingFileTransferException("Proxy authentication required", code, responseHeaders); //$NON-NLS-1$
			} else {
				throw new IncomingFileTransferException(NLS.bind("General connection error with response code={0}", Integer.valueOf(code)), code, responseHeaders); //$NON-NLS-1$
			}
		} catch (final Exception e) {
			this.exception = (e instanceof IncomingFileTransferException) ? e : new IncomingFileTransferException(NLS.bind(Messages.UrlConnectionRetrieveFileTransfer_EXCEPTION_COULD_NOT_CONNECT, getRemoteFileURL().toString()), e, code, responseHeaders);
			this.done = true;
			hardClose();
			fireTransferReceiveDoneEvent();
			return false;
		}
	}

	private static final String ACCEPT_ENCODING = "Accept-encoding"; //$NON-NLS-1$
	private static final String CONTENT_ENCODING_GZIP = "gzip"; //$NON-NLS-1$

	private static final String CONTENT_ENCODING_ACCEPTED = CONTENT_ENCODING_GZIP; // +

	private static class Compression {

		private String type;

		private Compression(String i) {
			this.type = i;
		}

		static Compression NONE = new Compression("none"); //$NON-NLS-1$

		static Compression GZIP = new Compression("gzip"); //$NON-NLS-1$

		public String toString() {
			return type;
		}
	}

	private void setCompressionRequestHeader() {
		// Set request header for possible gzip encoding, but only if
		// 1) The file range specification is null (we want the whole file)
		// 2) The target remote file does *not* end in .gz (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=280205)
		if (getFileRangeSpecification() == null && !targetHasGzSuffix(super.getRemoteFileName()))
			urlConnection.setRequestProperty(ACCEPT_ENCODING, CONTENT_ENCODING_ACCEPTED);
	}

	private Compression getCompressionResponseHeader() {
		String encoding = urlConnection.getContentEncoding();
		if (null == encoding) {
			return Compression.NONE;
			// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=269018
		} else if (encoding.equalsIgnoreCase(CONTENT_ENCODING_GZIP) && !targetHasGzSuffix(remoteFileName)) {
			return Compression.GZIP;
		}
		return Compression.NONE;
	}

	private InputStream getDecompressedStream() throws IOException {
		InputStream input = urlConnection.getInputStream();
		getResponseHeaderValues();
		Compression type = getCompressionResponseHeader();

		if (Compression.GZIP == type) {
			return new java.util.zip.GZIPInputStream(input);
			// } else if (Compression.DEFLATE == type) {
			// return new java.util.zip.InflaterInputStream(input);
		}
		return input;
	}
}
