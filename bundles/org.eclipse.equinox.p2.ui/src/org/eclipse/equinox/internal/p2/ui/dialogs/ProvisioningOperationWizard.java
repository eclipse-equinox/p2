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
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
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

	protected ProvisioningUI ui;
	protected IUElementListRoot root;
	protected ProfileChangeOperation operation;
	protected Object[] planSelections;
	protected ISelectableIUsPage mainPage;
	protected IResolutionErrorReportingPage errorPage;
	protected ResolutionResultsWizardPage resolutionPage;
	private ProvisioningContext provisioningContext;
	protected PreloadMetadataRepositoryJob repoPreloadJob;
	boolean couldNotResolve;

	boolean waitingForOtherJobs = false;

	public ProvisioningOperationWizard(ProvisioningUI ui, ProfileChangeOperation operation, Object[] initialSelections, PreloadMetadataRepositoryJob job) {
		super();
		this.ui = ui;
		initializeResolutionModelElements(initialSelections);
		this.operation = operation;
		this.repoPreloadJob = job;
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
		errorPage = createErrorReportingPage();
		if (errorPage != mainPage)
			addPage(errorPage);
		resolutionPage = createResolutionPage();
		addPage(resolutionPage);
	}

	protected abstract IResolutionErrorReportingPage createErrorReportingPage();

	protected abstract ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections);

	protected abstract ResolutionResultsWizardPage createResolutionPage();

	public boolean performFinish() {
		return resolutionPage.performFinish();
	}

	protected PreloadMetadataRepositoryJob getRepositoryPreloadJob() {
		return repoPreloadJob;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public IWizardPage getNextPage(IWizardPage page) {
		// If we are moving from the main page or error page, we may need to resolve before
		// advancing.
		if (page == mainPage || page == errorPage) {
			ISelectableIUsPage currentPage = (ISelectableIUsPage) page;
			// Do we need to resolve?
			if (operation == null || (operation != null && shouldRecomputePlan(currentPage))) {
				recomputePlan(getContainer());
			} else {
				// the selections have not changed from an IU point of view, but we want
				// to reinitialize the resolution model elements to ensure they are up to
				// date.
				initializeResolutionModelElements(planSelections);
			}
			IStatus status = operation.getResolutionResult();
			if (status == null || status.getSeverity() == IStatus.ERROR) {
				return errorPage;
			} else if (status.getSeverity() == IStatus.CANCEL) {
				return page;
			}
		}
		return super.getNextPage(page);
	}

	private boolean shouldRecomputePlan(ISelectableIUsPage page) {
		boolean previouslyWaiting = waitingForOtherJobs;
		boolean previouslyCanceled = getCurrentStatus().getSeverity() == IStatus.CANCEL;
		waitingForOtherJobs = ui.hasScheduledOperations();
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
		errorPage.updateStatus(root, operation);
		if (errorPage != resolutionPage) {
			resolutionPage.updateStatus(root, operation);
		}
	}

	protected abstract void initializeResolutionModelElements(Object[] selectedElements);

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
		couldNotResolve = false;
		operation = null;
		provisioningContext = getProvisioningContext();
		planSelections = ProvisioningOperationWizard.this.mainPage.getCheckedIUElements();
		initializeResolutionModelElements(planSelections);
		if (planSelections.length == 0) {
			couldNotResolve(ProvUIMessages.ResolutionWizardPage_NoSelections);
		} else {
			operation = getProfileChangeOperation(planSelections);
			try {
				runnableContext.run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						operation.resolveModal(monitor);
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

	protected abstract ProfileChangeOperation getProfileChangeOperation(Object[] elements);

	void couldNotResolve(String message) {
		couldNotResolve = true;
		if (message != null) {
			IStatus status = new MultiStatus(ProvUIActivator.PLUGIN_ID, IStatusCodes.UNEXPECTED_NOTHING_TO_DO, message, null);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
	}

	public IStatus getCurrentStatus() {
		if (operation != null && operation.getResolutionResult() != null)
			return operation.getResolutionResult();
		// we haven't resolved yet, assume all is well.
		return Status.OK_STATUS;
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

	protected Policy getPolicy() {
		return ui.getPolicy();
	}

	protected String getProfileId() {
		return ui.getProfileId();
	}

	protected String getRootMarkerKey() {
		return ui.getPolicy().getQueryContext().getVisibleInstalledIUProperty();
	}
}
