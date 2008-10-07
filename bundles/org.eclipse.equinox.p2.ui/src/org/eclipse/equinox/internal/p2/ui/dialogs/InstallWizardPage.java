/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.InstallAction;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.InstallWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class InstallWizardPage extends ProfileModificationWizardPage {
	InstallWizard wizard;
	boolean useCheckbox;

	public InstallWizardPage(Policy policy, String profileId, IInstallableUnit[] selectedIUs, ProvisioningPlan plan, InstallWizard wizard) {
		super(policy, "InstallWizardPage", selectedIUs, profileId, plan); //$NON-NLS-1$
		useCheckbox = selectedIUs != null;
		this.wizard = wizard;
		setTitle(ProvUIMessages.InstallIUOperationLabel);
		if (useCheckbox)
			setDescription(ProvUIMessages.InstallDialog_InstallSelectionMessage);
		else
			setDescription(ProvUIMessages.InstallWizardPage_NoCheckboxDescription);
	}

	protected String getOperationLabel() {
		return ProvUIMessages.InstallIUOperationLabel;
	}

	protected ProfileChangeRequest computeProfileChangeRequest(Object[] selectedElements, MultiStatus additionalStatus, IProgressMonitor monitor) {
		IInstallableUnit[] selected = elementsToIUs(selectedElements);
		return InstallAction.computeProfileChangeRequest(selected, getProfileId(), additionalStatus, monitor);
	}

	protected TableViewer createTableViewer(Composite parent) {
		if (!useCheckbox)
			return new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		return super.createTableViewer(parent);
	}

	protected Object[] getCheckedElements() {
		if (!useCheckbox)
			return wizard.getCheckedIUs();
		return super.getCheckedElements();
	}

	protected void setInitialCheckState() {
		if (!useCheckbox) {
			return;
		}
		super.setInitialCheckState();
	}

	public void updateIUs() {
		tableViewer.setInput(getCheckedElements());
		super.checkedIUsChanged();
	}
}
