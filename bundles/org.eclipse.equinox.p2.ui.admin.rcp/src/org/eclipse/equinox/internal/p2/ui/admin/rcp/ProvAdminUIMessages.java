/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.admin.rcp;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for provisioning UI messages.  
 * 
 * @since 3.4
 */
public class ProvAdminUIMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.ui.admin.rcp.messages"; //$NON-NLS-1$
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, ProvAdminUIMessages.class);
	}
	public static String ApplicationActionBarAdvisor_FileMenuName;
	public static String ApplicationActionBarAdvisor_HelpMenuName;
	public static String ApplicationActionBarAdvisor_WindowMenuName;
	public static String ApplicationWindowTitle;
}
