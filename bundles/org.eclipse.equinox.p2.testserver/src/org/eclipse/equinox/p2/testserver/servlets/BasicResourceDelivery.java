/*******************************************************************************
 * Copyright (c) 2009, Cloudsmith Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  IBM Corporation - initial API and implementation
 *  Cloudsmith Inc. - this copy and adaption to abstract servlet
 *******************************************************************************/
package org.eclipse.equinox.p2.testserver.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.equinox.p2.testserver.Activator;
import org.eclipse.equinox.p2.testserver.HttpConstants;
import org.eclipse.equinox.p2.testserver.MimeLookup;
import org.eclipse.equinox.p2.testserver.SecureAction;
import org.osgi.service.http.HttpService;

public class BasicResourceDelivery extends HttpServlet {
	private static final long serialVersionUID = 1L;
	// private static final String CHARSET = "utf-8"; //$NON-NLS-1$
	private SecureAction secureAction;
	private String alias;
	private URI path;
	protected static final String defaultMimeType = "application/octet-stream"; //$NON-NLS-1$

	/**
	 * Delivers resources from the bundle, or from an absolute URI
	 */
	public BasicResourceDelivery(String theAlias, URI thePath) {
		secureAction = new SecureAction();
		alias = theAlias;
		path = thePath;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		URI resource = null;
		try {
			resource = getFilename(request.getRequestURI());
		} catch (URISyntaxException e) {
			/* ignore - just leave resource == null */
		}

		URL url = null;
		if (resource != null) {
			if (!resource.isAbsolute())
				url = getServletContext().getResource(resource.getPath());
			else
				url = resource.toURL();
		}

		if (url == null) {
			fileNotFound(resource, request, response);
			return;
		}

		// process cache control
		URLConnection conn = secureAction.openURL(url);

		long modifiedSince = request.getDateHeader("If-Modified-Since"); //$NON-NLS-1$
		if (modifiedSince >= 0) {
			long modified = getLastModified(conn);
			if ((modified > 0) && (modifiedSince >= modified)) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			}
		}
		InputStream in;
		try {
			in = conn.getInputStream();
		} catch (IOException ex) {
			fileNotFound(resource, request, response);
			return;
		}
		try {
			// always set the default charset
			// not in Servlet 2.1 // response.setCharacterEncoding(CHARSET);
			deliver(conn, in, resource.toString(), request, response); // TODO: modify to resource==URI ?
		} finally {
			in.close();
		}
	}

	protected void deliver(URLConnection conn, InputStream in, String filename, HttpServletRequest request, HttpServletResponse response) throws IOException {
		this.doDeliver(conn, in, filename, request, response);
	}

	/** 
	 * Default method delivering content from a file. Subclasses should override this method
	 * to deliver "broken content" (e.g. truncated, mismatched content length etc.).
	 * This default method performs the same delivery as the default "register resource" in the
	 * http service.
	 * @param conn - The URLConnection to the resource
	 * @param in - InputStream to read from
	 * @param filename - the filename being read
	 * @param request - the servlet request
	 * @param response - the servlet response
	 * @throws IOException - on errors
	 */
	protected void doDeliver(URLConnection conn, InputStream in, String filename, HttpServletRequest request, HttpServletResponse response) throws IOException {
		// set when the resource was modified
		addDateHeader(response, HttpConstants.LAST_MODIFIED, getLastModified(conn));
		int statusCode = HttpHeaderToStatus(conn.getHeaderField(0));

		response.setStatus(statusCode != -1 ? HttpServletResponse.SC_OK : statusCode);

		int contentlength = getContentLength(conn);
		if (contentlength >= 0) {
			response.setContentLength(contentlength);

			String mimeType = computeMimeType(filename, conn);
			response.setContentType(mimeType);

			// We want to use a writer if we are sending text
			if (mimeType.startsWith("text/")) //$NON-NLS-1$
			{
				PrintWriter writer = response.getWriter();

				writer.flush(); /* write the headers and unbuffer the output */

				BufferedReader reader = new BufferedReader(new InputStreamReader(in));

				char buffer[] = new char[4096];
				int read;
				while ((read = reader.read(buffer, 0, buffer.length)) != -1) {
					writer.write(buffer, 0, read);
				}
			} else {
				ServletOutputStream out = response.getOutputStream();

				out.flush(); /* write the headers and unbuffer the output */

				byte buffer[] = new byte[4096];
				int read;
				while ((read = in.read(buffer, 0, buffer.length)) != -1) {
					out.write(buffer, 0, read);
					out.flush();
				}
			}
		}
	}

	protected void fileNotFound(URI file, HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, (file == null ? "<no file>" : file.toString()) + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		URI resource = null;
		try {
			resource = getFilename(request.getRequestURI());
		} catch (URISyntaxException e) {
			/* ignore - just leave resource == null */
		}

		URL url = null;
		if (resource != null) {
			if (!resource.isAbsolute())
				url = getServletContext().getResource(resource.getPath());
			else
				url = resource.toURL();
		}

		if (url == null) {
			fileNotFound(resource, request, response);
			return;
		}
		URLConnection conn = secureAction.openURL(url);
		// always set default charset
		// Not in Servlet 2.1 // response.setCharacterEncoding(CHARSET);
		doDeliverHead(resource.toString(), conn, request, response); // TODO: change API to use URI?
	}

	protected void deliverHead(String filename, URLConnection conn, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.doDeliverHead(filename, conn, request, response);
	}

	protected void doDeliverHead(String filename, URLConnection conn, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		int contentlength = getContentLength(conn);
		int statusCode = HttpHeaderToStatus(conn.getHeaderField(0));
		// set status = ok if there was no http header...
		response.setStatus(statusCode == -1 ? HttpServletResponse.SC_OK : statusCode);
		if (contentlength >= 0) {
			response.setContentLength(contentlength);

			String mimeType = computeMimeType(filename, conn);
			response.setContentType(mimeType);
			long modified = getLastModified(conn);
			addDateHeader(response, HttpConstants.LAST_MODIFIED, modified);
		} else {
			super.doHead(request, response);
		}

	}

	public int HttpHeaderToStatus(String httpHeader) {
		if (httpHeader == null)
			return -1;
		try {
			return Integer.valueOf((httpHeader.substring(9, 12))).intValue();
		} catch (IndexOutOfBoundsException e) {
			return -1;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Override to lie about last modified
	 * @param conn
	 * @return time in ms
	 */
	protected long getLastModified(URLConnection conn) {
		return conn.getLastModified();
	}

	/**
	 * Override to lie about content length
	 * @param conn
	 * @return length in bytes
	 */
	protected int getContentLength(URLConnection conn) {
		return conn.getContentLength();
	}

	protected URI getFilename(String filename) throws URISyntaxException {
		//If the requested URI is equal to the Registeration's alias, send the file
		//corresponding to the alias.  Otherwise, we have request for a file in an
		//registered directory (the file was not directly registered itself).
		if (filename.equals(alias)) {
			filename = path.getPath();
		} else {
			// The file we re looking for is the registered resource (alias) + the rest of the
			// filename that is not part of the registered resource.  For example, if we export
			// /a to /tmp and we have a request for /a/b/foo.txt, then /tmp is our directory
			// (file.toString()) and /b/foo.txt is the rest.
			// The result is that we open the file /tmp/b/foo.txt.

			int aliaslen = alias.length();
			int pathlen = path.getPath().length();

			if (pathlen == 1) /* path == "/" */
			{
				if (aliaslen > 1) /* alias != "/" */
				{
					filename = filename.substring(aliaslen);
				}
			} else /* path != "/" */
			{
				StringBuffer buf = new StringBuffer(aliaslen + pathlen);
				buf.append(path.getPath());

				if (aliaslen == 1) /* alias == "/" */
				{
					buf.append(filename);
				} else /* alias != "/" */
				{
					buf.append(filename.substring(aliaslen));
				}

				filename = buf.toString();
			}
		}
		return new URI(path.getScheme(), path.getUserInfo(), path.getHost(), path.getPort(), filename, path.getQuery(), path.getFragment());
		// return (filename);
	}

	/**
	 * This method returns the correct MIME type of a given URI by first checking
	 * the HttpContext::getMimeType and, if null, checking the httpservice's MIMETypes table.
	 * return mimetype with charset=utf-8 for all text/... types
	 */
	protected String computeMimeType(String name, URLConnection conn) {
		String mimeType = computeMimeType2(name, conn);
		if (mimeType.startsWith("text/")) //$NON-NLS-1$
			return mimeType + "; charset=utf-8"; //$NON-NLS-1$
		return mimeType;
	}

	protected String computeMimeType2(String name, URLConnection conn) {
		// use type set in connection if it is available
		String mimeType = conn.getContentType();
		if (mimeType != null) {
			return (mimeType);
		}
		// use type from context
		mimeType = getServletContext().getMimeType(name);
		if (mimeType != null) {
			return (mimeType);
		}
		// try the "extras"
		return MimeLookup.getMimeType(name);
	}

	public HttpService getHttpService() {
		return Activator.getInstance().getHttp();
	}

	public static final DateFormat PATTERN_RFC1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US); //$NON-NLS-1$

	/**
	 * Support setting date header - in servlet > 2.1 it is possible to call this method directly on the HttpResponse.
	 * @param response
	 * @param name
	 * @param timestamp
	 */
	public void addDateHeader(HttpServletResponse response, String name, long timestamp) {
		DateFormat df = PATTERN_RFC1123;
		// must always be GMT
		df.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
		response.setHeader(name, df.format(new Date(timestamp)));

	}

}
