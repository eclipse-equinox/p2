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
package org.eclipse.equinox.p2.ui.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.engine.phases.Sizing;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.operations.*;
import org.eclipse.swt.widgets.Shell;

public class InstallDialog extends UpdateInstallDialog {

	public InstallDialog(Shell parentShell, IInstallableUnit[] ius, Profile profile) {
		super(parentShell, ius, profile, ProvUIMessages.InstallIUOperationLabel, ProvUIMessages.InstallDialog_InstallSelectionMessage);
	}

	protected String getOkButtonString() {
		return ProvUIMessages.InstallIUOperationLabelWithMnemonic;
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

	protected String getOperationLabel() {
		return ProvUIMessages.InstallIUOperationLabel;
	}

	protected ProfileModificationOperation createProfileModificationOperation(Object[] selectedElements) {
		try {
			IInstallableUnit[] ius = elementsToIUs(selectedElements);
			ProvisioningPlan plan = ProvisioningUtil.getInstallPlan(ius, profile, null);
			IStatus status = plan.getStatus();
			if (status.isOK())
				return new InstallOperation(getOperationLabel(), profile.getProfileId(), plan, ius);
			ProvUI.reportStatus(status);
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null);
		}
		return null;
	}
}