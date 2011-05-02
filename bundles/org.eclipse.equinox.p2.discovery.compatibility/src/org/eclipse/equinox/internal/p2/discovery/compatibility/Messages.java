/*******************************************************************************
 * Copyright (c) 2009, 2011 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Sonatype, Inc. - added caching support
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.discovery.compatibility;

import org.eclipse.osgi.util.NLS;

/**
 * @author David Green
 */
public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.discovery.compatibility.messages"; //$NON-NLS-1$

	public static String BundleDiscoveryStrategy_3;

	public static String BundleDiscoveryStrategy_categoryDisallowed;

	public static String BundleDiscoveryStrategy_task_loading_local_extensions;

	public static String BundleDiscoveryStrategy_task_processing_extensions;

	public static String BundleDiscoveryStrategy_unexpected_element;

	public static String CacheManager_AuthenticationFaileFor_0;

	public static String CacheManager_FailedCommunication_0;

	public static String CacheManager_Neither_0_nor_1_found;

	public static String CacheManage_ErrorRenamingCache;

	public static String ConnectorDiscoveryExtensionReader_Documents;

	public static String ConnectorDiscoveryExtensionReader_Tasks;

	public static String ConnectorDiscoveryExtensionReader_unexpected_element_icon;

	public static String ConnectorDiscoveryExtensionReader_unexpected_element_overview;

	public static String ConnectorDiscoveryExtensionReader_unexpected_value_kind;

	public static String ConnectorDiscoveryExtensionReader_Version_Control;

	public static String DirectoryParser_no_directory;

	public static String DirectoryParser_unexpected_element;

	public static String DiscoveryRegistryStrategy_cannot_load_bundle;

	public static String DiscoveryRegistryStrategy_missing_pluginxml;

	public static String RemoteBundleDiscoveryStrategy_cannot_download_bundle;

	public static String RemoteBundleDiscoveryStrategy_empty_directory;

	public static String RemoteBundleDiscoveryStrategy_Invalid_source_specified_Error;

	public static String RemoteBundleDiscoveryStrategy_io_failure_discovery_directory;

	public static String RemoteBundleDiscoveryStrategy_io_failure_temp_storage;

	public static String RemoteBundleDiscoveryStrategy_task_remote_discovery;

	public static String RemoteBundleDiscoveryStrategy_unexpectedError;

	public static String RemoteBundleDiscoveryStrategy_unknown_host_discovery_directory;

	public static String RemoteBundleDiscoveryStrategy_unrecognized_discovery_url;

	public static String SiteVerifier_Error_with_cause;

	public static String SiteVerifier_Unexpected_Error;

	public static String SiteVerifier_Verify_Job_Label;

	public static String TransportUtil_InternalError;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// constructor
	}

}
