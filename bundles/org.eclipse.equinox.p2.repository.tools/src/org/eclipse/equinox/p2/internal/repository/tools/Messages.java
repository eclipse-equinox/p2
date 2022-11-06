/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.p2.internal.repository.tools;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.p2.internal.repository.tools.messages"; //$NON-NLS-1$
	public static String exception_destinationNotModifiable;
	public static String exception_unableToRemoveRepo;
	public static String exception_notLocalFileRepo;
	public static String exception_noEngineService;
	public static String exception_noPlannerService;
	public static String exception_needIUsOrNonEmptyRepo;
	public static String exception_needDestinationRepo;
	public static String exception_onlyOneComparator;
	public static String exception_loadingRepository;

	public static String AbstractApplication_no_valid_destinations;

	public static String AbstractRepositoryTask_unableToFind;

	public static String CompositeRepository_composite_repository_exists;
	public static String CompositeRepository_default_artifactRepo_name;
	public static String CompositeRepository_default_metadataRepo_name;

	public static String no_artifactRepo_manager;
	public static String no_metadataRepo_manager;
	public static String no_profile_registry;
	public static String no_provisioning_agent;
	public static String unable_to_process_uri;
	public static String unknown_repository_type;

	public static String MirrorApplication_artifactDestinationNoSource;
	public static String MirrorApplication_metadataDestinationNoSource;
	public static String MirrorApplication_no_IUs;
	public static String MirrorApplication_set_source_repositories;

	public static String ProcessRepo_location_not_url;
	public static String ProcessRepo_must_be_local;

	public static String SlicingOption_invalid_platform;
	public static String exception_invalidDestination;
	public static String exception_invalidSource;
	public static String Repo2RunnableTask_errorTransforming;
	public static String SlicingOption_invalidFilterFormat;

	public static String skippingInvalidFilter;

	public static String message_mirroringStatus;
	public static String mirror_alreadyExists;
	public static String Mirroring_noMatchingDescriptor;
	public static String Mirroring_ValidationError;
	public static String Mirroring_missingDescriptor;
	public static String Mirroring_differentDescriptorProperty;

	public static String invalidComparatorId;

	public static String info_noMD5Infomation;
	public static String info_noMD5InRepository;

	public static String info_noChecksumInfomation;
	public static String info_noChecksumInRepository;
	public static String warning_different_checksum;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}

}
