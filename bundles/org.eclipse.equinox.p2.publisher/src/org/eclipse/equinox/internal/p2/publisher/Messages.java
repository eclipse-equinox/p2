/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM Corporation - initial API and implementation
 *   SAP AG - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.publisher.messages";//$NON-NLS-1$

	public static String exception_errorConverting;
	public static String exception_stateAddition;
	public static String exception_errorReadingManifest;
	public static String exception_errorLoadingManifest;
	public static String exception_errorPublishingBundle;
	public static String exception_errorLoadingProductFile;
	public static String exception_noPluginConverter;
	public static String exception_noArtifactRepo;
	public static String exception_noMetadataRepo;
	public static String exception_noBundlesOrLocations;
	public static String exception_noFeaturesOrLocations;
	public static String exception_invalidSiteReference;
	public static String exception_invalidSiteReferenceInFeature;
	public static String exception_repoMustBeURL;
	public static String exception_sourcePath;
	public static String exception_nonExistingJreLocationFile;

	public static String message_bundlesPublisherMultistatus;
	public static String message_generatingMetadata;
	public static String message_generationCompleted;
	public static String message_noSimpleconfigurator;
	public static String message_resultException;
	public static String message_publisherArguments;

	public static String message_problemsWhilePublishingEE;
	public static String message_problemsWhileParsingProfileProperty;
	public static String message_eeIgnoringNamespace;
	public static String message_eeInvalidVersionAttribute;
	public static String message_eeMissingNameAttribute;
	public static String message_eeMissingVersionAttribute;
	public static String message_eeDuplicateVersionAttribute;

	public static String exception_artifactRepoNoAppendDestroysInput;

	public static String error_rootIU_generation;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}
