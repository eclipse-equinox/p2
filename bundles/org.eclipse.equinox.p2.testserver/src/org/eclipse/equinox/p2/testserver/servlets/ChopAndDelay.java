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
import org.eclipse.equinox.p2.testserver.LinearChange;

/**
 * The ChopAndDelay deliver the content chopped up in smaller packets and adds delay
 * between packets.
 * 
 */
public class ChopAndDelay extends BasicResourceDelivery {

	int chopFactor;
	private LinearChange delayFunction;
	private long msDelay;
	private int fastPercent;

	/**
	 * Create a file molester that turns content into gibberish.
	 * 
	 * @param theAlias the path this servlet is registered under
	 * @param thePath the path to use as root for the alias
	 * @param chopFactor - a value between 1 and 12 where 1 is one byte, and 12 is 4k bytes at a time.
	 * @param delayFunction - function returning a series of delay values
	 */
	public ChopAndDelay(String theAlias, URI thePath, int chopFactor, int fastPercent, LinearChange delayFunction) {
		super(theAlias, thePath);
		if (chopFactor < 1 || chopFactor > 12)
			throw new IllegalArgumentException("chopFactor must be between 1 and 12 (inclusive) - was:" + Integer.valueOf(chopFactor)); //$NON-NLS-1$
		this.chopFactor = chopFactor;
		if (fastPercent < 0 || fastPercent > 100)
			throw new IllegalArgumentException("fastPercent must be 0-100 - was:" + Integer.valueOf(fastPercent)); //$NON-NLS-1$
		this.fastPercent = fastPercent;
		this.delayFunction = delayFunction;
		msDelay = 0L;
		this.delayFunction = delayFunction;
		if (this.delayFunction.hasNext())
			msDelay = delayFunction.next();
	}

	private static final long serialVersionUID = 1L;

	protected void deliver(URLConnection conn, InputStream in, String filename, HttpServletRequest request, HttpServletResponse response) throws IOException {
		// chop up all files
		doChop(conn, in, filename, request, response);
	}

	protected void doChop(URLConnection conn, InputStream in, String filename, HttpServletRequest request, HttpServletResponse response) throws IOException {
		LinearChange delayer = delayFunction.fork();
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

				int chunksize = 1 << chopFactor;
				char buffer[] = new char[4096];
				int read;
				int totalRead = 0;
				boolean delay = fastPercent == 0 ? true : false;
				while ((read = reader.read(buffer, 0, buffer.length)) != -1) {
					int nChunks = read / chunksize + (read % chunksize > 0 ? 1 : 0);
					for (int i = 0; i < nChunks; i++) {
						writer.write(buffer, i * chunksize, Math.min(chunksize, read - i * chunksize));
						writer.flush();
						if (delay && msDelay > 0)
							try {
								Thread.sleep(msDelay);
							} catch (InterruptedException e) {
								// ignore
							}
						if (delay && delayer.hasNext())
							msDelay = delayer.next();
					}
					totalRead += read;
					if (totalRead > contentlength * fastPercent / 100)
						delay = true;
				}
			} else {
				ServletOutputStream out = response.getOutputStream();

				out.flush(); /* write the headers and unbuffer the output */

				int chunksize = 1 << chopFactor;
				byte buffer[] = new byte[4096];
				int read;
				int totalRead = 0;
				boolean delay = fastPercent == 0 ? true : false;
				while ((read = in.read(buffer, 0, buffer.length)) != -1) {
					int nChunks = read / chunksize + (read % chunksize > 0 ? 1 : 0);
					for (int i = 0; i < nChunks; i++) {
						out.write(buffer, i * chunksize, Math.min(chunksize, read - i * chunksize));
						out.flush();
						if (delay && msDelay > 0)
							try {
								Thread.sleep(msDelay);
							} catch (InterruptedException e) {
								// ignore
							}
						if (delay && delayer.hasNext())
							msDelay = delayer.next();
					}
					totalRead += read;
					if (totalRead > contentlength * fastPercent / 100)
						delay = true;
				}
			}
		}
	}

}
