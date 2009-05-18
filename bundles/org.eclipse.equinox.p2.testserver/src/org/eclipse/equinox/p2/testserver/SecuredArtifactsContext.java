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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.service.http.HttpContext;

/**
 * The SecureContext can be used to add basic authentication to a path.
 * This implementation requires the user "Aladdin" to log in with the password "open sesame".
 */
public class SecuredArtifactsContext extends SecureContext {

	public SecuredArtifactsContext(HttpContext defaultContext) {
		super(defaultContext);
	}

	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
		String path = request.getRequestURI();
		if (path == null)
			return true;

		if (path.indexOf("features/") != -1 //$NON-NLS-1$
				|| path.indexOf("plugins/") != -1 //$NON-NLS-1$
				|| path.indexOf("binaries/") != -1) //$NON-NLS-1$
			return super.handleSecurity(request, response);

		return true;
	}

}
