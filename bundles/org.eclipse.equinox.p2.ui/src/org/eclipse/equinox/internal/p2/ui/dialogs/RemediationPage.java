/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.ArrayList;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class RemediationPage extends ResolutionStatusPage {

	private RemediationComposite remediationComposite;
	private Composite mainComposite;

	protected RemediationPage(ProvisioningUI ui, ProvisioningOperationWizard wizard, IUElementListRoot input, ProfileChangeOperation operation) {
		super("RemediationPage", ui, wizard); //$NON-NLS-1$
		if (wizard instanceof InstallWizard) {
			setTitle(ProvUIMessages.InstallRemediationPage_Title);
			setDescription(ProvUIMessages.InstallRemediationPage_Description);
		} else {
			setTitle(ProvUIMessages.UpdateRemediationPage_Title);
			setDescription(ProvUIMessages.UpdateRemediationPage_Description);
		}
	}

	public void createControl(Composite parent) {
		mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new GridLayout());

		remediationComposite = new RemediationComposite();
		remediationComposite.createRemediationControl(mainComposite);
		Composite innerComposite = remediationComposite.getComposite();
		setMessage(remediationComposite.getMessage(), IStatus.WARNING);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		innerComposite.setLayoutData(gd);
		setControl(mainComposite);
		setPageComplete(false);

		Dialog.applyDialogFont(mainComposite);

	}

	public ArrayList<AvailableIUElement> transformIUstoIUElements() {
		return remediationComposite.transformIUstoIUElements();
	}

	public boolean canFlipToNextPage() {
		return isPageComplete();
	}

	public void updateStatus(IUElementListRoot newRoot, ProfileChangeOperation operation, Object[] planSelections) {
		remediationComposite.update(((ProvisioningOperationWizard) getWizard()).getRemediationOperation());
	}

	@Override
	protected void updateCaches(IUElementListRoot root, ProfileChangeOperation resolvedOperation) {
		// TODO Auto-generated method stub

	}

	@Override
	protected boolean isCreated() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected IUDetailsGroup getDetailsGroup() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected IInstallableUnit getSelectedIU() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Object[] getSelectedElements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getDialogSettingsName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected SashForm getSashForm() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int getColumnWidth(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected String getClipboardText(Control control) {
		// TODO Auto-generated method stub
		return null;
	}
}