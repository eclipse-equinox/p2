/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.equinox.internal.p2.ui.actions;

import java.util.Collection;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.model.CategoryElement;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.DisableIUOperation;
import org.eclipse.equinox.p2.operations.EnableIUOperation;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * An action that re-enables previously disabled IInstallableUnits by restoring
 * {@link IProfile#PROP_PROFILE_ROOT_IU}. Only enables when the selection is an
 * IU that is present in the profile but not currently a root (i.e. disabled).
 *
 * @since 3.10
 */
public class EnableAction extends ProfileModificationAction {

	public EnableAction(ProvisioningUI ui, ISelectionProvider selectionProvider, String profileId) {
		super(ui, ProvUI.ENABLE_COMMAND_LABEL, selectionProvider, profileId);
		setToolTipText(ProvUI.ENABLE_COMMAND_TOOLTIP);
	}

	@Override
	protected boolean isEnabledFor(Object[] selectionArray) {
		if (selectionArray.length == 0) {
			return false;
		}
		IProfile profile = getProfile();
		if (profile == null) {
			return false;
		}
		for (Object selection : selectionArray) {
			if (selection instanceof InstalledIUElement element) {
				IInstallableUnit iu = element.getIU();
				if (iu == null) {
					return false;
				}
				// Enable only when the IU is explicitly tagged as disabled.
				String disabled = profile.getInstallableUnitProperty(iu,
						org.eclipse.equinox.p2.operations.DisableIUOperation.PROP_DISABLED);
				if (!Boolean.TRUE.toString().equals(disabled)) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	@Override
	protected boolean isSelectable(IIUElement element) {
		return !(element instanceof CategoryElement);
	}

	@Override
	protected ProfileChangeOperation getProfileChangeOperation(Collection<IInstallableUnit> ius) {
		return new EnableIUOperation(ui.getSession(), ius);
	}

	@Override
	protected int performAction(ProfileChangeOperation operation, Collection<IInstallableUnit> ius) {
		ProvisioningJob job = operation.getProvisioningJob(null);
		if (job == null) {
			// Resolution failed — the operation's status already contains the error.
			org.eclipse.ui.statushandlers.StatusManager.getManager().handle(
					operation.getResolutionResult(),
					org.eclipse.ui.statushandlers.StatusManager.SHOW | org.eclipse.ui.statushandlers.StatusManager.LOG);
			return Window.CANCEL;
		}
		// Enable also requires restart
		if (job instanceof org.eclipse.equinox.p2.operations.ProfileModificationJob profileModificationJob) {
			profileModificationJob.setRestartPolicy(ProvisioningJob.RESTART_ONLY);
		}
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					// Write bundles.info after the p2 profile is committed.
					DisableIUOperation.updateBundlesInfo(ius, true,
							ui.getSession(), ui.getProfileId());
				}
			}
		});
		ui.schedule(job, StatusManager.SHOW | StatusManager.LOG);
		return Window.OK;
	}
}
