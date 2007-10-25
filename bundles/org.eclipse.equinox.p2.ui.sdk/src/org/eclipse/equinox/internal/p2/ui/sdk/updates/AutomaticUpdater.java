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
package org.eclipse.equinox.internal.p2.ui.sdk.updates;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.equinox.internal.p2.ui.sdk.*;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.equinox.p2.ui.operations.*;
import org.eclipse.equinox.p2.updatechecker.IUpdateListener;
import org.eclipse.equinox.p2.updatechecker.UpdateEvent;
import org.eclipse.ui.PlatformUI;

/**
 * @since 3.4
 */
public class AutomaticUpdater implements IUpdateListener {

	Preferences prefs;

	public AutomaticUpdater() {
		prefs = ProvSDKUIActivator.getDefault().getPluginPreferences();

	}

	public void updatesAvailable(final UpdateEvent event) {
		final boolean download = prefs.getBoolean(PreferenceConstants.P_DOWNLOAD);
		final IInstallableUnit[] toUpdate = (ProvSDKPolicies.getUpdatesToShow(event));
		if (toUpdate.length <= 0)
			return;
		try {
			if (download) {
				IInstallableUnit[] replacements = ProvisioningUtil.updatesFor(toUpdate, null);
				if (replacements.length > 0) {
					final ProvisioningPlan plan = ProvisioningUtil.getPlanner().getReplacePlan(toUpdate, replacements, event.getProfile(), null);
					ProvisioningOperationResult result = ProvisioningOperationRunner.execute(new ProfileModificationOperation(ProvSDKMessages.AutomaticUpdater_AutomaticDownloadOperationName, event.getProfile().getProfileId(), plan, new DownloadPhaseSet(), false), null, null);
					// TODO need to listen to the job and open the popup when download is done
				}
			} else {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						new AutomaticUpdatesPopup(toUpdate, event.getProfile(), download).open();
					}
				});
			}

		} catch (ProvisionException e) {
			ProvUI.handleException(e, null);
		}

	}
}
