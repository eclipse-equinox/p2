/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.ui;

import java.net.URI;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.common.LicenseManager;
import org.eclipse.equinox.p2.common.TranslationSupport;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * @since 2.0
 *
 */
public class ProvisioningUI {

	public static ProvisioningUI getDefaultUI() {
		return ProvUIActivator.getDefault().getProvisioningUI();
	}

	private Policy policy;
	private ProvisioningSession session;
	private String profileId;
	private ProvisioningOperationRunner runner;

	/**
	 * Creates a new instance of the provisioning user interface.
	 * 
	 * @param session The current provisioning session
	 * @param profileId The profile that this user interface is operating on
	 * @param policy The user interface policy settings to use
	 */
	public ProvisioningUI(ProvisioningSession session, String profileId, Policy policy) {
		this.policy = policy;
		this.profileId = profileId;
		if (profileId == null)
			this.profileId = IProfileRegistry.SELF;
		this.session = session;
		this.runner = new ProvisioningOperationRunner(this);
	}

	public Policy getPolicy() {
		return policy;
	}

	public ProvisioningSession getSession() {
		return session;
	}

	public LicenseManager getLicenseManager() {
		return (LicenseManager) ServiceHelper.getService(ProvUIActivator.getContext(), LicenseManager.class.getName());
	}

	public RepositoryTracker getRepositoryTracker() {
		return (RepositoryTracker) ServiceHelper.getService(ProvUIActivator.getContext(), RepositoryTracker.class.getName());
	}

	public String getProfileId() {
		return profileId;
	}

	public TranslationSupport getTranslationSupport() {
		return ProvUIActivator.getDefault().getTranslationSupport();
	}

	public InstallOperation getInstallOperation(IInstallableUnit[] iusToInstall, URI[] repositories) {
		InstallOperation op = new InstallOperation(getSession(), iusToInstall);
		op.setProfileId(getProfileId());
		//		op.setRootMarkerKey(getPolicy().getQueryContext().getVisibleInstalledIUProperty());
		op.setProvisioningContext(makeProvisioningContext(repositories));
		return op;
	}

	public UpdateOperation getUpdateOperation(IInstallableUnit[] iusToUpdate, URI[] repositories) {
		UpdateOperation op = new UpdateOperation(getSession(), iusToUpdate);
		op.setProfileId(getProfileId());
		//		op.setRootMarkerKey(getPolicy().getQueryContext().getVisibleInstalledIUProperty());
		op.setProvisioningContext(makeProvisioningContext(repositories));
		return op;
	}

	public UninstallOperation getUninstallOperation(IInstallableUnit[] iusToUninstall, URI[] repositories) {
		UninstallOperation op = new UninstallOperation(getSession(), iusToUninstall);
		op.setProfileId(getProfileId());
		//		op.setRootMarkerKey(getPolicy().getQueryContext().getVisibleInstalledIUProperty());
		op.setProvisioningContext(makeProvisioningContext(repositories));
		return op;
	}

	private ProvisioningContext makeProvisioningContext(URI[] repos) {
		if (repos != null) {
			ProvisioningContext context = new ProvisioningContext(repos);
			context.setArtifactRepositories(repos);
			return context;
		}
		// look everywhere
		return new ProvisioningContext();
	}

	public int openInstallWizard(Shell shell, IInstallableUnit[] initialSelections, InstallOperation operation, LoadMetadataRepositoryJob job) {
		if (operation == null) {
			InstallWizard wizard = new InstallWizard(this, operation, initialSelections, job);
			WizardDialog dialog = new ProvisioningWizardDialog(shell, wizard);
			dialog.create();
			PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.INSTALL_WIZARD);
			return dialog.open();
		}
		PreselectedIUInstallWizard wizard = new PreselectedIUInstallWizard(this, operation, initialSelections, job);
		WizardDialog dialog = new ProvisioningWizardDialog(shell, wizard);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.INSTALL_WIZARD);
		return dialog.open();
	}

	public int openUpdateWizard(Shell shell, boolean skipSelectionsPage, UpdateOperation operation, LoadMetadataRepositoryJob job) {
		UpdateWizard wizard = new UpdateWizard(this, operation, operation.getSelectedUpdates(), job);
		wizard.setSkipSelectionsPage(skipSelectionsPage);
		WizardDialog dialog = new ProvisioningWizardDialog(shell, wizard);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.UPDATE_WIZARD);
		return dialog.open();
	}

	public int openUninstallWizard(Shell shell, IInstallableUnit[] initialSelections, UninstallOperation operation, LoadMetadataRepositoryJob job) {
		UninstallWizard wizard = new UninstallWizard(this, operation, initialSelections, job);
		WizardDialog dialog = new ProvisioningWizardDialog(shell, wizard);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.UNINSTALL_WIZARD);
		return dialog.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#manipulateRepositories(org.eclipse.swt.widgets.Shell)
	 */
	public boolean manipulateRepositories(Shell shell) {
		if (policy.getRepositoryPreferencePageId() != null) {
			PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(shell, policy.getRepositoryPreferencePageId(), null, null);
			dialog.open();
		} else {
			TitleAreaDialog dialog = new TitleAreaDialog(shell) {
				RepositoryManipulationPage page;

				protected Control createDialogArea(Composite parent) {
					page = new RepositoryManipulationPage();
					page.setProvisioningUI(ProvisioningUI.this);
					page.init(PlatformUI.getWorkbench());
					page.createControl(parent);
					this.setTitle(ProvUIMessages.RepositoryManipulationPage_Title);
					this.setMessage(ProvUIMessages.RepositoryManipulationPage_Description);
					return page.getControl();
				}

				protected void okPressed() {
					if (page.performOk())
						super.okPressed();
				}

				protected void cancelPressed() {
					if (page.performCancel())
						super.cancelPressed();
				}
			};
			dialog.open();
		}
		return true;
	}

	public Shell getDefaultParentShell() {
		return ProvUI.getDefaultParentShell();
	}

	/**
	 * Schedule a job to execute the supplied ProvisioningOperation.
	 * 
	 * @param job The operation to execute
	 * @param errorStyle the flags passed to the StatusManager for error reporting
	 */
	public void schedule(final ProvisioningJob job, final int errorStyle) {
		runner.schedule(job, errorStyle);
	}

	public void manageJob(Job job, final int jobRestartPolicy) {
		runner.manageJob(job, jobRestartPolicy);
	}

	public boolean hasScheduledOperations() {
		return getSession().hasScheduledOperationsFor(profileId);
	}

	public ProvisioningOperationRunner getOperationRunner() {
		return runner;
	}
}
