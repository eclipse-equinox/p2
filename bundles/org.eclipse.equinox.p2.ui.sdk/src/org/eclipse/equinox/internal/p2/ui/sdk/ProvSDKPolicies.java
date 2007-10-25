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
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.util.ArrayList;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitConstants;
import org.eclipse.equinox.p2.ui.viewers.IUGroupFilter;
import org.eclipse.equinox.p2.ui.viewers.IUProfilePropertyFilter;
import org.eclipse.equinox.p2.updatechecker.UpdateEvent;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Implements policy-style decisions about what is shown in the UI.
 * 
 * @since 3.4
 */

public class ProvSDKPolicies extends AbstractUIPlugin {

	public static ViewerFilter getInstalledIUFilter() {
		return new IUProfilePropertyFilter(IInstallableUnitConstants.PROFILE_ROOT_IU, Boolean.toString(true));
	}

	public static ViewerFilter getAvailableIUFilter() {
		return new IUGroupFilter();
	}

	public static IInstallableUnit[] getUpdatesToShow(UpdateEvent event) {
		// TODO hardcoded to only show install roots, there is probably
		// more policy here.
		IInstallableUnit[] ius = event.getIUs();
		ArrayList roots = new ArrayList();
		for (int i = 0; i < ius.length; i++) {
			String value = event.getProfile().getInstallableUnitProfileProperty(ius[i], IInstallableUnitConstants.PROFILE_ROOT_IU);
			if (value != null && value.equals(Boolean.toString(true))) {
				roots.add(ius[i]);
			}
		}
		return (IInstallableUnit[]) roots.toArray(new IInstallableUnit[roots.size()]);
	}
}
