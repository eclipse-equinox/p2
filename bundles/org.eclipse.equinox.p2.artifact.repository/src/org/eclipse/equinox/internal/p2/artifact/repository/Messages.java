/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
	public static String no_location;
	public static String downloading;
	public static String error_closing_stream;
	public static String io_failedRead;
	public static String io_failedWrite;
	public static String io_incompatibleVersion;
	public static String io_invalidLocation;
	public static String SignatureVerification_failedRead;
	public static String SignatureVerification_invalidContent;
	public static String SignatureVerification_invalidFileContent;

	public static String SignatureVerifier_OutOfMemory;
	public static String io_parseError;
	public static String mirroring;
	public static String repoMan_internalError;
	public static String repoFailedWrite;

	public static String sar_downloading;
	public static String sar_downloadJobName;
	public static String sar_failedMkdir;
	public static String sar_reportStatus;

	public static String mirror_alreadyExists;
	public static String message_artifactsFromChildRepos;
	public static String message_problemReadingArtifact;

	public static String exception_comparatorNotFound;
	public static String exception_noComparators;
	public static String exception_unsupportedAddToComposite;

	public static String exception_unsupportedGetOutputStream;
	public static String exception_unsupportedRemoveFromComposite;

	public static String MirrorLog_Console_Log;
	public static String MirrorLog_Exception_Occurred;

	public static String MirrorRequest_multipleDownloadProblems;
	public static String MirrorRequest_transferFailed;

	public static String exception_unableToCreateParentDir;

	public static String folder_artifact_not_file_repo;

	public static String retryRequest;

	public static String error_copying_local_file;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}

}