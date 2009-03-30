/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.engine.phases;

import org.eclipse.osgi.util.NLS;

//TODO Shouldn't have messages class in API package
class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.provisional.p2.engine.phases.messages"; //$NON-NLS-1$
	public static String Phase_Collect_Error;
	public static String Phase_Install_Error;
	public static String Phase_Configure_Error;
	public static String Phase_Configure_Task;
	public static String Phase_Install_Task;
	public static String Phase_Sizing_Error;
	public static String Phase_Sizing_Warning;
	public static String Phase_Unconfigure_Error;
	public static String Phase_Uninstall_Error;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}

}
