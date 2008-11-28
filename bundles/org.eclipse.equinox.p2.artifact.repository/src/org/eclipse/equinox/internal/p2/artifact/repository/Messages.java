/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.artifact.repository.messages"; //$NON-NLS-1$

	public static String artifact_not_found;
	public static String available_already_in;
	public static String downloading;
	public static String error_closing_stream;
	public static String io_failedRead;
	public static String io_incompatibleVersion;
	public static String SignatureVerification_failedRead;
	public static String SignatureVerification_invalidContent;
	public static String SignatureVerification_invalidFileContent;

	public static String SignatureVerifier_OutOfMemory;
	public static String io_parseError;
	public static String mirroring;
	public static String repoMan_internalError;
	public static String repoFailedWrite;
	public static String repoReadOnly;

	public static String sar_downloading;
	public static String sar_downloadJobName;
	public static String sar_failedMkdir;
	public static String sar_reportStatus;

	public static String mirror_alreadyExists;
	public static String message_mirroringStatus;
	public static String exception_comparatorNotFound;
	public static String exception_noComparators;
	public static String exception_destinationNotModifiable;
	public static String exception_needSourceDestination;
	public static String exception_malformedRepoURI;

	public static String Mirroring_NO_MATCHING_DESCRIPTOR;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}

}