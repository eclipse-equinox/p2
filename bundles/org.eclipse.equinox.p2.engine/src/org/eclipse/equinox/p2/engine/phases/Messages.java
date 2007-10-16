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
package org.eclipse.equinox.p2.engine.phases;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.p2.engine.phases.messages"; //$NON-NLS-1$

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}

	public static String Engine_Collect_Phase;
	public static String Engine_Collecting_For_IU;
	public static String Engine_Install_Phase;
	public static String Engine_Installing_IU;
	public static String Engine_Uninstall_Phase;
	public static String Engine_Uninstalling_IU;
	public static String Engine_Configure_Phase;
	public static String Engine_Unconfigure_Phase;
}
