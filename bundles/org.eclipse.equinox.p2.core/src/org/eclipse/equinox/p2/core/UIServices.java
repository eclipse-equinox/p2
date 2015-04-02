/*******************************************************************************
 *  Copyright (c) 2008, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rapicorp, Inc. - add support for information dialog
 *******************************************************************************/
package org.eclipse.equinox.p2.core;

import java.security.cert.Certificate;

/**
 * Service used for prompting for user information from within lower level code.
 * Implementors of this service are responsible for registering the service.
 * 
 * It is possible that the UIServices service is requested very early in the startup
 * sequence for an application.  For example, applications that check for updates 
 * during startup will trigger the service lookup if a server requiring authentication
 * is detected.  For this reason, implementors of UIServices should ensure that the 
 * bundle providing the service is partitioned appropriately.
 * 
 * @since 2.0
 */
public abstract class UIServices {
	/**
	 * Service name constant for the UI service.
	 */
	public static final String SERVICE_NAME = UIServices.class.getName();

	/**
	 * This constant may be returned by the <code>getUsernamePassword</code> methods if the user 
	 * explicitly canceled the authentication prompt.
	 * 
	 * @since 2.2
	 */
	public static final AuthenticationInfo AUTHENTICATION_PROMPT_CANCELED = new AuthenticationInfo("", "", false); //$NON-NLS-1$//$NON-NLS-2$

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
	 * Trust information returned from a trust request.	 *
	 */
	public static class TrustInfo {
		private final Certificate[] trustedCertificates;
		private final boolean saveTrustedCertificates;
		private final boolean trustUnsigned;

		public TrustInfo(Certificate[] trusted, boolean save, boolean trustUnsigned) {
			this.trustedCertificates = trusted;
			this.saveTrustedCertificates = save;
			this.trustUnsigned = trustUnsigned;
		}

		/**
		 * Return an array of the certificates that should be trusted for the
		 * requested operation.
		 * 
		 * @return the trusted certificates, or <code>null</code> if there are
		 * no certificates that were verified as trusted.
		 */
		public Certificate[] getTrustedCertificates() {
			return trustedCertificates;
		}

		/**
		 * Return a boolean indicating whether the trusted certificates should
		 * be persisted for future operations.
		 * 
		 * @return <code>true</code> if the trusted certificates should be persisted, <code>false</code> if 
		 * the trust only applies for this request.
		 */
		public boolean persistTrust() {
			return saveTrustedCertificates;
		}

		/**
		 * Return a boolean indicating whether the unsigned content should be trusted
		 * during this operation.
		 * 
		 * @return <code>true</code> if the unsigned content should be trusted, or if there was no unsigned content, 
		 * and <code>false</code> if there was unsigned content and should not be trusted.
		 */
		public boolean trustUnsignedContent() {
			return trustUnsigned;
		}
	}

	/**
	 * Opens a UI prompt for authentication details
	 * 
	 * @param location - the location requiring login details, may be <code>null</code>.
	 * @return The authentication result, or <code>null</code>, or {@link #AUTHENTICATION_PROMPT_CANCELED}
	 */
	public abstract AuthenticationInfo getUsernamePassword(String location);

	/**
	 * Opens a UI prompt for authentication details when cached or remembered details
	 * where not accepted.
	 * 
	 * @param location  the location requiring login details
	 * @param previousInfo - the previously used authentication details - may not be null.
	 * @return The authentication result, or <code>null</code>, or {@link #AUTHENTICATION_PROMPT_CANCELED}
	 */
	public abstract AuthenticationInfo getUsernamePassword(String location, AuthenticationInfo previousInfo);

	/**
	 * Opens a UI prompt to capture information about trusted content.
	 *  
	 * @param untrustedChain - an array of certificate chains for which there is no current trust anchor.  May be
	 * <code>null</code>, which means there are no untrusted certificate chains.
	 * @param unsignedDetail - an array of strings, where each String describes content that is not signed.
	 * May be <code>null</code>, which means there is no unsigned content
	 * @return  the TrustInfo that describes the user's choices for trusting certificates and
	 * unsigned content. 
	 */
	public abstract TrustInfo getTrustInfo(Certificate[][] untrustedChain, String[] unsignedDetail);

	/**
	 * Shows the given message to the user.  It depends on the concrete implementation how this is done,
	 * e.g. in a dialog, on the console, or in other ways.
	 *
	 * @param title - a title if the message is shown in a dialog
	 * @param text - the message to be shown
	 * @param linkText - an optional text to be rendered as hyperlink on the UI
	 *
	 * @since 2.4
	 */
	public void showInformationMessage(String title, String text, String linkText) {
		System.out.println(text);
	}
}
