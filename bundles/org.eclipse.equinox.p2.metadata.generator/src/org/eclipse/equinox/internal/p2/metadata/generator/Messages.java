/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.generator;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.metadata.generator.messages";//$NON-NLS-1$

	public static String exception_errorConverting;
	public static String exception_errorParsingUpdateSite;
	public static String exception_stateAddition;
	public static String exception_sourceDirectoryInvalid;
	public static String exception_artifactRepoNotWritable;
	public static String exception_artifactRepoNotSpecified;
	public static String exception_metadataRepoNotWritable;
	public static String exception_metadataRepoNotSpecified;
	public static String exception_baseLocationNotSpecified;
	public static String exception_artifactRepoNoAppendDestroysInput;

	public static String message_generatingMetadata;
	public static String message_generationCompleted;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}
