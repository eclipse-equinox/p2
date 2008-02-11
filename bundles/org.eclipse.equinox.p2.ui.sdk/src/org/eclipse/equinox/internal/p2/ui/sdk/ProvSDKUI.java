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
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.equinox.internal.provisional.p2.ui.LicenseManager;
import org.eclipse.equinox.internal.provisional.p2.ui.query.IQueryProvider;

/**
 * ProvSDKUI provides API for accessing the configurable p2 UI components 
 * specified when creating UI classes.
 */
public class ProvSDKUI {

	public static IQueryProvider getQueryProvider() {
		return ProvSDKUIActivator.getDefault().getQueryProvider();
	}

	public static LicenseManager getLicenseManager() {
		return ProvSDKUIActivator.getDefault().getLicenseManager();
	}
}
