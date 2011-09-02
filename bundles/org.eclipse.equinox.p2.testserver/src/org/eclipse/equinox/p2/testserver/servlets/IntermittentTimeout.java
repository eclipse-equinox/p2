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
import org.eclipse.equinox.p2.testserver.HttpConstants;

public class IntermittentTimeout extends BasicResourceDelivery {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2216234319571297257L;
	int count = 1;

	public IntermittentTimeout(String theAlias, URI thePath) {
		super(theAlias, thePath);
	}

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

				doDelay(filename, 150);

				BufferedReader reader = new BufferedReader(new InputStreamReader(in));

				char buffer[] = new char[4096];
				int read;
				while ((read = reader.read(buffer, 0, buffer.length)) != -1) {
					writer.write(buffer, 0, read);
				}
			} else {
				ServletOutputStream out = response.getOutputStream();

				out.flush(); /* write the headers and unbuffer the output */

				doDelay(filename, 150);

				byte buffer[] = new byte[4096];
				int read;
				while ((read = in.read(buffer, 0, buffer.length)) != -1) {
					out.write(buffer, 0, read);
					out.flush();
				}
			}
		}
	}

	private void doDelay(String filename, int seconds) {
		if (filename.endsWith("emptyJarRepo/plugins/HelloWorldText_1.0.0.txt") && (count++ % 3 != 0)) {//$NON-NLS-1$
			try {
				Thread.sleep(1000 * seconds);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}
}
