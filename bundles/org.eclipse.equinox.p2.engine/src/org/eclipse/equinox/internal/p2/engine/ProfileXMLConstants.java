/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.equinox.internal.p2.persistence.XMLConstants;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 *	Constants defining the structure of the XML for a Profile
 */
public interface ProfileXMLConstants extends XMLConstants {

	// A format version number for profile XML.
	public static final String XML_CURRENT = "0.0.2"; //$NON-NLS-1$
	public static final Version CURRENT_VERSION = new Version(XML_CURRENT);
	public static final String XML_COMPATIBLE = "0.0.1"; //$NON-NLS-1$
	public static final Version COMPATIBLE_VERSION = new Version(XML_CURRENT);
	public static final VersionRange XML_TOLERANCE = new VersionRange(COMPATIBLE_VERSION, true, CURRENT_VERSION, true);

	// Constants for profile elements
	public static final String PROFILES_ELEMENT = "profiles"; //$NON-NLS-1$
	public static final String PROFILE_ELEMENT = "profile"; //$NON-NLS-1$

}
