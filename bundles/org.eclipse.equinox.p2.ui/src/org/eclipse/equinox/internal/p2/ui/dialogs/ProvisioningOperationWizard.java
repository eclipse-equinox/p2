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
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
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
	private ProvisioningPlan plan;

	public ProvisioningOperationWizard(Policy policy, String profileId, IUElementListRoot root, Object[] initialSelections, ProvisioningPlan initialPlan) {
		super();
		this.policy = policy;
		this.profileId = profileId;
		this.root = root;
		this.plan = initialPlan;
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
		if (plan != null && planSelections != null) {
			resolutionPage = createResolutionPage(makeResolutionElementRoot(planSelections), plan);
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

	protected abstract ResolutionWizardPage createResolutionPage(IUElementListRoot input, ProvisioningPlan initialPlan);

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
				if (mainPageSelectionsHaveChanged()) {
					// any initial plan that was passed in is no longer valid, no need to hang on to it
					plan = null;
					planSelections = mainPage.getCheckedIUElements();
					resolutionPage.recomputePlan(makeResolutionElementRoot(planSelections));
					planChanged();
				}
			} else {
				if (plan != null && mainPageSelectionsHaveChanged())
					plan = null;
				resolutionPage = createResolutionPage(makeResolutionElementRoot(mainPage.getCheckedIUElements()), plan);
				planChanged();
				addPage(resolutionPage);
			}
			return resolutionPage;
		}
		return null;
	}

	private boolean mainPageSelectionsHaveChanged() {
		HashSet selectedIUs = new HashSet();
		selectedIUs.addAll(Arrays.asList(ElementUtils.elementsToIUs(mainPage.getCheckedIUElements())));
		HashSet lastIUSelections = new HashSet();
		lastIUSelections.addAll(Arrays.asList(ElementUtils.elementsToIUs(planSelections)));
		return !(selectedIUs.equals(lastIUSelections));
	}

	protected void planChanged() {
		// hook for subclasses.  Default is to do nothing
	}

	protected abstract IUElementListRoot makeResolutionElementRoot(Object[] selectedElements);
}
