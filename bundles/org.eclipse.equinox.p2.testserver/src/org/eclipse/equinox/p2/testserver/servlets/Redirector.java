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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Redirects n times, before redirecting to final location (a path on same server).
 * 
 */
public class Redirector extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/html"); //$NON-NLS-1$
		PrintWriter writer = response.getWriter();
		doStatus(request, response, writer);
	}

	protected void doStatus(HttpServletRequest request, HttpServletResponse response, PrintWriter writer) {
		String requestPath = request.getRequestURI();
		String[] result = requestPath.split("/"); //$NON-NLS-1$
		if (result.length < 3 && !"redirect".equalsIgnoreCase(result[1])) //$NON-NLS-1$
		{
			getServletContext().log("Error Servlet requires being configured to get /redirect/count paths. Example /redirect/500, got" + requestPath); //$NON-NLS-1$
			return;
		}
		// get the error code
		int iterations = 0;
		try {
			iterations = Integer.parseInt(result[2]);
		} catch (NumberFormatException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			getServletContext().log("Number format exception in /redirect/count path.", e); //$NON-NLS-1$
			return;
		}
		String redirectPath;
		if (iterations == 0) {
			if (result.length > 3)
				redirectPath = requestPath.substring("/redirect/0".length()); //$NON-NLS-1$
			else
				redirectPath = null;
		} else {
			redirectPath = "/redirect/" + Integer.valueOf(iterations - 1); //$NON-NLS-1$
			for (int i = 3; i < result.length; i++)
				redirectPath += "/" + result[i]; //$NON-NLS-1$
		}

		if (redirectPath != null) {
			// perform a redirect
			URI location = null;
			try {
				location = new URI(request.getScheme(), null, request.getServerName(), request.getServerPort(), //
						redirectPath, null, null);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				htmlPage(writer, "Internal error constructing redirect URL", false); //$NON-NLS-1$
				return;
			}

			response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
			response.setHeader("Location", location.toString()); //$NON-NLS-1$
		} else {
			// set the errorCode as the response and write a message
			if (writer != null)
				htmlPage(writer, "Redirected Successfully", false); //$NON-NLS-1$
		}
	}

	public void doHead(HttpServletRequest request, HttpServletResponse response) {
		// produce same response as for GET, but no content (writer == null)
		doStatus(request, response, null);
	}

	private void htmlPage(PrintWriter writer, String body, boolean consoleOutput) {
		if (consoleOutput)
			System.err.println(body);

		writer.println("<html>"); //$NON-NLS-1$
		writer.println("<body>"); //$NON-NLS-1$
		writer.println(body);
		writer.println("<br/>"); //$NON-NLS-1$
		writer.println("</body>"); //$NON-NLS-1$
		writer.println("</html>"); //$NON-NLS-1$
		writer.flush();
	}
}
