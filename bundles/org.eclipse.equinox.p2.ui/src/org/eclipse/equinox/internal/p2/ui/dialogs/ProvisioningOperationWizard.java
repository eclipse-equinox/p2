/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.Arrays;
import java.util.HashSet;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

/**
 * Common superclass for a wizard that performs a provisioning
 * operation.
 * 
 * @since 3.5
 */
public abstract class ProvisioningOperationWizard extends Wizard {

	protected Policy policy;
	protected String profileId;
	private IUElementListRoot root;
	private Object[] planSelections;
	protected ISelectableIUsPage mainPage;
	protected ResolutionWizardPage resolutionPage;
	private ProvisioningContext provisioningContext;
	private PlannerResolutionOperation resolutionOperation;
	boolean waitingForOtherJobs = false;

	public ProvisioningOperationWizard(Policy policy, String profileId, IUElementListRoot root, Object[] initialSelections, PlannerResolutionOperation initialResolution) {
		super();
		this.policy = policy;
		this.profileId = profileId;
		this.root = root;
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
		if (resolutionOperation != null && planSelections != null) {
			resolutionPage = createResolutionPage(makeResolutionElementRoot(planSelections), resolutionOperation);
			addPage(resolutionPage);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getStartingPage()
	 */
	public IWizardPage getStartingPage() {
		// If we already had initial selections and a plan, then there is no reason to get
		// additional information on the selection page.
		if (resolutionPage != null)
			return resolutionPage;
		return super.getStartingPage();
	}

	protected abstract ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections);

	protected abstract ResolutionWizardPage createResolutionPage(IUElementListRoot input, PlannerResolutionOperation initialResolution);

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
		return super.canFinish();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == mainPage) {
			if (resolutionPage != null) {
				if (shouldRecomputePlan()) {
					// any initial plan that was passed in is no longer valid, no need to hang on to it
					resolutionOperation = null;
					// record the provisioning context so we'll know if it's different next time
					provisioningContext = getProvisioningContext();
					planSelections = mainPage.getCheckedIUElements();
					root = makeResolutionElementRoot(planSelections);
					resolutionPage.recomputePlan(root, provisioningContext, getContainer());
					planChanged();
				}
			} else {
				if (resolutionOperation != null && shouldRecomputePlan())
					resolutionOperation = null;
				provisioningContext = getProvisioningContext();
				root = makeResolutionElementRoot(mainPage.getCheckedIUElements());
				resolutionPage = createResolutionPage(root, resolutionOperation);
				if (resolutionOperation == null)
					resolutionPage.recomputePlan(root, provisioningContext, getContainer());
				planChanged();
				addPage(resolutionPage);
			}
			IStatus status = resolutionPage.getCurrentStatus();
			// Normally, resolution errors are reported on the next page.
			// But if the user canceled the resolution, we don't want to move
			// to the next page.  In the future we may have other reasons not to
			// move to the next page (providing selection quick fixes on the first
			// page, etc.)
			if (status.getSeverity() != IStatus.CANCEL)
				return resolutionPage;
		}
		return null;
	}

	private boolean shouldRecomputePlan() {
		boolean previouslyWaiting = waitingForOtherJobs;
		boolean previouslyCanceled = resolutionPage != null && resolutionPage.getCurrentStatus().getSeverity() == IStatus.CANCEL;
		waitingForOtherJobs = ProvisioningOperationRunner.hasScheduledOperationsFor(profileId);
		return waitingForOtherJobs || previouslyWaiting || previouslyCanceled || mainPageSelectionsHaveChanged() || provisioningContextChanged();
	}

	private boolean mainPageSelectionsHaveChanged() {
		HashSet selectedIUs = new HashSet();
		selectedIUs.addAll(Arrays.asList(ElementUtils.elementsToIUs(mainPage.getCheckedIUElements())));
		HashSet lastIUSelections = new HashSet();
		lastIUSelections.addAll(Arrays.asList(ElementUtils.elementsToIUs(planSelections)));
		return !(selectedIUs.equals(lastIUSelections));
	}

	private boolean provisioningContextChanged() {
		ProvisioningContext currentProvisioningContext = getProvisioningContext();
		if (currentProvisioningContext == null && provisioningContext == null)
			return false;
		if (currentProvisioningContext != null && provisioningContext != null)
			return provisioningContext.getMetadataRepositories() != getProvisioningContext().getMetadataRepositories();
		// One is null and the other is not
		return true;
	}

	protected void planChanged() {
		// hook for subclasses.  Default is to do nothing
	}

	protected abstract IUElementListRoot makeResolutionElementRoot(Object[] selectedElements);

	protected ProvisioningContext getProvisioningContext() {
		return null;
	}
}
