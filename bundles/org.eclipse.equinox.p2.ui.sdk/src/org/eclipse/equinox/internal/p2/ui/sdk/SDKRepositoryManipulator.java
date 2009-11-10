/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.p2.ui.ColocatedRepositoryManipulator;

class SDKRepositoryManipulator extends ColocatedRepositoryManipulator {

	public SDKRepositoryManipulator() {
		super(PreferenceConstants.PREF_PAGE_SITES);
	}

	public String getManipulatorLinkLabel() {
		return ProvSDKMessages.ProvSDKUIActivator_SitePrefLink;
	}

}