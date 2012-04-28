/*******************************************************************************
 *  Copyright (c) 2008, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Pascal Rapicault - Support for bundled macosx http://bugs.eclipse.org/57349
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.touchpoint.eclipse.messages"; //$NON-NLS-1$
	public static String error_loading_manipulator;
	public static String BundlePool;
	public static String failed_acquire_framework_manipulator;
	public static String failed_prepareIU;
	public static String error_saving_manipulator;
	public static String error_saving_platform_configuration;
	public static String error_saving_source_bundles_list;
	public static String error_parsing_configuration;
	public static String publisher_not_available;
	public static String artifact_write_unsupported;
	public static String iu_contains_no_arifacts;
	public static String artifact_file_not_found;
	public static String artifact_retrieval_unsupported;
	public static String bundle_pool_not_writeable;
	public static String cannot_calculate_extension_location;
	public static String parent_dir_features;
	public static String platform_config_unavailable;
	public static String unexpected_prepareiu_error;
	public static String error_validating_profile;
	public static String invalid_macox_bundled_setup;

	static {
		// load message values from bundle file and assign to fields below
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}
