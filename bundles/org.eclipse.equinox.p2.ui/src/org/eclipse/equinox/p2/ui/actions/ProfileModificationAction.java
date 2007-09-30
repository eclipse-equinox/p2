/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.equinox.p2.ui.operations.IOperationConfirmer;
import org.eclipse.equinox.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

abstract class ProfileModificationAction extends ProvisioningAction {

	Profile profile;
	IProfileChooser profileChooser;

	public ProfileModificationAction(String text, ISelectionProvider selectionProvider, IOperationConfirmer confirmer, Profile profile, IProfileChooser profileChooser, Shell shell) {
		super(text, selectionProvider, confirmer, shell);
		this.profile = profile;
		this.profileChooser = profileChooser;
	}

	public void run() {
		// If the profile was not provided, see if we have a
		// viewer element that can tell us.
		Profile targetProfile = profile;
		if (targetProfile == null && profileChooser != null) {
			targetProfile = profileChooser.getProfile();
		}
		// We could not figure out a profile to operate on, so return
		if (targetProfile == null) {
			return;
		}

		List elements = getStructuredSelection().toList();
		List iusList = new ArrayList(elements.size());

		for (int i = 0; i < elements.size(); i++) {
			Object element = elements.get(i);
			if (element instanceof IInstallableUnit) {
				iusList.add(element);
			} else if (element instanceof IAdaptable) {
				iusList.add(((IAdaptable) element).getAdapter(IInstallableUnit.class));
			}
		}

		final IInstallableUnit[] ius = (IInstallableUnit[]) iusList.toArray(new IInstallableUnit[iusList.size()]);
		final ProfileModificationOperation[] ops = new ProfileModificationOperation[1];
		final Profile prof = targetProfile;
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				ops[0] = validateAndGetOperation(ius, prof, monitor);
			}
		};
		try {
			new ProgressMonitorDialog(getShell()).run(false, false, runnable);
		} catch (InterruptedException e) {
			// don't report thread interruption
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), null);
		}

		if (ops[0] == null)
			return;

		if (operationConfirmer != null && !operationConfirmer.continuePerformingOperation(ops[0], getShell())) {
			return;
		}

		final IStatus[] status = new IStatus[1];
		final IAdaptable adapter = ProvUI.getUIInfoAdapter(getShell());
		runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
					status[0] = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(ops[0], monitor, adapter);
					if (!status[0].isOK()) {
						StatusManager.getManager().handle(status[0], StatusManager.SHOW | StatusManager.LOG);
					}
				} catch (ExecutionException e) {
					ProvUI.handleException(e.getCause(), null);
				}
			}
		};
		try {
			new ProgressMonitorDialog(getShell()).run(true, true, runnable);
			// If we updated the running profile, we need to determine whether to restart.
			// TODO for now we pretend restart is optional, we really don't know yet
			if (status[0] != null && status[0].isOK()) {
				try {
					Profile selfProfile = ProvisioningUtil.getProfile(IProfileRegistry.SELF);
					if (selfProfile != null && (selfProfile.getProfileId().equals(targetProfile.getProfileId()))) {
						ProvisioningUtil.requestRestart(false, ProvUI.getUIInfoAdapter(getShell()));
					}
				} catch (ProvisionException e) {
					ProvUI.handleException(e, null);
				}
			}
		} catch (InterruptedException e) {
			// don't report thread interruption
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), null);
		}
	}

	/*
	 * Validate whether the proposed profile modification operation can run.
	 * If so, return an operation representing it.  If not, return null.
	 * We assume the user has been notified if something couldn't happen.
	 */
	protected abstract ProfileModificationOperation validateAndGetOperation(IInstallableUnit[] ius, Profile targetProfile, IProgressMonitor monitor);
}