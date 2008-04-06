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
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.messages"; //$NON-NLS-1$
	public static String artifact_file_not_found;
	public static String action_not_instantiated;
	public static String parameter_not_set;
	public static String iu_contains_no_arifacts;
	public static String no_matching_artifact;
	public static String missing_manifest;
	public static String failed_bundleinfo;
	public static String cannot_configure_source_bundle;
	public static String error_parsing_startlevel;

	static {
		// load message values from bundle file and assign to fields below
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}
