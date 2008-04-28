/**********************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.equinox;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.frameworkadmin.equinox.messages";//$NON-NLS-1$

	public static String exception_inputFileIsDirectory;
	public static String exception_fwConfigLocationName;
	public static String exception_failedToCreateDir;
	public static String exception_failedToRename;

	public static String log_configFile;
	public static String log_configProps;
	public static String log_renameSuccessful;
	public static String log_fwConfigSave;

	public static String msg_ConfigFileHeader;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}