/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *     Red Hat Inc. - Bug 460967
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 506432
 ******************************************************************************/

package org.eclipse.equinox.p2.ui;

import java.net.URI;
import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * ProvisioningUI defines the provisioning session, UI policy, and related services for a
 * provisioning UI.  
 * 
 * @since 2.0
 */
public class ProvisioningUI {

	/**
	 * Return the default ProvisioningUI.  
	 * 
	 * @return the default Provisioning UI.
	 */
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
		// register this UI with the agent so other UI related agent services can find it
		session.getProvisioningAgent().registerService(ProvisioningUI.class.getName(), this);
	}

	/**
	 * Return the UI policy used for this instance of the UI.
	 * 
	 * @return the UI policy, must not be <code>null</code>
	 */
	public Policy getPolicy() {
		return policy;
	}

	/**
	 * Return the provisioning session that should be used to obtain
	 * provisioning services.
	 * 
	 * @return the provisioning session, must not be <code>null</code>
	 */
	public ProvisioningSession getSession() {
		return session;
	}

	/**
	 * Return the license manager that should be used to remember
	 * accepted user licenses.
	 * @return  the license manager.  May be <code>null</code> if licenses are not
	 * to be remembered.
	 */
	public LicenseManager getLicenseManager() {
		return ServiceHelper.getService(ProvUIActivator.getContext(), LicenseManager.class);
	}

	/**
	 * Return the repository tracker that should be used to add, remove, and track the
	 * statuses of known repositories.
	 * 
	 * @return the repository tracker, must not be <code>null</code>
	 */
	public RepositoryTracker getRepositoryTracker() {
		return (RepositoryTracker) session.getProvisioningAgent().getService(RepositoryTracker.class.getName());
	}

	/**
	 * Return the profile id that should be assumed for this ProvisioningUI if no other
	 * id is otherwise specified.  Some UI classes are assigned a profile id, while others 
	 * are not.  For those classes that are not assigned a current profile id, this id can
	 * be used to obtain one.
	 * 
	 * @return a profile id
	 */
	public String getProfileId() {
		return profileId;
	}

	/**
	 * Return an install operation that describes installing the specified IInstallableUnits from the
	 * provided list of repositories.
	 * 
	 * @param iusToInstall the IInstallableUnits to be installed
	 * @param repositories the repositories to use for the operation
	 * @return the install operation
	 */
	public InstallOperation getInstallOperation(Collection<IInstallableUnit> iusToInstall, URI[] repositories) {
		InstallOperation op = new InstallOperation(getSession(), iusToInstall);
		op.setProfileId(getProfileId());
		op.setProvisioningContext(makeProvisioningContext(repositories));
		return op;
	}

	/**
	 * Return an update operation that describes updating the specified IInstallableUnits from the
	 * provided list of repositories.
	 * 
	 * @param iusToUpdate the IInstallableUnits to be updated
	 * @param repositories the repositories to use for the operation
	 * @return the update operation
	 */
	public UpdateOperation getUpdateOperation(Collection<IInstallableUnit> iusToUpdate, URI[] repositories) {
		UpdateOperation op = new UpdateOperation(getSession(), iusToUpdate);
		op.setProfileId(getProfileId());
		op.setProvisioningContext(makeProvisioningContext(repositories));
		return op;
	}

	/**
	 * Return an uninstall operation that describes uninstalling the specified IInstallableUnits, using
	 * the supplied repositories to replace any metadata that must be retrieved for the uninstall.
	 * 
	 * @param iusToUninstall the IInstallableUnits to be installed
	 * @param repositories the repositories to use for the operation
	 * @return the uninstall operation
	 */
	public UninstallOperation getUninstallOperation(Collection<IInstallableUnit> iusToUninstall, URI[] repositories) {
		UninstallOperation op = new UninstallOperation(getSession(), iusToUninstall);
		op.setProfileId(getProfileId());
		op.setProvisioningContext(makeProvisioningContext(repositories));
		return op;
	}

	private ProvisioningContext makeProvisioningContext(URI[] repos) {
		if (repos != null) {
			ProvisioningContext context = new ProvisioningContext(getSession().getProvisioningAgent());
			context.setMetadataRepositories(repos);
			context.setArtifactRepositories(repos);
			return context;
		}
		// look everywhere
		return new ProvisioningContext(getSession().getProvisioningAgent());
	}

	/**
	 * Open an install wizard for installing the specified IInstallableUnits
	 * 
	 * @param initialSelections the IInstallableUnits that should be selected when the wizard opens.  May be <code>null</code>.
	 * @param operation the operation describing the proposed install.  If this operation is not <code>null</code>, then a wizard showing
	 * only the IInstallableUnits described in the operation will be shown.  If the operation is <code>null</code>, then a
	 * wizard allowing the user to browse the repositories will be opened.
	 * @param job a repository load job that is loading or has already loaded the repositories.  Can be used to pass along
	 * an in-memory repository reference to the wizard.
	 * 
	 * @return the wizard return code
	 */
	public int openInstallWizard(Collection<IInstallableUnit> initialSelections, InstallOperation operation, LoadMetadataRepositoryJob job) {
		return openInstallWizard(initialSelections, operation, null, job);
	}

	/**
	 * Open an install wizard for installing the specified IInstallableUnits and remediationOperation.
	 * 
	 * @param initialSelections the IInstallableUnits that should be selected when the wizard opens.  May be <code>null</code>.
	 * @param operation the operation describing the proposed install.  If this operation is not <code>null</code>, then a wizard showing
	 * only the IInstallableUnits described in the operation will be shown.  If the operation is <code>null</code>, then a
	 * wizard allowing the user to browse the repositories will be opened.
	 * @param remediationOperation the alternate operations if the proposed update failed.  May be <code>null</code>.
	 * @param job a repository load job that is loading or has already loaded the repositories.  Can be used to pass along
	 * an in-memory repository reference to the wizard.
	 * 
	 * @return the wizard return code
	 * @see RemediationOperation
	 * @since 2.3
	 */
	public int openInstallWizard(Collection<IInstallableUnit> initialSelections, InstallOperation operation, RemediationOperation remediationOperation, LoadMetadataRepositoryJob job) {
		if (operation == null) {
			InstallWizard wizard = new InstallWizard(this, operation, initialSelections, job);
			// check if provitiondialog is already open and give focus to it, if that is the case
			Shell[] shells = Display.getCurrent().getShells();
			for (Shell shell : shells) {
				if (shell.getData() instanceof ProvisioningWizardDialog) {
					shell.forceActive();
					shell.setFocus();
					return Window.OK;
				}
			}
			WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
			dialog.create();
			PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.INSTALL_WIZARD);
			return dialog.open();
		}
		PreselectedIUInstallWizard wizard = new PreselectedIUInstallWizard(this, operation, initialSelections, job);
		wizard.setRemediationOperation(remediationOperation);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.INSTALL_WIZARD);
		return dialog.open();
	}

	/**
	 * Open an update wizard for the specified update operation.
	 * 
	 * @param skipSelectionsPage <code>true</code> if the selection page should be skipped so that the user is 
	 * viewing the resolution results.  <code>false</code> if the update selection page should be shown first.
	 * @param operation the operation describing the proposed update.  Must not be <code>null</code>.
	 * @param job a repository load job that is loading or has already loaded the repositories.  Can be used to pass along
	 * an in-memory repository reference to the wizard.
	 * 
	 * @return the wizard return code
	 */
	public int openUpdateWizard(boolean skipSelectionsPage, UpdateOperation operation, LoadMetadataRepositoryJob job) {
		return openUpdateWizard(skipSelectionsPage, operation, null, job);

	}

	/**
	 * Open an update wizard for the specified update operation and remediationOperation.
	 * 
	 * @param skipSelectionsPage <code>true</code> if the selection page should be skipped so that the user is 
	 * viewing the resolution results.  <code>false</code> if the update selection page should be shown first.
	 * @param operation the operation describing the proposed update.  Must not be <code>null</code>.
	 * @param remediationOperation the alternate operations if the proposed update failed.  May be <code>null</code>.
	 * @param job a repository load job that is loading or has already loaded the repositories.  Can be used to pass along
	 * an in-memory repository reference to the wizard.
	 * 
	 * @return the wizard return code
	 * @since 2.3
	 */
	public int openUpdateWizard(boolean skipSelectionsPage, UpdateOperation operation, RemediationOperation remediationOperation, LoadMetadataRepositoryJob job) {
		if (getPolicy().getUpdateWizardStyle() == Policy.UPDATE_STYLE_SINGLE_IUS && UpdateSingleIUWizard.validFor(operation)) {
			UpdateSingleIUWizard wizard = new UpdateSingleIUWizard(this, operation);
			WizardDialog dialog = new WizardDialog(ProvUI.getDefaultParentShell(), wizard);
			dialog.create();
			// TODO do we need a hook for providing custom help?  Or would this just be shown in the update browser?
			return dialog.open();
		}
		UpdateWizard wizard = new UpdateWizard(this, operation, operation.getSelectedUpdates(), job);
		wizard.setRemediationOperation(remediationOperation);
		wizard.setSkipSelectionsPage(skipSelectionsPage);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.UPDATE_WIZARD);
		if (wizard.getCurrentStatus().getSeverity() == IStatus.ERROR && remediationOperation != null && remediationOperation.getResolutionResult() != Status.OK_STATUS) {
			wizard.deselectLockedIUs();
		}
		return dialog.open();

	}

	/**
	 * Open an uninstall wizard for the specified uninstall operation.
	 * 
	 * @param initialSelections the IInstallableUnits that should be selected when the wizard opens.  May be <code>null</code>.
	 * @param operation the operation describing the proposed uninstall.  Must not be <code>null</code>.
	 * @param job a repository load job that is loading or has already loaded the repositories.  Can be used to pass along
	 * an in-memory repository reference to the wizard.
	 * 
	 * @return the wizard return code
	 */
	public int openUninstallWizard(Collection<IInstallableUnit> initialSelections, UninstallOperation operation, LoadMetadataRepositoryJob job) {
		UninstallWizard wizard = new UninstallWizard(this, operation, initialSelections, job);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.UNINSTALL_WIZARD);
		return dialog.open();
	}

	/**
	 * Open a UI that allows the user to manipulate the repositories.
	 * @param shell the shell that should parent the UI
	 */
	public void manipulateRepositories(Shell shell) {
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

					Control control = page.getControl();
					control.setLayoutData(new GridData(GridData.FILL_BOTH));
					return page.getControl();
				}

				protected boolean isResizable() {
					return true;
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
	}

	/**
	 * Schedule a job to execute the supplied ProvisioningOperation.
	 * 
	 * @param job The operation to execute
	 * @param errorStyle the flags passed to the StatusManager for error reporting
	 */
	public void schedule(final ProvisioningJob job, final int errorStyle) {
		job.setUser(true);
		runner.schedule(job, errorStyle);
	}

	/**
	 * Manage the supplied job as a provisioning operation.  This will allow
	 * the ProvisioningUI to be aware that a provisioning job is running, as well
	 * as manage the restart behavior for the job.
	 * 
	 * @param job the job to be managed
	 * @param jobRestartPolicy an integer constant specifying whether the
	 * supplied job should cause a restart of the system.  The UI Policy's
	 * restart policy is used in conjunction with this constant to determine
	 * what actually occurs when a job completes.
	 * 
	 * @see ProvisioningJob#RESTART_NONE
	 * @see ProvisioningJob#RESTART_ONLY
	 * @see ProvisioningJob#RESTART_OR_APPLY
	 */
	public void manageJob(Job job, final int jobRestartPolicy) {
		runner.manageJob(job, jobRestartPolicy);
	}

	/**
	 * Return a boolean indicating whether the receiver has scheduled any operations
	 * for the profile under management.
	 * 
	 * @return <code>true</code> if other provisioning operations have been scheduled,
	 * <code>false</code> if there are no operations scheduled.
	 */
	public boolean hasScheduledOperations() {
		return getSession().hasScheduledOperationsFor(profileId);
	}

	/**
	 * This method is for automated testing only.
	 * @return the provisioning operation that can suppress restart for automated testing.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public ProvisioningOperationRunner getOperationRunner() {
		return runner;
	}

	/**
	 * Signal that a repository operation is about to begin.  This allows clients to ignore intermediate
	 * events until the operation is completed.  Callers are responsible for ensuring that
	 * a corresponding operation ending event is signaled.
	 */
	public void signalRepositoryOperationStart() {
		runner.eventBatchCount++;
		if (Tracing.DEBUG_EVENTS_CLIENT)
			Tracing.debug("Batch Count Incremented to:  " + Integer.toString(runner.eventBatchCount)); //$NON-NLS-1$
		ProvUI.getProvisioningEventBus(getSession()).publishEvent(new RepositoryOperationBeginningEvent(this));
	}

	/**
	 * Signal that a repository operation has completed.
	 * 
	 * @param event a {@link RepositoryEvent} that describes the overall operation.  May be <code>null</code>, which
	 * indicates that there was no single event that can describe the operation.  
	 * @param update <code>true</code> if the event should be reflected in the UI, false if it should be ignored.
	 */
	public void signalRepositoryOperationComplete(RepositoryEvent event, boolean update) {
		runner.eventBatchCount--;
		if (Tracing.DEBUG_EVENTS_CLIENT)
			Tracing.debug("Batch Count Decremented to:  " + Integer.toString(runner.eventBatchCount)); //$NON-NLS-1$
		ProvUI.getProvisioningEventBus(getSession()).publishEvent(new RepositoryOperationEndingEvent(this, update, event));
	}

	/**
	 * Load the specified metadata repository, signaling a repository operation start event
	 * before loading, and a repository operation complete event after loading.
	 * 
	 * @param location the location of the repository
	 * @param notify <code>true</code> if the UI should be updated as a result of the load, <code>false</code> if it should not
	 * @param monitor the progress monitor to be used
	 * @return the repository
	 * @throws ProvisionException if the repository could not be loaded
	 */

	public IMetadataRepository loadMetadataRepository(URI location, boolean notify, IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepository repo = null;
		try {
			signalRepositoryOperationStart();
			IMetadataRepositoryManager manager = ProvUI.getMetadataRepositoryManager(getSession());
			repo = manager.loadRepository(location, monitor);
			// If there is no user nickname assigned to this repo but there is a provider name, then set the nickname.
			// This will keep the name in the manager even when the repo is not loaded
			String name = manager.getRepositoryProperty(location, IRepository.PROP_NICKNAME);
			if (name == null || name.length() == 0) {
				name = repo.getName();
				if (name != null && name.length() > 0)
					manager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, name);
			}
		} catch (ProvisionException e) {
			getRepositoryTracker().reportLoadFailure(location, e);
		} finally {
			// We have no idea how many repos may have been touched as a result of loading this one.
			signalRepositoryOperationComplete(null, notify);
		}
		return repo;
	}

	/**
	 * Load the specified artifact repository, signaling a repository operation start event
	 * before loading, and a repository operation complete event after loading.
	 * 
	 * @param location the location of the repository
	 * @param update <code>true</code> if the UI should be updated as a result of the load, <code>false</code> if it should not
	 * @param monitor the progress monitor to be used
	 * @return the repository
	 * @throws ProvisionException if the repository could not be loaded
	 */
	public IArtifactRepository loadArtifactRepository(URI location, boolean update, IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepository repo;
		signalRepositoryOperationStart();
		try {
			IArtifactRepositoryManager manager = ProvUI.getArtifactRepositoryManager(getSession());
			repo = manager.loadRepository(location, monitor);

			// If there is no user nickname assigned to this repo but there is a provider name, then set the nickname.
			// This will keep the name in the manager even when the repo is not loaded
			String name = manager.getRepositoryProperty(location, IRepository.PROP_NICKNAME);
			if (name == null) {
				name = manager.getRepositoryProperty(location, IRepository.PROP_NAME);
				if (name != null)
					manager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, name);
			}
		} finally {
			// We have no idea how many repos may have been touched as a result of loading this one,
			// so we do not use a specific repository event to represent it.
			signalRepositoryOperationComplete(null, update);
		}
		return repo;
	}
}
