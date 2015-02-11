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
 *    SAP AG - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.publisher.eclipse.messages";//$NON-NLS-1$

	// exception
	public static String exception_missingElement;
	public static String exception_featureParse;
	public static String exception_productParse;
	public static String exception_invalidProductContentType;
	public static String exception_invalidFeatureInstallMode;

	// feature parsing
	public static String feature_parse_invalidIdOrVersion;
	public static String feature_parse_emptyRequires;

	public static String featuresInProductFileIgnored;
	public static String bundlesInProductFileIgnored;

	public static String message_problemPublishingProduct;
	public static String message_cannotDetermineFilterOnInclusion;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}