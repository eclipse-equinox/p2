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
	public static final Version CURRENT_VERSION = new Version(0, 0, 2);
	public static final Version COMPATIBLE_VERSION = new Version(0, 0, 1);
	public static final VersionRange XML_TOLERANCE = new VersionRange(COMPATIBLE_VERSION, true, new Version(2, 0, 0), false);

	// Constants for profile elements

	public static final String PROFILE_ELEMENT = "profile"; //$NON-NLS-1$
	public static final String TIMESTAMP_ATTRIBUTE = "timestamp"; //$NON-NLS-1$
	public static final String IUS_PROPERTIES_ELEMENT = "iusProperties"; //$NON-NLS-1$
	public static final String IU_PROPERTIES_ELEMENT = "iuProperties"; //$NON-NLS-1$
	public static final String PROFILE_TARGET = "profile"; //$NON-NLS-1$
}
