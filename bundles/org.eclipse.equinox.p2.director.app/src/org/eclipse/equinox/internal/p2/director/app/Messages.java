/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director.app;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.director.app.messages"; //$NON-NLS-1$
	public static String destination_commandline;
	public static String Disk_size;
	public static String Download_size;
	public static String Inconsistent_flavor;

	public static String Installation_complete;
	public static String Uninstallation_complete;
	public static String Installation_failed;
	public static String Uninstallation_failed;

	public static String Missing_director;
	public static String Missing_Engine;
	public static String Missing_IU;
	public static String Missing_planner;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
