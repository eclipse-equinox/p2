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

package org.eclipse.equinox.internal.p2.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;

public abstract class ProfileModificationAction extends ProvisioningAction {

	Profile profile;
	IProfileChooser profileChooser;
	LicenseManager licenseManager;

	protected ProfileModificationAction(String text, ISelectionProvider selectionProvider, Profile profile, IProfileChooser profileChooser, LicenseManager licenseManager, Shell shell) {
		super(text, selectionProvider, shell);
		this.profile = profile;
		this.profileChooser = profileChooser;
		this.licenseManager = licenseManager;
	}

	public void run() {
		// If the profile was not provided, see if we have a
		// viewer element that can tell us.
		Profile targetProfile = profile;
		if (targetProfile == null && profileChooser != null) {
			targetProfile = profileChooser.getProfile(getShell());
		}
		// We could not figure out a profile to operate on, so return
		if (targetProfile == null) {
			return;
		}

		List elements = getStructuredSelection().toList();
		List iusList = new ArrayList(elements.size());

		for (int i = 0; i < elements.size(); i++) {
			IInstallableUnit iu = getIU(elements.get(i));
			if (iu != null)
				iusList.add(iu);
		}

		final IInstallableUnit[] ius = (IInstallableUnit[]) iusList.toArray(new IInstallableUnit[iusList.size()]);
		final IStatus[] status = new IStatus[1];
		final Profile prof = targetProfile;
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				status[0] = validateOperation(ius, prof, monitor);
			}
		};
		try {
			new ProgressMonitorDialog(getShell()).run(true, true, runnable);
		} catch (InterruptedException e) {
			// don't report thread interruption
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), null);
		}

		if (status[0].isOK())
			performOperation(ius, prof);
		else
			ProvUI.reportStatus(status[0]);

	}

	/*
	 * Validate whether the proposed profile modification operation can run.
	 */
	protected abstract IStatus validateOperation(IInstallableUnit[] ius, Profile targetProfile, IProgressMonitor monitor);

	/*
	 * Run the operation, opening any dialogs, etc. 
	 */
	protected abstract void performOperation(IInstallableUnit[] ius, Profile targetProfile);

	protected abstract String getTaskName();

	protected IInstallableUnit getIU(Object element) {
		return (IInstallableUnit) ProvUI.getAdapter(element, IInstallableUnit.class);

	}

	protected LicenseManager getLicenseManager() {
		return licenseManager;
	}

}