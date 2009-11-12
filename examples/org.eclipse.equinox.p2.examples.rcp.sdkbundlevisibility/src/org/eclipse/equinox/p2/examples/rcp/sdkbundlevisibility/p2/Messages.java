/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.examples.rcp.sdkbundlevisibility.p2;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.p2.examples.rcp.sdkbundlevisibility.p2.messages"; //$NON-NLS-1$
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	public static String UpdateHandler_NoSitesMessage;
	public static String UpdateHandler_NoSitesTitle;
	public static String ProvisioningPreferencePage_AlwaysOpenWizard;
	public static String ProvisioningPreferencePage_BrowsingPrefsGroup;
	public static String ProvisioningPreferencePage_ShowLatestVersions;
	public static String ProvisioningPreferencePage_ShowAllVersions;
	public static String ProvisioningPreferencePage_NeverOpenWizard;
	public static String ProvisioningPreferencePage_OpenWizardIfInvalid;
	public static String ProvisioningPreferencePage_PromptToOpenWizard;

}
