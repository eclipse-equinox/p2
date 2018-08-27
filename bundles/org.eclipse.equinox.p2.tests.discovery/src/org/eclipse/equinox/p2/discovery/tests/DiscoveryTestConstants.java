/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.discovery.tests;

/**
 * @author David Green
 */
public abstract class DiscoveryTestConstants {

	/**
	 * The URL of the mylyn discovery directory, may be overridden using the system property
	 * <tt>mylyn.discovery.directory</tt>.
	 */
	public static final String DISCOVERY_URL = System.getProperty("mylyn.discovery.directory", "http://www.eclipse.org/mylyn/discovery/directory-3.3.xml"); //$NON-NLS-1$ //$NON-NLS-2$

	private DiscoveryTestConstants() {
		// don't allow clients to instantiate
	}

}
