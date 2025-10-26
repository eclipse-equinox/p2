/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse.bundledescription;

import org.eclipse.osgi.util.NLS;

public class StateMsg extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.publisher.eclipse.bundledescription.StateMessages"; //$NON-NLS-1$

	public static String HEADER_REQUIRED;
	public static String HEADER_PACKAGE_DUPLICATES;
	public static String HEADER_PACKAGE_JAVA;
	public static String HEADER_VERSION_ERROR;
	public static String HEADER_EXPORT_ATTR_ERROR;
	public static String HEADER_DIRECTIVE_DUPLICATES;
	public static String HEADER_ATTRIBUTE_DUPLICATES;
	public static String HEADER_EXTENSION_ERROR;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, StateMsg.class);
	}

	public static String MANIFEST_INVALID_HEADER_EXCEPTION;
}
