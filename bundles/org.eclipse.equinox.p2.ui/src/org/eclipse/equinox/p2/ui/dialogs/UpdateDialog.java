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

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.internal.p2.ui.model.AvailableUpdateElement;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.engine.phases.Sizing;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.p2.ui.operations.ProvisioningUtil;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

public class UpdateDialog extends UpdateInstallDialog {

	public UpdateDialog(Shell parentShell, IInstallableUnit[] ius, Profile profile) {
		super(parentShell, ius, profile, ProvUIMessages.UpdateAction_UpdatesAvailableTitle, ProvUIMessages.UpdateAction_UpdatesAvailableMessage);
	}

	protected String getOkButtonString() {
		return ProvUIMessages.UpdateIUOperationLabelWithMnemonic;
	}

	protected AvailableIUElement[] makeElements(final IInstallableUnit[] ius) {
		final List elements = new ArrayList();

		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				monitor.beginTask(ProvUIMessages.UpdateDialog_AssemblingUpdatesProgress, ius.length);
				for (int i = 0; i < ius.length; i++) {
					if (monitor.isCanceled())
						close();
					try {
						IInstallableUnit[] replacementIUs = ProvisioningUtil.updatesFor(new IInstallableUnit[] {ius[i]}, null);
						SubMonitor loopMonitor = SubMonitor.convert(monitor, 100 / ius.length);
						loopMonitor.setWorkRemaining(replacementIUs.length);
						for (int j = 0; j < replacementIUs.length; j++) {
							elements.add(new AvailableUpdateElement(replacementIUs[j], getSize(ius[i], replacementIUs[j], loopMonitor.newChild(1)), ius[i]));
							if (monitor.isCanceled())
								close();
						}
					} catch (ProvisionException e) {
						break;
					}
				}
			}
		};

		try {
			new ProgressMonitorDialog(getShell()).run(false, true, runnable);
		} catch (InterruptedException e) {
			// don't report thread interruption
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), null);
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

	protected long getSize(IInstallableUnit iuToRemove, IInstallableUnit iuToAdd, IProgressMonitor monitor) {
		long size;
		try {
			ProvisioningPlan plan = ProvisioningUtil.getReplacePlan(new IInstallableUnit[] {iuToRemove}, new IInstallableUnit[] {iuToAdd}, profile, monitor);
			Sizing info = ProvisioningUtil.getSizeInfo(plan, profile, monitor);
			size = info.getDiskSize();
		} catch (ProvisionException e) {
			size = AvailableIUElement.SIZE_UNKNOWN;
		}
		return size;
	}

	protected String getOperationLabel() {
		return ProvUIMessages.UpdateIUOperationLabel;
	}

	protected ProfileModificationOperation createProfileModificationOperation(Object[] selectedElements) {
		try {
			ProvisioningPlan plan = ProvisioningUtil.getReplacePlan(getIUsToReplace(selectedElements), elementsToIUs(selectedElements), profile, null);
			IStatus status = plan.getStatus();
			if (status.isOK())
				return new ProfileModificationOperation(getOperationLabel(), profile.getProfileId(), plan);
			ProvUI.reportStatus(status);
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null);
		}
		return null;
	}
}