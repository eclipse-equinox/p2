/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.core;

import java.security.cert.Certificate;

/**
 * Callback API for prompting for user information from within lower level code.
 */
public interface IServiceUI {
	/**
	 * Authentication information returned from an authentication prompt request.
	 */
	public static class AuthenticationInfo {
		private final boolean save;
		private final String userName;
		private final String password;

		public AuthenticationInfo(String userName, String password, boolean saveResult) {
			this.userName = userName;
			this.password = password;
			this.save = saveResult;
		}

		public boolean saveResult() {
			return save;
		}

		public String getUserName() {
			return userName;
		}

		public String getPassword() {
			return password;
		}
	}

	/**
	 * Opens a UI prompt for authentication details
	 * 
	 * @param location - the location requiring login details, may be <code>null</code>.
	 * @return The authentication result
	 */
	public AuthenticationInfo getUsernamePassword(String location);

	/**
	 * Displays a list of certificates to the user.
	 *  
	 * @param certificates - a list of certificates to display to the user
	 * @return An array of certificates that have been accepted.
	 */
	public Certificate[] showCertificates(Certificate[][] certificates);
}
