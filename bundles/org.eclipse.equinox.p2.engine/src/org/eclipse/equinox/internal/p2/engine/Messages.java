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

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.engine.messages"; //$NON-NLS-1$

	public static String Engine_Error_During_Phase;
	public static String Engine_Operation_Canceled_By_User;

	public static String Install_Operand_Description;
	public static String InstallRegistry_Parser_Error_Parsing_Registry;
	public static String InstallRegistry_Parser_Has_Incompatible_Version;

	public static String Phase_Collect_Error;
	public static String Phase_Configure_Error;
	public static String Phase_Configure_Task;
	public static String Phase_Error;
	public static String Phase_Install_Error;
	public static String Phase_Install_Task;
	public static String Phase_Sizing_Error;
	public static String Phase_Unconfigure_Error;
	public static String Phase_Uninstall_Error;

	public static String Profile_Duplicate_Child_Profile_Id;
	public static String Profile_Duplicate_Root_Profile_Id;
	public static String Profile_Not_Named_Self;
	public static String Profile_Null_Profile_Id;

	public static String SimpleProfileRegistry_Cannot_Create_File_Error;
	public static String SimpleProfileRegistry_Parser_Error_Parsing_Registry;
	public static String SimpleProfileRegistry_Parser_Has_Incompatible_Version;
	public static String SimpleProfileRegistry_Persist_To_Non_File_URL_Error;

	public static String TouchpointManager_Attribute_Not_Specified;
	public static String TouchpointManager_Conflicting_Touchpoint_Types;
	public static String TouchpointManager_Exception_Creating_Touchpoint_Extension;
	public static String TouchpointManager_Incorrectly_Named_Extension;
	public static String TouchpointManager_No_Extension_Point;
	public static String TouchpointManager_Null_Creating_Touchpoint_Extension;
	public static String TouchpointManager_Null_Touchpoint_Type_Argument;
	public static String TouchpointManager_Required_Touchpoint_Not_Found;
	public static String TouchpointManager_Touchpoint_Type_Mismatch;

	public static String Uninstall_Operand_Description;
	public static String Update_Operand_Description;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}

}
