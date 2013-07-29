/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ericsson AB - initial API and implementation
 *     Ericsson AB (Pascal Rapicault)
 *     Ericsson AB (Hamdan Msheik) 
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler.migration;

import org.eclipse.osgi.util.NLS;

public class ProvUIMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.ui.sdk.scheduler.migration.messages"; //$NON-NLS-1$
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, ProvUIMessages.class);
	}

	public static String AbstractImportPage_HigherVersionInstalled;
	public static String AbstractImportPage_SameVersionInstalled;
	public static String Column_Id;
	public static String Column_Name;
	public static String Column_Version;
	public static String AbstractPage_ButtonSelectAll;
	public static String AbstractPage_ButtonDeselectAll;
	public static String PAGE_NOINSTALLTION_ERROR;
	public static String MigrationWizard_WINDOWTITLE;
	public static String MigrationWizard_WINDOWTITLE_FIRSTRUN;
	public static String MigrationPage_SELECT_COMPONENT;
	public static String MigrationPage_DIALOG_TITLE;
	public static String MigrationPage_DIALOG_TITLE_FIRSTRUN;
	public static String MigrationPage_DIALOG_DESCRIPTION;
	public static String MigrationPage_DIALOG_DESCRIPTION_FIRSTRUN;
	public static String MigrationPage_CONFIRMATION_TITLE;
	public static String MigrationPage_CONFIRMATION_DIALOG;
	public static String MigrationPage_LATER_BUTTON;
	public static String MigrationPage_UPDATE_TO_LATEST;

}
