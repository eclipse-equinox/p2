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
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.osgi.util.NLS;

class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.engine.messages"; //$NON-NLS-1$

	public static String ActionManager_Exception_Creating_Action_Extension;
	public static String ActionManager_Required_Touchpoint_Not_Found;

	public static String action_not_found;

	public static String download_artifact;
	public static String download_no_repository;

	public static String error_parsing_profile;

	public static String error_persisting_profile;

	public static String failed_creating_metadata_cache;

	public static String ParameterizedProvisioningAction_action_or_parameters_null;

	public static String profile_does_not_exist;

	public static String profile_not_current;

	public static String profile_not_registered;

	public static String Profile_Duplicate_Root_Profile_Id;
	public static String Profile_Null_Profile_Id;
	public static String Profile_Parent_Not_Found;

	public static String reg_dir_not_available;

	public static String SimpleProfileRegistry_Parser_Error_Parsing_Registry;
	public static String SimpleProfileRegistry_Parser_Has_Incompatible_Version;

	public static String SimpleProfileRegistry_Profile_in_use;
	public static String SimpleProfileRegistry_Profile_not_locked;
	public static String SimpleProfileRegistry_Profile_not_locked_due_to_exception;
	public static String SimpleProfileRegistry_Bad_profile_location;

	public static String thread_not_owner;

	public static String TouchpointManager_Attribute_Not_Specified;
	public static String TouchpointManager_Conflicting_Touchpoint_Types;
	public static String TouchpointManager_Exception_Creating_Touchpoint_Extension;
	public static String TouchpointManager_Incorrectly_Named_Extension;
	public static String TouchpointManager_Null_Creating_Touchpoint_Extension;
	public static String TouchpointManager_Null_Touchpoint_Type_Argument;
	public static String shared_profile_not_found;
	public static String action_syntax_error;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}

}
