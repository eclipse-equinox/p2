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
package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.QueryableMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.swt.widgets.Composite;

/**
 * @since 3.4
 */
public class InstallWizard extends WizardWithLicenses {

	QueryableMetadataRepositoryManager manager;

	public InstallWizard(Policy policy, String profileId, IInstallableUnit[] initialSelections, ProvisioningPlan initialPlan, QueryableMetadataRepositoryManager manager) {
		super(policy, profileId, null, initialSelections, initialPlan);
		this.manager = manager;
		setWindowTitle(ProvUIMessages.InstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_INSTALL));
	}

	public InstallWizard(Policy policy, String profileId) {
		this(policy, profileId, null, null, new QueryableMetadataRepositoryManager(policy, false));
	}

	protected ResolutionWizardPage createResolutionPage(IUElementListRoot input, ProvisioningPlan initialPlan) {
		return new InstallWizardPage(policy, profileId, input, initialPlan);
	}

	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		AvailableIUsPage page = new AvailableIUsPage(policy, profileId, manager);
		if (selections != null && selections.length > 0)
			page.setInitialSelections(selections);
		return page;

	}

	protected IUElementListRoot makeResolutionElementRoot(Object[] selectedElements) {
		IUElementListRoot elementRoot = new IUElementListRoot();
		ArrayList list = new ArrayList(selectedElements.length);
		for (int i = 0; i < selectedElements.length; i++) {
			IInstallableUnit iu = ElementUtils.getIU(selectedElements[i]);
			if (iu != null)
				list.add(new AvailableIUElement(elementRoot, iu, profileId, policy.getQueryContext().getShowProvisioningPlanChildren()));
		}
		elementRoot.setChildren(list.toArray());
		return elementRoot;
	}

	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		if (manager != null)
			manager.reportAccumulatedStatus();
	}
}
