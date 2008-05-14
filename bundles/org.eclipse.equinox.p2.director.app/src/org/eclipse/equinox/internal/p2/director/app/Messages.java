/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
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
	public static String destination_commandline;
	public static String Inconsistent_flavor;

	public static String Operation_complete;
	public static String Operation_failed;
	public static String Cant_change_roaming;

	public static String Missing_bundle;
	public static String Missing_director;
	public static String Missing_Engine;
	public static String Missing_IU;
	public static String Missing_planner;

	public static String Listing;
	public static String Ambigous_Command;
	public static String Missing_Required_Argument;

	public static String Installing;
	public static String Uninstalling;

	public static String Ignored_repo;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		//empty
	}
}
