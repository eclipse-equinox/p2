/*******************************************************************************
 *  Copyright (c) 20011 SAP AG.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.console;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.console.messages"; //$NON-NLS-1$

	private Messages() {
		// do not instantiate
	}

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String Console_help_header;
	public static String Console_help_repository_header;
	public static String Console_help_provaddrepo_description;
	public static String Console_help_provdelrepo_description;
	public static String Console_help_provaddmetadatarepo_description;
	public static String Console_help_provdelmetadatarepo_description;
	public static String Console_help_provaddartifactrepo_description;
	public static String Console_help_provdelartifactrepo_description;
	public static String Console_help_provlg_description;
	public static String Console_help_provlr_description;
	public static String Console_help_provlar_description;
	public static String Console_help_provliu_description;
	public static String Console_help_provlquery_description;
	public static String Console_help_profile_registry_header;
	public static String Console_help_provaddprofile_description;
	public static String Console_help_provdelprofile_description;
	public static String Console_help_provlp_description;
	public static String Console_help_provlgp_description;
	public static String Console_help_provlpts_description;
	public static String Console_help_provlpquery_description;
	public static String Console_help_install_header;
	public static String Console_help_provinstall_description;
	public static String Console_help_provremove_description;
	public static String Console_help_provrevert_description;
}
