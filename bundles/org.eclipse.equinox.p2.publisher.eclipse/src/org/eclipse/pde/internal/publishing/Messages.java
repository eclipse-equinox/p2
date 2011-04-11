/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM - Initial API and implementation
 *    SAP AG - consolidation of publishers for PDE formats
 *******************************************************************************/
package org.eclipse.pde.internal.publishing;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.publisher.eclipse.messages";//$NON-NLS-1$

	// exception
	public static String exception_missingElement;
	public static String exception_featureParse;
	public static String exception_productParse;

	// feature parsing
	public static String feature_parse_invalidIdOrVersion;
	public static String feature_parse_emptyRequires;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}