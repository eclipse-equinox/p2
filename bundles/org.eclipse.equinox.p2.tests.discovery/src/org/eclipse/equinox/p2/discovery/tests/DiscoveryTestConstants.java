/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
