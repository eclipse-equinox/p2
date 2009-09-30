/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.core;

import java.security.cert.Certificate;

/**
 * Service used for prompting for user information from within lower level code.
 * Implementors of this service are responsible for registering the service.
 * 
 * It is possible that the IServiceUI service is requested very early in the startup
 * sequence for an application.  For example, applications that check for updates 
 * during startup will trigger the service lookup if a server requiring authentication
 * is detected.  For this reason, implementors of IServiceUI should ensure that the 
 * bundle providing the service is partitioned appropriately.
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
	 * Opens a UI prompt for authentication details when cached or remembered details
	 * where not accepted.
	 * 
	 * @param location  the location requiring login details
	 * @param previousInfo - the previously used authentication details - may not be null.
	 * @return The authentication result
	 */
	public AuthenticationInfo getUsernamePassword(String location, AuthenticationInfo previousInfo);

	/**
	 * Displays a list of certificates to the user.
	 *  
	 * @param certificates - a list of certificates to display to the user
	 * @return An array of certificates that have been accepted.
	 */
	public Certificate[] showCertificates(Certificate[][] certificates);
}
