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
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The FileMolester will keep a certain amount of the file from the beginning, and return
 * garbage/gibberish for the rest of the file's content. The garbage is produced by shifting every
 * byte 1 bit to the left.
 * 
 * The idea for keeping some percentage is to support keeping xml headers / doctype etc.
 */
public class FileMolester extends BasicResourceDelivery {

	int keepLength;

	/**
	 * Create a file molester that turns content into gibberish.
	 * 
	 * @param theAlias the path this servlet is registered under
	 * @param thePath the path to use as root for the alias
	 * @param keepLength - how many bytes in the beginning that will be unmolested
	 */
	public FileMolester(String theAlias, URI thePath, int keepLength) {
		super(theAlias, thePath);
		if (keepLength < 0)
			throw new IllegalArgumentException("keepLength must be >= 0 - was:" + Integer.valueOf(keepLength)); //$NON-NLS-1$
		this.keepLength = keepLength;
	}

	private static final long serialVersionUID = 1L;

	protected void deliver(URLConnection conn, InputStream in, String filename, HttpServletRequest request, HttpServletResponse response) throws IOException {
		// molest all files
		doMolest(conn, in, filename, request, response);
	}

	protected void doMolest(URLConnection conn, InputStream in, String filename, HttpServletRequest request, HttpServletResponse response) throws IOException {
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
					if (written + read > keepLength)
						molest(buffer, written, read);
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
					if (written + read > keepLength)
						molest(buffer, written, read);
					out.write(buffer, 0, read);
					written += read;
				}
			}
		}
	}

	/** Molest a char buffer */
	private void molest(char[] buffer, int written, int read) {
		int start = keepLength - written;
		if (start > read)
			return;
		for (int i = start < 0 ? 0 : start; i < read; i++)
			buffer[i] = (char) (buffer[i] << 1);
	}

	/** Molest a byte buffer */
	private void molest(byte[] buffer, int written, int read) {
		int start = keepLength - written;
		if (start > read)
			return;
		for (int i = start < 0 ? 0 : start; i < read; i++)
			buffer[i] = (byte) (buffer[i] << 1);
	}
}
