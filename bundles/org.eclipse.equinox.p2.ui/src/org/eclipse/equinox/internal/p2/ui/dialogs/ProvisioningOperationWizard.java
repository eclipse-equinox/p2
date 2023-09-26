/*******************************************************************************
 *  Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *     Red Hat, Inc. - support for remediation page
 *     Ericsson (AB) - bug 409073
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.stream.Stream;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.RemediationOperation;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Common superclass for a wizard that performs a provisioning operation.
 *
 * @since 3.5
 */
public abstract class ProvisioningOperationWizard extends Wizard {

	private static final String WIZARD_SETTINGS_SECTION = "WizardSettings"; //$NON-NLS-1$
	protected ProvisioningUI ui;
	protected IUElementListRoot root;
	protected ProfileChangeOperation operation;
	protected Object[] planSelections;
	protected RemediationPage remediationPage;
	protected ISelectableIUsPage mainPage;
	protected IResolutionErrorReportingPage errorPage;
	protected ResolutionResultsWizardPage resolutionPage;
	private ProvisioningContext provisioningContext;
	protected LoadMetadataRepositoryJob repoPreloadJob;
	IStatus couldNotResolveStatus = Status.OK_STATUS; // we haven't tried and failed
	boolean resolveWithRelaxedConstraints = false;
	boolean waitingForOtherJobs = false;
	protected RemediationOperation remediationOperation;
	private IProvisioningPlan localJRECheckPlan;

	public ProvisioningOperationWizard(ProvisioningUI ui, ProfileChangeOperation operation, Object[] initialSelections,
			LoadMetadataRepositoryJob job) {
		super();
		this.ui = ui;
		this.operation = operation;
		initializeResolutionModelElements(initialSelections);
		this.repoPreloadJob = job;
		setForcePreviousAndNextButtons(true);
		setNeedsProgressMonitor(true);
		if (operation != null) {
			provisioningContext = operation.getProvisioningContext();
		}
	}

	public void setRemediationOperation(RemediationOperation remediationOperation) {
		this.remediationOperation = remediationOperation;
	}

	public RemediationOperation getRemediationOperation() {
		return remediationOperation;
	}

	@Override
	public void addPages() {
		mainPage = createMainPage(root, planSelections);
		addPage(mainPage);
		errorPage = createErrorReportingPage();
		if (errorPage != mainPage)
			addPage(errorPage);
		remediationPage = createRemediationPage();
		if (remediationPage != null)
			addPage(remediationPage);
		resolutionPage = createResolutionPage();
		addPage(resolutionPage);
	}

	protected abstract RemediationPage createRemediationPage();

	protected abstract IResolutionErrorReportingPage createErrorReportingPage();

	protected abstract ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections);

	protected abstract ResolutionResultsWizardPage createResolutionPage();

	@Override
	public boolean performFinish() {
		return resolutionPage.performFinish();
	}

	protected LoadMetadataRepositoryJob getRepositoryPreloadJob() {
		return repoPreloadJob;
	}

	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		if (page == errorPage) {
			return mainPage;
		}
		return super.getPreviousPage(page);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		// If we are moving from the main page or error page, we may need to resolve
		// before
		// advancing.

		if (page == remediationPage) {
			try {
				getContainer().run(true, true, monitor -> {
					remediationOperation.setCurrentRemedy(remediationPage.getRemediationGroup().getCurrentRemedy());
					remediationOperation.resolveModal(monitor);
					if (getPolicy().getCheckAgainstCurrentExecutionEnvironment()) {
						this.localJRECheckPlan = ProvUI
								.toCompabilityWithCurrentJREProvisioningPlan(remediationOperation, monitor);
						if (!compatibleWithCurrentEE()) {
							couldNotResolveStatus = localJRECheckPlan.getStatus();
						}
					}
				});
			} catch (InterruptedException e) {
				// Nothing to report if thread was interrupted
			} catch (InvocationTargetException e) {
				ProvUI.handleException(e.getCause(), null, StatusManager.SHOW | StatusManager.LOG);
				couldNotResolve(null);
			}
			operation = remediationOperation;
			initializeResolutionModelElements(ElementUtils.requestToElement(
					((RemediationOperation) operation).getCurrentRemedy(), !(this instanceof UpdateWizard)));
			planChanged();
			if (getPolicy().getCheckAgainstCurrentExecutionEnvironment() && !compatibleWithCurrentEE()) {
				return errorPage;
			}
			return resolutionPage;
		} else if (page == mainPage || page == errorPage) {
			ISelectableIUsPage currentPage = (ISelectableIUsPage) page;
			// Do we need to resolve?
			if (operation == null || (operation != null && shouldRecomputePlan(currentPage))) {
				recomputePlan(getContainer(), true);
			} else {
				// the selections have not changed from an IU point of view, but we want
				// to reinitialize the resolution model elements to ensure they are up to
				// date.
				initializeResolutionModelElements(planSelections);
			}
			IStatus status = operation.getResolutionResult();
			if (getPolicy().getCheckAgainstCurrentExecutionEnvironment() && !compatibleWithCurrentEE()) {
				// skip remediation for EE compatibility issues
				return errorPage;
			}
			if (status == null || status.getSeverity() == IStatus.ERROR) {
				if (page == mainPage) {
					if (remediationOperation != null && remediationOperation.getResolutionResult() == Status.OK_STATUS
							&& remediationOperation.getRemedyConfigs().length == 1) {
						planChanged();
						return getNextPage(remediationPage);
					}
					if (remediationOperation != null
							&& remediationOperation.getResolutionResult() == Status.OK_STATUS) {
						planChanged();
						return remediationPage;
					}
					return errorPage;
				}
			} else if (status.getSeverity() == IStatus.CANCEL) {
				return page;
			} else {
				if (remediationPage != null)
					remediationPage.setPageComplete(true);
				return resolutionPage;
			}
		}
		return super.getNextPage(page);
	}

	private boolean compatibleWithCurrentEE() {
		if (operation == null || !getPolicy().getCheckAgainstCurrentExecutionEnvironment()) {
			return true;
		}
		if (localJRECheckPlan == null) {
			try {
				getContainer().run(true, true, monitor -> {
					if (!operation.hasResolved()) {
						operation.resolveModal(monitor);
					}
					if (operation.getProfileChangeRequest() != null) {
						this.localJRECheckPlan = ProvUI.toCompabilityWithCurrentJREProvisioningPlan(operation, null);
						if (!compatibleWithCurrentEE()) {
							couldNotResolveStatus = localJRECheckPlan.getStatus();
						}
					}
				});
			} catch (InvocationTargetException | InterruptedException e) {
				return false;
			}
		}
		if (localJRECheckPlan == null) {
			return true;
		}

		IStatus currentEEPlanStatus = localJRECheckPlan.getStatus();
		if (currentEEPlanStatus.getSeverity() != IStatus.ERROR) {
			return true;
		}
		return Stream.of(currentEEPlanStatus).filter(status -> status.getSeverity() == IStatus.ERROR)
				.flatMap(status -> status.isMultiStatus() ? Stream.of(status.getChildren()) : Stream.of(status))
				.filter(status -> status.getSeverity() == IStatus.ERROR)
				.flatMap(status -> status.isMultiStatus() ? Stream.of(status.getChildren()) : Stream.of(status))
				.filter(status -> status.getSeverity() == IStatus.ERROR)
				.flatMap(status -> status.isMultiStatus() ? Stream.of(status.getChildren()) : Stream.of(status))
				.map(IStatus::getMessage).noneMatch(message -> message.contains("osgi.ee")); //$NON-NLS-1$
	}

	/**
	 * The selections that drive the provisioning operation have changed. We might
	 * need to change the completion state of the resolution page.
	 */
	public void operationSelectionsChanged(ISelectableIUsPage page) {
		if (resolutionPage != null) {
			// If the page selections are different than what we may have resolved
			// against, then this page is not complete.
			boolean old = resolutionPage.isPageComplete();
			resolutionPage
					.setPageComplete(page.getCheckedIUElements() != null && page.getCheckedIUElements().length > 0);
			// If the state has truly changed, update the buttons.
			if (old != resolutionPage.isPageComplete()) {
				IWizardContainer container = getContainer();
				if (container != null && container.getCurrentPage() != null)
					getContainer().updateButtons();
			}
		}
	}

	private boolean shouldRecomputePlan(ISelectableIUsPage page) {
		boolean previouslyWaiting = waitingForOtherJobs;
		boolean previouslyCanceled = getCurrentStatus().getSeverity() == IStatus.CANCEL;
		waitingForOtherJobs = ui.hasScheduledOperations();
		return waitingForOtherJobs || previouslyWaiting || previouslyCanceled || pageSelectionsHaveChanged(page)
				|| provisioningContextChanged();
	}

	protected boolean pageSelectionsHaveChanged(ISelectableIUsPage page) {
		HashSet<IInstallableUnit> selectedIUs = new HashSet<>();
		Object[] currentSelections = page.getCheckedIUElements();
		selectedIUs.addAll(ElementUtils.elementsToIUs(currentSelections));
		HashSet<IInstallableUnit> lastIUSelections = new HashSet<>();
		if (planSelections != null)
			lastIUSelections.addAll(ElementUtils.elementsToIUs(planSelections));
		return !(selectedIUs.equals(lastIUSelections));
	}

	private boolean provisioningContextChanged() {
		ProvisioningContext currentProvisioningContext = getProvisioningContext();
		if (currentProvisioningContext == null && provisioningContext == null)
			return false;
		if (currentProvisioningContext != null && provisioningContext != null)
			return !currentProvisioningContext.equals(provisioningContext);
		// One is null and the other is not
		return true;
	}

	protected void planChanged() {
		IWizardPage currentPage = getContainer().getCurrentPage();
		if ((currentPage == null || currentPage == mainPage) && remediationPage != null && remediationOperation != null
				&& remediationOperation.getResolutionResult() == Status.OK_STATUS) {
			remediationPage.updateStatus(root, operation, planSelections);
		}
		resolutionPage.updateStatus(root, operation);
		if (errorPage != resolutionPage) {
			IUElementListRoot newRoot = shouldUpdateErrorPageModelOnPlanChange() ? root : null;
			errorPage.updateStatus(newRoot, operation);
		}
	}

	protected boolean shouldUpdateErrorPageModelOnPlanChange() {
		return errorPage != mainPage;
	}

	protected abstract void initializeResolutionModelElements(Object[] selectedElements);

	protected ProvisioningContext getProvisioningContext() {
		if (operation != null) {
			return operation.getProvisioningContext();
		}
		return new ProvisioningContext(ui.getSession().getProvisioningAgent());
	}

	public void recomputePlan(IRunnableContext runnableContext) {
		recomputePlan(runnableContext, false);
	}

	public void computeRemediationOperation(ProfileChangeOperation op, ProvisioningUI ui, IProgressMonitor monitor) {
		monitor.setTaskName(ProvUIMessages.ProvisioningOperationWizard_Remediation_Operation);
		remediationOperation = new RemediationOperation(ui.getSession(), op.getProfileChangeRequest());
		remediationOperation.resolveModal(monitor);
		monitor.done();
	}

	/**
	 * Recompute the provisioning plan based on the items in the IUElementListRoot
	 * and the given provisioning context. Report progress using the specified
	 * runnable context. This method may be called before the page is created.
	 *
	 * @param runnableContext
	 */
	public void recomputePlan(IRunnableContext runnableContext, final boolean withRemediation) {
		couldNotResolveStatus = Status.OK_STATUS;
		provisioningContext = getProvisioningContext();
		operation = null;
		localJRECheckPlan = null;
		remediationOperation = null;
		initializeResolutionModelElements(getOperationSelections());
		if (planSelections.length == 0) {
			couldNotResolve(ProvUIMessages.ResolutionWizardPage_NoSelections);
		} else {
			operation = getProfileChangeOperation(planSelections);
			operation.setProvisioningContext(provisioningContext);
			try {
				runnableContext.run(true, true, monitor -> {
					operation.resolveModal(monitor);
					if (operation.getProfileChangeRequest() != null
							&& getPolicy().getCheckAgainstCurrentExecutionEnvironment()) {
						this.localJRECheckPlan = ProvUI.toCompabilityWithCurrentJREProvisioningPlan(operation, monitor);
						if (!compatibleWithCurrentEE()) {
							couldNotResolveStatus = localJRECheckPlan.getStatus();
						}
					}
					if (withRemediation) {
						IStatus status = operation.getResolutionResult();
						if (remediationPage != null && shouldRemediate(status)) {
							computeRemediationOperation(operation, ui, monitor);
						}
					}
				});
			} catch (InterruptedException e) {
				// Nothing to report if thread was interrupted
			} catch (InvocationTargetException e) {
				ProvUI.handleException(e.getCause(), null, StatusManager.SHOW | StatusManager.LOG);
				couldNotResolve(null);
			}
		}
		planChanged();
	}

	public boolean shouldRemediate(IStatus status) {
		return status == null || status.getSeverity() == IStatus.ERROR;
	}

	/*
	 * Get the selections that drive the provisioning operation.
	 */
	protected Object[] getOperationSelections() {
		return mainPage.getCheckedIUElements();
	}

	protected abstract ProfileChangeOperation getProfileChangeOperation(Object[] elements);

	void couldNotResolve(String message) {
		if (message != null) {
			couldNotResolveStatus = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, message, null);
		} else {
			couldNotResolveStatus = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID,
					ProvUIMessages.ProvisioningOperationWizard_UnexpectedFailureToResolve, null);
		}
		StatusManager.getManager().handle(couldNotResolveStatus, StatusManager.LOG);
	}

	public IStatus getCurrentStatus() {
		if (statusOverridesOperation())
			return couldNotResolveStatus;
		if (operation != null && operation.getResolutionResult() != null) {
			if (!operation.getResolutionResult().isOK() || localJRECheckPlan == null || compatibleWithCurrentEE()) {
				return operation.getResolutionResult();
			} else if (!compatibleWithCurrentEE()) {
				return localJRECheckPlan.getStatus();
			}
		}
		return couldNotResolveStatus;
	}

	public String getDialogSettingsSectionName() {
		return getClass().getName() + "." + WIZARD_SETTINGS_SECTION; //$NON-NLS-1$
	}

	public void saveBoundsRelatedSettings() {
		IWizardPage[] pages = getPages();
		for (IWizardPage page : pages) {
			if (page instanceof ProvisioningWizardPage)
				((ProvisioningWizardPage) page).saveBoundsRelatedSettings();
		}
	}

	protected Policy getPolicy() {
		return ui.getPolicy();
	}

	protected String getProfileId() {
		return ui.getProfileId();
	}

	protected boolean shouldShowProvisioningPlanChildren() {
		return ProvUI.getQueryContext(getPolicy()).getShowProvisioningPlanChildren();
	}

	/*
	 * Overridden to start the preload job after page control creation. This allows
	 * any listeners on repo events to be set up before a batch load occurs. The job
	 * creator uses a property to indicate if the job needs scheduling (the client
	 * may have already completed the job before the UI was opened).
	 */
	@Override
	public void createPageControls(Composite pageContainer) {
		// We call this so that wizards ignore all repository eventing that occurs while
		// the wizard is
		// open. Otherwise, we can get an add event when a repository loads its
		// references that we
		// don't want to respond to. Since repo discovery events can be received
		// asynchronously by the
		// manager, the subsequent add events generated by the manager aren't guaranteed
		// to be synchronous,
		// even if our listener is synchronous. Thus, we can't fine-tune
		// the "ignore" window to a specific operation.
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=277265#c38
		ui.signalRepositoryOperationStart();
		super.createPageControls(pageContainer);
		if (repoPreloadJob != null) {
			if (repoPreloadJob.getProperty(LoadMetadataRepositoryJob.WIZARD_CLIENT_SHOULD_SCHEDULE) != null) {
				// job has not been scheduled. Set a listener so we can report accumulated
				// errors and
				// schedule it.
				repoPreloadJob.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent e) {
						asyncReportLoadFailures();
					}
				});
				repoPreloadJob.schedule();
			} else {
				// job has been scheduled, might already be done.
				if (repoPreloadJob.getState() == Job.NONE) {
					// job is done, report failures found so far
					// do it asynchronously since we are in the middle of creation
					asyncReportLoadFailures();
				} else {
					// job is waiting, sleeping, running, report failures when
					// it's done
					repoPreloadJob.addJobChangeListener(new JobChangeAdapter() {

						@Override
						public void done(IJobChangeEvent e) {
							asyncReportLoadFailures();
						}

					});
				}

			}
		}
	}

	@Override
	public void dispose() {
		ui.signalRepositoryOperationComplete(null, false);
		super.dispose();
	}

	void asyncReportLoadFailures() {
		if (repoPreloadJob != null && getShell() != null && !getShell().isDisposed()) {
			getShell().getDisplay().asyncExec(() -> {
				if (PlatformUI.isWorkbenchRunning() && getShell() != null && !getShell().isDisposed())
					repoPreloadJob.reportAccumulatedStatus();
			});
		}
	}

	/*
	 * Return a boolean indicating whether the wizard's current status should
	 * override any detail reported by the operation.
	 */
	public boolean statusOverridesOperation() {
		return operation != null && operation.getResolutionResult() != null
				&& operation.getResolutionResult().getSeverity() < IStatus.ERROR
				&& (localJRECheckPlan != null && !compatibleWithCurrentEE());
	}

	public void setRelaxedResolution(boolean value) {
		this.resolveWithRelaxedConstraints = value;
	}

	public boolean getRelaxedResoltion() {
		return this.resolveWithRelaxedConstraints;
	}
}
