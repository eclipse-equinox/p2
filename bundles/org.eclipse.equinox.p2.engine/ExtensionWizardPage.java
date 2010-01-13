/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.server.discovery.internal.wizard;

import org.eclipse.equinox.p2.engine.ProvisioningPlan;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.AcceptLicensesWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.server.discovery.ExtensionWizard;
import org.eclipse.wst.server.discovery.internal.Messages;
import org.eclipse.wst.server.discovery.internal.Trace;
import org.eclipse.wst.server.discovery.internal.model.Extension;

public class ExtensionWizardPage extends WizardPage {
	private ExtensionComposite comp;
	protected AcceptLicensesWizardPage licensePage;
	protected ErrorWizardPage errorPage;
	protected IWizardPage nextPage;
	private Extension extension;

	public ExtensionWizardPage(AcceptLicensesWizardPage licenseWizardPage, ErrorWizardPage errorWizardPage) {
		super("extension");
		this.licensePage = licenseWizardPage;
		this.errorPage = errorWizardPage;
		setTitle(Messages.wizExtensionTitle);
		setDescription(Messages.wizExtensionDescription);
		setPageComplete(false);
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		Composite composite = new Composite(parent, SWT.NULL);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(data);
		
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(4);
		layout.verticalSpacing = convertVerticalDLUsToPixels(4);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		//WorkbenchHelp.setHelp(this, ContextIds.SELECT_CLIENT_WIZARD);
		
		Label label = new Label(composite, SWT.WRAP);
		data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		data.widthHint = 350;
		label.setLayoutData(data);
		label.setText(Messages.wizExtensionMessage);
		
		comp = new ExtensionComposite(composite, SWT.NONE, new ExtensionComposite.ExtensionSelectionListener() {
			public void extensionSelected(Extension sel) {
				handleSelection(sel);
			}
		});
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 375;
		comp.setLayoutData(data);
		
		Dialog.applyDialogFont(composite);
		setControl(composite);
	}

	protected void handleSelection(Extension sel) {
		extension = sel;
		if (extension == null)
			licensePage.update(new IInstallableUnit[0], null);
		else {
			try {
				getContainer().run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						final ProvisioningPlan plan = extension.getProvisioningPlan(true, monitor);
						if (plan != null && plan.getStatus().isOK()) {
							getShell().getDisplay().asyncExec(new Runnable() {
								public void run() {
									licensePage.update(extension.getIUs(), plan);
									nextPage = licensePage;
									((ExtensionWizard)getWizard()).setSecondPage(nextPage);
								}
							});
						} else {
							getShell().getDisplay().asyncExec(new Runnable() {
								public void run() {
									errorPage.setStatus(plan.getStatus());
								}
							});
							nextPage = errorPage;
							((ExtensionWizard)getWizard()).setSecondPage(nextPage);
						}
						monitor.done();
					}
				});
			} catch (Exception e) {
				Trace.trace(Trace.SEVERE, "Error verifying license", e);
			}
		}
		setPageComplete(extension != null);
	}

	public Extension getExtension() {
		return extension;
	}
}