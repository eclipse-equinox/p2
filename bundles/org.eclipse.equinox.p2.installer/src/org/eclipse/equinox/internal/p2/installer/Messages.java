/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.installer;

import org.eclipse.osgi.util.NLS;

/**
 * 
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.installer.messages"; //$NON-NLS-1$
	public static String Advisor_Canceled;
	public static String Advisor_Preparing;
	public static String App_Error;
	public static String App_FailedStart;
	public static String App_InvalidSite;
	public static String App_LaunchFailed;
	public static String App_Launching;
	public static String App_NoInstallLocation;
	public static String App_NoSite;
	public static String Dialog_BrowseButton;
	public static String Dialog_CancelButton;
	public static String Dialog_CloseButton;
	public static String Dialog_ExplainShared;
	public static String Dialog_ExplainStandalone;
	public static String Dialog_InstallButton;
	public static String Dialog_InstalllingProgress;
	public static String Dialog_InternalError;
	public static String Dialog_LaunchButton;
	public static String Dialog_LayoutGroup;
	public static String Dialog_LocationField;
	public static String Dialog_LocationLabel;
	public static String Dialog_LocationPrompt;
	public static String Dialog_PromptStart;
	public static String Dialog_SelectLocation;
	public static String Dialog_SharedButton;
	public static String Dialog_ShellTitle;
	public static String Dialog_StandaloneButton;
	public static String Op_Cleanup;
	public static String Op_InstallComplete;
	public static String Op_Installing;
	public static String Op_IUNotFound;
	public static String Op_NoId;
	public static String Op_NoService;
	public static String Op_NoServiceImpl;
	public static String Op_Preparing;
	public static String Op_UpdateComplete;
	public static String Op_Updating;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		//nothing to do
	}
}
