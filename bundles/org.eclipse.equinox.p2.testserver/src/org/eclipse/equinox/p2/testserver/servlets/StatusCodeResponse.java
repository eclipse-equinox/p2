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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Produces a response with a status code specified in the request URI.
 * Requires to be registered for "/status" path. The request is made on the format
 * "/status/code/" where code is the HTTP integer status code. The path after /code/ can be
 * anything - it is always ignored.
 * The response to GET produces HTML text "Requested status: code".
 * The response to HEAD just produces the status header response.
 * 
 */
public class StatusCodeResponse extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/html"); //$NON-NLS-1$
		PrintWriter writer = response.getWriter();
		doStatus(request, response, writer);
	}

	protected void doStatus(HttpServletRequest request, HttpServletResponse response, PrintWriter writer) {
		String requestPath = request.getRequestURI();
		String[] result = requestPath.split("/"); //$NON-NLS-1$
		if (result.length < 3 && !"status".equalsIgnoreCase(result[1])) //$NON-NLS-1$
		{
			getServletContext().log("Error Servlet requires being configured to get /status/statuscode paths. Example /status/500, got" + requestPath); //$NON-NLS-1$
			return;
		}
		// get the error code
		int errorCode = 0;
		try {
			errorCode = Integer.parseInt(result[2]);
		} catch (NumberFormatException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			getServletContext().log("Number format exception in /status/statuscode path.", e); //$NON-NLS-1$
			return;
		}

		// set the errorCode as the response and write a message
		response.setStatus(errorCode);
		if (writer != null)
			htmlPage(writer, "Requested Status: " + Integer.valueOf(errorCode), false); //$NON-NLS-1$
	}

	public void doHead(HttpServletRequest request, HttpServletResponse response) {
		// produce same reponse as for GET, but no content (writer == null)
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
