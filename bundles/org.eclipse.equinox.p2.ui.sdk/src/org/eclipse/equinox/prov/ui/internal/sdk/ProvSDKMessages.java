/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.prov.ui.internal.sdk;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for provisioning UI messages.  
 * 
 * @since 3.4
 */
public class ProvSDKMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.prov.ui.internal.sdk.messages"; //$NON-NLS-1$
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, ProvSDKMessages.class);
	}
	public static String RepositoryManipulationDialog_UpdateSitesDialogTitle;
	public static String UpdateAndInstallDialog_AvailableFeatures;
	public static String UpdateAndInstallDialog_InstalledFeatures;
	public static String UpdateAndInstallDialog_ManageSites;
	public static String UpdateAndInstallDialog_Title;
	public static String UpdateHandler_NoProfilesDefined;
	public static String UpdateHandler_NoProfileInstanceDefined;
	public static String UpdateHandler_SDKUpdateUIMessageTitle;
}
