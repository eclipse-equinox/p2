/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Common superclass for a wizard that performs a provisioning
 * operation.
 * 
 * @since 3.5
 */
public abstract class ProvisioningOperationWizard extends Wizard {

	private static final String WIZARD_SETTINGS_SECTION = "WizardSettings"; //$NON-NLS-1$

	protected Policy policy;
	protected String profileId;
	protected IUElementListRoot root, originalRoot;
	protected PlannerResolutionOperation resolutionOperation;
	private Object[] planSelections;
	protected ISelectableIUsPage mainPage;
	protected IResolutionErrorReportingPage errorPage;
	protected ResolutionResultsWizardPage resolutionPage;
	private ProvisioningContext provisioningContext;
	boolean couldNotResolve;

	boolean waitingForOtherJobs = false;

	public ProvisioningOperationWizard(Policy policy, String profileId, IUElementListRoot root, Object[] initialSelections, PlannerResolutionOperation initialResolution) {
		super();
		this.policy = policy;
		this.profileId = profileId;
		this.root = root;
		this.originalRoot = root;
		this.resolutionOperation = initialResolution;
		if (initialSelections == null)
			planSelections = new Object[0];
		else
			planSelections = initialSelections;
		setForcePreviousAndNextButtons(true);
		setNeedsProgressMonitor(true);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		mainPage = createMainPage(root, planSelections);
		addPage(mainPage);
	}

	protected abstract IResolutionErrorReportingPage getErrorReportingPage();

	protected abstract ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections);

	protected abstract ResolutionResultsWizardPage createResolutionPage();

	public boolean performFinish() {
		return resolutionPage.performFinish();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	public boolean canFinish() {
		if (resolutionPage == null)
			return false;
		if (!super.canFinish())
			return false;
		// Special case.  The error reporting page has to be complete in
		// order to press next and perform a resolution.  But that doesn't
		// mean the wizard can finish.
		if (resolutionOperation != null) {
			int severity = resolutionOperation.getResolutionResult().getSummaryStatus().getSeverity();
			return severity != IStatus.ERROR && severity != IStatus.CANCEL;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == mainPage || page == errorPage) {
			ISelectableIUsPage currentPage = (ISelectableIUsPage) page;
			// Do we need to resolve?
			boolean weResolved = false;
			if (resolutionOperation == null || (resolutionOperation != null && shouldRecomputePlan(currentPage))) {
				resolutionOperation = null;
				provisioningContext = getProvisioningContext();
				planSelections = currentPage.getCheckedIUElements();
				root = makeResolutionElementRoot(planSelections);
				recomputePlan(getContainer());
				planChanged();
				weResolved = true;
			} else {
				planSelections = currentPage.getCheckedIUElements();
				root = makeResolutionElementRoot(planSelections);
			}
			return selectNextPage(page, getCurrentStatus(), weResolved);
		}
		return super.getNextPage(page);
	}

	protected IWizardPage selectNextPage(IWizardPage currentPage, IStatus status, boolean hasResolved) {
		// We have already established before calling this method that the
		// current page is either the main page or the error page.  
		if (status.getSeverity() == IStatus.CANCEL)
			return currentPage;
		else if (status.getSeverity() == IStatus.ERROR) {
			if (errorPage == null)
				errorPage = getErrorReportingPage();
			if (currentPage == errorPage) {
				updateErrorPageStatus(errorPage);
				return null;
			}
			showingErrorPage();
			return errorPage;
		} else {
			if (resolutionPage == null) {
				resolutionPage = createResolutionPage();
				addPage(resolutionPage);
			}
			// need to clear any previous error status reported so that traversing
			// back to the error page will not show the error
			if (currentPage instanceof IResolutionErrorReportingPage)
				updateErrorPageStatus((IResolutionErrorReportingPage) currentPage);
			return resolutionPage;
		}
	}

	/**
	 * The error page is being shown for the first time given the
	 * current plan.  Update any information needed before showing
	 * the page.
	 */
	protected void showingErrorPage() {
		// default is to do nothing
	}

	private boolean shouldRecomputePlan(ISelectableIUsPage page) {
		boolean previouslyWaiting = waitingForOtherJobs;
		boolean previouslyCanceled = getCurrentStatus().getSeverity() == IStatus.CANCEL;
		waitingForOtherJobs = ProvisioningOperationRunner.hasScheduledOperationsFor(profileId);
		return waitingForOtherJobs || previouslyWaiting || previouslyCanceled || pageSelectionsHaveChanged(page) || provisioningContextChanged();
	}

	private boolean pageSelectionsHaveChanged(ISelectableIUsPage page) {
		HashSet selectedIUs = new HashSet();
		Object[] currentSelections = page.getCheckedIUElements();
		selectedIUs.addAll(Arrays.asList(ElementUtils.elementsToIUs(currentSelections)));
		HashSet lastIUSelections = new HashSet();
		lastIUSelections.addAll(Arrays.asList(ElementUtils.elementsToIUs(planSelections)));
		return !(selectedIUs.equals(lastIUSelections));
	}

	private boolean provisioningContextChanged() {
		ProvisioningContext currentProvisioningContext = getProvisioningContext();
		if (currentProvisioningContext == null && provisioningContext == null)
			return false;
		if (currentProvisioningContext != null && provisioningContext != null)
			return !Arrays.equals(provisioningContext.getMetadataRepositories(), currentProvisioningContext.getMetadataRepositories());
		// One is null and the other is not
		return true;
	}

	protected void planChanged() {
		if (resolutionOperation != null) {
			IStatus status = resolutionOperation.getResolutionResult().getSummaryStatus();
			if (status.getSeverity() != IStatus.ERROR && status.getSeverity() != IStatus.CANCEL) {
				if (resolutionPage != null)
					resolutionPage.updateStatus(root, resolutionOperation);
				else {
					resolutionPage = createResolutionPage();
					addPage(resolutionPage);
				}
			}
		}
	}

	protected abstract IUElementListRoot makeResolutionElementRoot(Object[] selectedElements);

	protected ProvisioningContext getProvisioningContext() {
		return null;
	}

	/**
	 * Recompute the provisioning plan based on the items in the IUElementListRoot and the given provisioning context.
	 * Report progress using the specified runnable context.  This method may be called before the page is created.
	 * 
	 * @param runnableContext
	 */
	public void recomputePlan(IRunnableContext runnableContext) {
		final Object[] elements = root.getChildren(root);
		couldNotResolve = false;
		try {
			if (elements.length == 0) {
				couldNotResolve(ProvUIMessages.ResolutionWizardPage_NoSelections);
			} else
				runnableContext.run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						resolutionOperation = null;
						MultiStatus status = PlanAnalyzer.getProfileChangeAlteredStatus();
						ProfileChangeRequest request = computeProfileChangeRequest(elements, status, monitor);
						if (request != null) {
							resolutionOperation = new PlannerResolutionOperation(ProvUIMessages.ProfileModificationWizardPage_ResolutionOperationLabel, profileId, request, provisioningContext, status, false);
							try {
								resolutionOperation.execute(monitor);
							} catch (ProvisionException e) {
								ProvUI.handleException(e, null, StatusManager.SHOW | StatusManager.LOG);
								couldNotResolve(null);
							}
						}
					}
				});
		} catch (InterruptedException e) {
			// Nothing to report if thread was interrupted
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), null, StatusManager.SHOW | StatusManager.LOG);
			couldNotResolve(null);
		}
		if (errorPage == null)
			errorPage = getErrorReportingPage();
		updateErrorPageStatus(errorPage);
	}

	void updateErrorPageStatus(IResolutionErrorReportingPage page) {
		page.updateStatus(originalRoot, resolutionOperation);
	}

	void couldNotResolve(String message) {
		resolutionOperation = null;
		couldNotResolve = true;
		if (message != null) {
			IStatus status = new MultiStatus(ProvUIActivator.PLUGIN_ID, IStatusCodes.UNEXPECTED_NOTHING_TO_DO, message, null);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
	}

	public IStatus getCurrentStatus() {
		if (couldNotResolve || resolutionOperation == null) {
			return PlanAnalyzer.getStatus(IStatusCodes.UNEXPECTED_NOTHING_TO_DO, null);
		}
		return resolutionOperation.getResolutionResult().getSummaryStatus();
	}

	public String getDialogSettingsSectionName() {
		return getClass().getName() + "." + WIZARD_SETTINGS_SECTION; //$NON-NLS-1$
	}

	public void saveBoundsRelatedSettings() {
		IWizardPage[] pages = getPages();
		for (int i = 0; i < pages.length; i++) {
			if (pages[i] instanceof ProvisioningWizardPage)
				((ProvisioningWizardPage) pages[i]).saveBoundsRelatedSettings();
		}
	}

	protected abstract ProfileChangeRequest computeProfileChangeRequest(Object[] checkedElements, MultiStatus additionalStatus, IProgressMonitor monitor);

}
