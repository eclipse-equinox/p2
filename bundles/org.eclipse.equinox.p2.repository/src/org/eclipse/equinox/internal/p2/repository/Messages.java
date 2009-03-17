/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc - additional messages
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.repository.messages"; //$NON-NLS-1$

	public static String artifact_not_found;
	public static String available_already_in;
	public static String downloading;
	public static String error_closing_stream;
	public static String io_failedRead;
	public static String ecf_configuration_error;
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
	public static String repoReadOnly;
	public static String repo_loading;

	public static String sar_downloading;
	public static String sar_downloadJobName;
	public static String sar_failedMkdir;
	public static String sar_reportStatus;

	public static String mirror_alreadyExists;
	public static String message_mirroringStatus;
	public static String message_childrenRepos;
	public static String exception_comparatorNotFound;
	public static String exception_noComparators;
	public static String exception_destinationNotModifiable;
	public static String exception_needSourceDestination;
	public static String exception_malformedRepoURI;
	public static String exception_unsupportedAddToComposite;

	public static String exception_unsupportedGetOutputStream;
	public static String exception_unsupportedRemoveFromComposite;

	public static String Mirroring_NO_MATCHING_DESCRIPTOR;

	public static String TransportErrorTranslator_400;
	public static String TransportErrorTranslator_401;
	public static String TransportErrorTranslator_402;
	public static String TransportErrorTranslator_403;
	public static String TransportErrorTranslator_404;
	public static String TransportErrorTranslator_405;
	public static String TransportErrorTranslator_406;
	public static String TransportErrorTranslator_407;
	public static String TransportErrorTranslator_408;
	public static String TransportErrorTranslator_409;
	public static String TransportErrorTranslator_410;
	public static String TransportErrorTranslator_411;
	public static String TransportErrorTranslator_412;
	public static String TransportErrorTranslator_413;
	public static String TransportErrorTranslator_414;
	public static String TransportErrorTranslator_415;
	public static String TransportErrorTranslator_416;
	public static String TransportErrorTranslator_417;
	public static String TransportErrorTranslator_418;
	public static String TransportErrorTranslator_422;
	public static String TransportErrorTranslator_423;
	public static String TransportErrorTranslator_424;
	public static String TransportErrorTranslator_425;
	public static String TransportErrorTranslator_426;
	public static String TransportErrorTranslator_449;
	public static String TransportErrorTranslator_450;
	public static String TransportErrorTranslator_500;
	public static String TransportErrorTranslator_501;
	public static String TransportErrorTranslator_502;
	public static String TransportErrorTranslator_503;
	public static String TransportErrorTranslator_504;
	public static String TransportErrorTranslator_505;
	public static String TransportErrorTranslator_506;
	public static String TransportErrorTranslator_507;
	public static String TransportErrorTranslator_508;
	public static String TransportErrorTranslator_510;
	public static String TransportErrorTranslator_MalformedRemoteFileReference;
	public static String TransportErrorTranslator_UnableToConnectToRepository_0;

	public static String TransportErrorTranslator_UnknownErrorCode;
	public static String TransportErrorTranslator_UnknownHost;

	public static String fetching_0_1_at_2;
	public static String fetching_0_1_of_2_at_3;
	public static String connection_to_0_failed_on_1_retry_attempt_2;

	public static String FileTransport_reader;

	public static String UnableToRead_0_TooManyAttempts;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}

}