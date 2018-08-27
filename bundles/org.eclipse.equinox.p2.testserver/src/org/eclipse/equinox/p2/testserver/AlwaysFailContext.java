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

package org.eclipse.equinox.p2.testserver;

import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.service.http.HttpContext;

/**
 * The AlwaysFailContext can be used to add basic authentication to a path. This
 * implementation will always fail (easier to test failure logic) this way.
 */
public class AlwaysFailContext implements HttpContext {
	private final HttpContext theDefaultContext;

	public AlwaysFailContext(HttpContext defaultContext) {
		theDefaultContext = defaultContext;
	}

	@Override
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
		// always fail
		response.setHeader("WWW-Authenticate", "BASIC realm=\"p2 Http Testing Server (Aladdin, open sesame)\""); //$NON-NLS-1$//$NON-NLS-2$
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		return false;

	}

	@Override
	public String getMimeType(String name) {
		return theDefaultContext.getMimeType(name);
	}

	@Override
	public URL getResource(String name) {
		return theDefaultContext.getResource(name);
	}

}
