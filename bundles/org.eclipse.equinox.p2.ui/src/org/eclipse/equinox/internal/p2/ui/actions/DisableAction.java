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
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.CategoryElement;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.DisableIUOperation;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * An action that disables selected IInstallableUnits. Disabling removes the
 * IU's {@link IProfile#PROP_PROFILE_ROOT_IU} property so the resolver no longer
 * treats it as an active root. Files remain on disk; re-enable with
 * {@link EnableAction} at any time.
 *
 * @since 3.10
 */
public class DisableAction extends ProfileModificationAction {

	public DisableAction(ProvisioningUI ui, ISelectionProvider selectionProvider, String profileId) {
		super(ui, ProvUI.DISABLE_COMMAND_LABEL, selectionProvider, profileId);
		setToolTipText(ProvUI.DISABLE_COMMAND_TOOLTIP);
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
		for (Object sel : selectionArray) {
			if (!(sel instanceof InstalledIUElement element)) {
				return false;
			}
			IInstallableUnit iu = element.getIU();
			if (iu == null) {
				return false;
			}
			// Must be in the profile at all
			if (profile.query(org.eclipse.equinox.p2.query.QueryUtil.createIUQuery(iu), null).isEmpty()) {
				return false;
			}
			// Must not be locked against uninstall
			int lock = getLock(profile, iu);
			if ((lock & IProfile.LOCK_UNINSTALL) == IProfile.LOCK_UNINSTALL) {
				return false;
			}
			// Must not already be tagged as disabled (button becomes Enable in that case)
			if (Boolean.TRUE.toString().equals(
					profile.getInstallableUnitProperty(iu, DisableIUOperation.PROP_DISABLED))) {
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
		return new DisableIUOperation(ui.getSession(), ius);
	}

	@Override
	protected int performAction(ProfileChangeOperation operation, Collection<IInstallableUnit> ius) {
		Shell shell = getShell();
		boolean confirmed = MessageDialog.openConfirm(shell,
				ProvUIMessages.DisableIUCommandLabel,
				ProvUIMessages.DisableAction_ConfirmMessage);
		if (!confirmed) {
			return Window.CANCEL;
		}
		ProvisioningJob job = operation.getProvisioningJob(null);
		if (job == null) {
			// Resolution failed
			StatusManager.getManager().handle(operation.getResolutionResult(),
					StatusManager.SHOW | StatusManager.LOG);
			return Window.CANCEL;
		}

		if (job instanceof org.eclipse.equinox.p2.operations.ProfileModificationJob profileModificationJob) {
			profileModificationJob.setRestartPolicy(ProvisioningJob.RESTART_ONLY);
		}
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					// Write bundles.info after the p2 profile is committed.
					DisableIUOperation.updateBundlesInfo(ius, false,
							ui.getSession(), ui.getProfileId());
				}
			}
		});
		ui.schedule(job, StatusManager.SHOW | StatusManager.LOG);
		return Window.OK;
	}
}
