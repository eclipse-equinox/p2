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

package org.eclipse.equinox.p2.testserver;

import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.service.http.HttpContext;

/**
 * The SecureContext can be used to add basic authentication to a path.
 * It will fail on every other request to enable flaky server, or authentication testing.
 * This implementation requires the user "Aladdin" to log in with the password "open sesame".
 */
public class FlipFlopFailContext implements HttpContext {
	private HttpContext theDefaultContext;
	public static int flipFlop = 0;

	public FlipFlopFailContext(HttpContext defaultContext) {
		theDefaultContext = defaultContext;
	}

	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
		String auth = request.getHeader("Authorization"); //$NON-NLS-1$
		// Response is Aladdin:open sesame in Base64 encoding (from RFC example)
		if (auth != null && "QWxhZGRpbjpvcGVuIHNlc2FtZQ==".equals(auth.substring(6))) //$NON-NLS-1$
			return (flipFlop ^= 1) == 1 ? true : false;

		// if not authorized or wrong user/password
		response.setHeader("WWW-Authenticate", "BASIC realm=\"p2 Http Testing Server (Aladdin, open sesame)\""); //$NON-NLS-1$//$NON-NLS-2$
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		return false;

	}

	public String getMimeType(String name) {
		return theDefaultContext.getMimeType(name);
	}

	public URL getResource(String name) {
		return theDefaultContext.getResource(name);
	}

}
