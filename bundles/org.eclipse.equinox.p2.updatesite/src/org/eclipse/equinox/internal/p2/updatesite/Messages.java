/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.updatesite;

import org.eclipse.osgi.util.NLS;

/**
 * @since 1.0
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.updatesite.messages"; //$NON-NLS-1$

	public static String ErrorReadingDigest;
	public static String ErrorReadingFeature;
	public static String ErrorReadingBundle;
	public static String ErrorReadingSite;
	public static String Error_generating_category;
	public static String Error_generating_siteXML;
	public static String Error_Generation;

	public static String DefaultFeatureParser_IdOrVersionInvalid;
	public static String DefaultSiteParser_NoSiteTag;
	public static String DefaultSiteParser_WrongParsingStack;
	public static String DefaultSiteParser_UnknownElement;
	public static String DefaultSiteParser_UnknownStartState;
	public static String DefaultSiteParser_Missing;
	public static String DefaultSiteParser_ParsingStackBackToInitialState;
	public static String DefaultSiteParser_ElementAlreadySet;
	public static String DefaultSiteParser_UnknownEndState;
	public static String DefaultSiteParser_ErrorParsing;
	public static String DefaultSiteParser_ErrorlineColumnMessage;
	public static String DefaultSiteParser_ErrorParsingSite;
	public static String DefaultSiteParser_UnknownState;
	public static String DefaultSiteParser_InvalidXMLStream;
	public static String DefaultSiteParser_mirrors;

	public static String Unexpected_exception;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// prevent instantiation
	}
}
