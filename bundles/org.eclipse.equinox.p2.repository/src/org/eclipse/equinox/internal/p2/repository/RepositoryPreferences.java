/*******************************************************************************
 * Copyright (c) 2006-2009, Cloudsmith Inc.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text or
 * such license is available at www.eclipse.org.
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.repository;

/**
 * Holds various preferences for repository.
 * TODO: if values should be configurable, they need to be persisted and read back.
 *
 */
public class RepositoryPreferences {

	/**
	 * Number of attempts to connect (with same credentials) before giving up.
	 * Note that newer ECF using apache HTTPclient has retry by default.
	 * TODO - make this configurable via a property.
	 * TODO - consider removing this option
	 * @return the value 1
	 */
	public static int getConnectionRetryCount() {
		return 1;
	}

	/**
	 * Reconnect delay after failure to connect (with same credentials)- in milliseconds.
	 * Current implementation returns 200ms.
	 * TODO - make this configurable via a property
	 * @return the value 200
	 */
	public static long getConnectionMsRetryDelay() {
		return 200;
	}

	/**
	 * Number of attempts to connect (with different credentials) before giving up.
	 * The returned value should be the number of prompts to the user + 1 (for the initial
	 * attempt with either no, or stored/cached credentials).
	 * TODO - make this configurable via a property.
	 * @return the value 4 - resulting in 3 prompts to user
	 */
	public static int getLoginRetryCount() {
		return 4;
	}
}
