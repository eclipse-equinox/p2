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
package org.eclipse.equinox.internal.p2.director;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.director.messages"; //$NON-NLS-1$

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}

	public static String Director_Install_Problems;

	public static String Director_Uninstall_Problems;
	public static String Director_Nothing_To_Uninstall;
	public static String Director_Cannot_Uninstall;
	public static String Director_Already_Installed;

	public static String Director_Replace_Problems;
	public static String Director_Become_Problems;
	public static String Director_Unexpected_IU;
	public static String Director_Task_Installing;
	public static String Director_Task_Uninstalling;
	public static String Director_Task_Updating;
	public static String Director_Task_Resolving_Dependencies;
	public static String Director_Resolving_Shared_Dependencies;
	public static String Director_Unsatisfied_Dependencies;
	public static String Director_Unsatisfied_Dependency;

}
