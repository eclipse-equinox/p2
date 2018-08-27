/*******************************************************************************
 * Copyright (c) 2009, 2018 Cloudsmith Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
 * Simple "smoke test" servlet - for manual testing that the http server is
 * running and is capable of running servlets.
 */
public class HelloWorld extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/html"); //$NON-NLS-1$
		PrintWriter writer = response.getWriter();
		writer.println("<html>"); //$NON-NLS-1$
		writer.println("<body>"); //$NON-NLS-1$
		writer.println("Hello, Equinox Embedded HTTP World"); //$NON-NLS-1$
		writer.println("<br/>"); //$NON-NLS-1$
		writer.println("</body>"); //$NON-NLS-1$
		writer.println("</html>"); //$NON-NLS-1$
	}
}
