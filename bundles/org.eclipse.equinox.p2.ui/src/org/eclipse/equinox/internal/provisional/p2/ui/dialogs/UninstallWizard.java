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
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;

/**
 * @since 3.4
 */
public class UninstallWizard extends ProvisioningOperationWizard {

	static IUElementListRoot makeElementRoot(Object[] selectedElements, String profileId) {
		IUElementListRoot elementRoot = new IUElementListRoot();
		ArrayList list = new ArrayList(selectedElements.length);
		for (int i = 0; i < selectedElements.length; i++) {
			IInstallableUnit iu = ElementUtils.getIU(selectedElements[i]);
			if (iu != null)
				list.add(new InstalledIUElement(elementRoot, profileId, iu));
		}
		elementRoot.setChildren(list.toArray());
		return elementRoot;
	}

	public UninstallWizard(Policy policy, String profileId, IInstallableUnit[] ius, ProvisioningPlan initialPlan) {
		super(policy, profileId, makeElementRoot(ius, profileId), ius, initialPlan);
		setWindowTitle(ProvUIMessages.UninstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_UNINSTALL));
	}

	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		ISelectableIUsPage page = new SelectableIUsPage(policy, input, selections, profileId);
		page.setTitle(ProvUIMessages.UninstallIUOperationLabel);
		page.setDescription(ProvUIMessages.UninstallDialog_UninstallMessage);
		return page;
	}

	protected ResolutionWizardPage createResolutionPage(IUElementListRoot input, ProvisioningPlan plan) {
		return new UninstallWizardPage(policy, input, profileId, plan);
	}

	protected IUElementListRoot makeResolutionElementRoot(Object[] selectedElements) {
		return makeElementRoot(selectedElements, profileId);
	}
}
