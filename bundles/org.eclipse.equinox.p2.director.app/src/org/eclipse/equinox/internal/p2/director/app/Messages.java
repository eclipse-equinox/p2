/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * 	IBM Corporation - initial API and implementation
 * 	Cloudsmith - https://bugs.eclipse.org/bugs/show_bug.cgi?id=226401
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director.app;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.director.app.messages"; //$NON-NLS-1$

	public static String could_not_remove_initialProfile;

	public static String Deprecated_application;
	public static String Ambigous_Command;
	public static String Application_NoManager;

	public static String Application_NoRepositories;

	public static String ArgRequiresOtherArgs;
	public static String Bad_format;
	public static String Cant_change_roaming;
	public static String destination_commandline;

	public static String Cannot_set_iu_profile_property_iu_does_not_exist;
	public static String File_does_not_exist;

	public static String Help_A_list_of_properties_in_the_form_key_value_pairs;
	public static String Help_A_list_of_URLs_denoting_artifact_repositories;
	public static String Help_A_list_of_URLs_denoting_colocated_repositories;
	public static String Help_A_list_of_URLs_denoting_metadata_repositories;
	public static String Help_Defines_flavor_to_use_for_created_profile;
	public static String Help_Defines_what_profile_to_use_for_the_actions;
	public static String Help_Indicates_that_the_product_can_be_moved;
	public static String Help_Installs_the_listed_IUs;
	public static String Help_lb_lt_path_gt_rb;
	public static String Help_List_all_IUs_found_in_repos;
	public static String Help_List_installed_roots;
	public static String Help_lt_comma_separated_list_gt;

	public static String Help_lt_list_format_gt;
	public static String Help_lb_lt_comma_separated_list_gt_rb;
	public static String Help_lt_name_gt;
	public static String Help_lt_path_gt;
	public static String Help_Missing_argument;
	public static String Help_Only_verify_dont_install;
	public static String Help_path_to_IU_profile_properties_file;
	public static String Help_Prints_this_command_line_help;
	public static String Help_The_ARCH_when_profile_is_created;
	public static String Help_The_folder_in_which_the_targetd_product_is_located;
	public static String Help_The_location_where_the_plugins_and_features_will_be_stored;
	public static String Help_The_NL_when_profile_is_created;
	public static String Help_The_OS_when_profile_is_created;
	public static String Help_The_WS_when_profile_is_created;
	public static String Help_Uninstalls_the_listed_IUs;
	public static String Help_Revert_to_previous_state;
	public static String Help_Use_a_shared_location_for_the_install;
	public static String Help_Purge_the_install_registry;
	public static String Help_Follow_references;
	public static String Help_Defines_a_tag_for_provisioning_session;
	public static String Help_List_Tags;
	public static String Help_Download_Only;

	public static String Help_formats_the_IU_list;

	public static String Ignored_repo;
	public static String Installing;
	public static String Missing_director;
	public static String Missing_Engine;
	public static String Missing_IU;
	public static String Missing_planner;
	public static String Missing_profileid;
	public static String Missing_Required_Argument;
	public static String Missing_profile;

	public static String Operation_complete;
	public static String Operation_failed;
	public static String option_0_requires_an_argument;
	public static String unable_to_parse_0_to_uri_1;
	public static String Uninstalling;
	public static String unknown_option_0;

	public static String problem_CallingDirector;
	public static String Problem_loading_file;
	public static String problem_repoMustBeURI;
	public static String unableToWriteLogFile;
	public static String Unmatched_iu_profile_property_key_value;

	public static String Cant_write_in_destination;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		//empty
	}
}
