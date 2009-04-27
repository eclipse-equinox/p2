/*******************************************************************************
 * Copyright (c) 2009, Cloudsmith Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.testserver.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLConnection;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Truncator extends BasicResourceDelivery {

	double keepFactor;

	public Truncator(String theAlias, URI thePath, int keepPercent) {
		super(theAlias, thePath);
		if (keepPercent < 0 || keepPercent > 100)
			throw new IllegalArgumentException("keepPercent must be between 0 and 100 - was:" + Integer.valueOf(keepPercent)); //$NON-NLS-1$
		keepFactor = keepPercent / 100.0;
	}

	private static final long serialVersionUID = 1L;

	protected void deliver(URLConnection conn, InputStream in, String filename, HttpServletRequest request, HttpServletResponse response) throws IOException {
		// truncate all files
		doTruncate(conn, in, filename, request, response);
	}

	protected void doTruncate(URLConnection conn, InputStream in, String filename, HttpServletRequest request, HttpServletResponse response) throws IOException {
		int contentlength = conn.getContentLength();
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
				int written = 0;
				while ((read = reader.read(buffer, 0, buffer.length)) != -1) {
					read = cap(contentlength, written, read);
					writer.write(buffer, 0, read);
					written += read;
				}
			} else {
				ServletOutputStream out = response.getOutputStream();

				out.flush(); /* write the headers and unbuffer the output */

				byte buffer[] = new byte[4096];
				int read;
				int written = 0;
				while ((read = in.read(buffer, 0, buffer.length)) != -1) {
					read = cap(contentlength, written, read);
					out.write(buffer, 0, read);
					written += read;
				}
			}
		}
	}

	/**
	 * Returns read if entire amount should be read. Returns a lower number if
	 * written + read > keepPercent of total
	 * @param total
	 * @param written
	 * @param read
	 * @return read, or a lower number if cap is reached
	 */
	private int cap(int total, int written, int read) {
		int cap = (int) (keepFactor * total);
		return (read + written) > cap ? cap - written : read;
	}

	protected void deliverHead(String filename, URLConnection conn, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// deliver normal head response
		super.doDeliverHead(filename, conn, request, response);

	}

}
