/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.generator.features;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.metadata.generator.features.messages";//$NON-NLS-1$

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
	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}
}
