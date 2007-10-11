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

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.engine.phases.SizingPhase;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.ProvisioningUtil;
import org.eclipse.equinox.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.p2.ui.model.AvailableUpdateElement;
import org.eclipse.equinox.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.p2.ui.viewers.IUColumnConfig;
import org.eclipse.swt.widgets.Shell;

public class UpdateDialog extends ProfileModificationDialog {

	public UpdateDialog(Shell parentShell, IInstallableUnit[] ius, Profile profile) {
		super(parentShell, ius, profile, ProvUIMessages.UpdateAction_UpdatesAvailableTitle, ProvUIMessages.UpdateAction_UpdatesAvailableMessage);
	}

	protected ProfileModificationOperation createProfileModificationOperation(Object[] selectedElements, IProgressMonitor monitor, IAdaptable uiInfo) {
		try {
			ProvisioningPlan plan = ProvisioningUtil.getReplacePlan(getIUsToReplace(selectedElements), elementsToIUs(selectedElements), profile, monitor, uiInfo);
			IStatus status = plan.getStatus();
			if (status.isOK())
				return new ProfileModificationOperation(ProvUIMessages.UpdateIUOperationLabel, profile.getProfileId(), plan);
			ProvUI.reportStatus(status);
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null);
		}
		return null;
	}

	protected String getOkButtonString() {
		return ProvUIMessages.UpdateIUOperationLabelWithMnemonic;
	}

	protected IUColumnConfig[] getColumnConfig() {
		return new IUColumnConfig[] {new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_ID), new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION), new IUColumnConfig(ProvUIMessages.ProvUI_SizeColumnTitle, IUColumnConfig.COLUMN_SIZE)};
	}

	protected AvailableIUElement[] makeElements(IInstallableUnit[] ius) {
		List elements = new ArrayList();
		for (int i = 0; i < ius.length; i++) {
			try {
				IInstallableUnit[] replacementIUs = ProvisioningUtil.updatesFor(new IInstallableUnit[] {ius[i]}, profile, null, ProvUI.getUIInfoAdapter(getShell()));
				for (int j = 0; j < replacementIUs.length; j++) {
					elements.add(new AvailableUpdateElement(replacementIUs[j], getSize(ius[i], replacementIUs[j]), ius[i]));
				}
			} catch (ProvisionException e) {
				break;
			}
		}
		return (AvailableIUElement[]) elements.toArray(new AvailableIUElement[elements.size()]);
	}

	private IInstallableUnit[] getIUsToReplace(Object[] replacementElements) {
		Set iusToReplace = new HashSet();
		for (int i = 0; i < replacementElements.length; i++) {
			if (replacementElements[i] instanceof AvailableUpdateElement) {
				iusToReplace.add(((AvailableUpdateElement) replacementElements[i]).getIUToBeUpdated());
			}
		}
		return (IInstallableUnit[]) iusToReplace.toArray(new IInstallableUnit[iusToReplace.size()]);
	}

	protected long getSize(IInstallableUnit iuToRemove, IInstallableUnit iuToAdd) {
		long size;
		try {
			ProvisioningPlan plan = ProvisioningUtil.getReplacePlan(new IInstallableUnit[] {iuToRemove}, new IInstallableUnit[] {iuToAdd}, profile, new NullProgressMonitor(), ProvUI.getUIInfoAdapter(getShell()));
			SizingPhase info = ProvisioningUtil.getSizeInfo(plan, profile, new NullProgressMonitor(), ProvUI.getUIInfoAdapter(getShell()));
			size = info.getDlSize();
		} catch (ProvisionException e) {
			size = AvailableIUElement.SIZE_UNKNOWN;
		}
		return size;
	}
}