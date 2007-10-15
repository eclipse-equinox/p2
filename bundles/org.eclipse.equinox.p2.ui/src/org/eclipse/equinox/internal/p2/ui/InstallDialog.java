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
package org.eclipse.equinox.internal.p2.ui;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.engine.phases.Sizing;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.p2.ui.operations.*;
import org.eclipse.equinox.p2.ui.viewers.IUColumnConfig;
import org.eclipse.swt.widgets.Shell;

public class InstallDialog extends ProfileModificationDialog {

	public InstallDialog(Shell parentShell, IInstallableUnit[] ius, Profile profile) {
		super(parentShell, ius, profile, ProvUIMessages.InstallIUOperationLabel, ProvUIMessages.InstallDialog_InstallSelectionMessage);
	}

	protected ProfileModificationOperation createProfileModificationOperation(Object[] selectedElements, IProgressMonitor monitor) {
		try {
			IInstallableUnit[] selectedIUs = elementsToIUs(selectedElements);
			ProvisioningPlan plan = ProvisioningUtil.getInstallPlan(selectedIUs, profile, monitor);
			IStatus status = plan.getStatus();
			if (status.isOK())
				return new InstallOperation(ProvUIMessages.InstallIUOperationLabel, profile.getProfileId(), plan, selectedIUs);
			ProvUI.reportStatus(status);
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null);
		}
		return null;
	}

	protected String getOkButtonString() {
		return ProvUIMessages.InstallIUOperationLabelWithMnemonic;
	}

	protected IUColumnConfig[] getColumnConfig() {
		return new IUColumnConfig[] {new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_ID), new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION), new IUColumnConfig(ProvUIMessages.ProvUI_SizeColumnTitle, IUColumnConfig.COLUMN_SIZE)};
	}

	protected long getSize(IInstallableUnit iu) {
		long size;
		try {
			ProvisioningPlan plan = ProvisioningUtil.getInstallPlan(new IInstallableUnit[] {iu}, profile, new NullProgressMonitor());
			Sizing info = ProvisioningUtil.getSizeInfo(plan, profile, new NullProgressMonitor());
			size = info.getDiskSize();
		} catch (ProvisionException e) {
			size = AvailableIUElement.SIZE_UNKNOWN;
		}
		return size;
	}
}