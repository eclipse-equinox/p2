/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.ui.IStatusCodes;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * SDKPolicy defines the Eclipse SDK UI policies for the
 * p2 UI.  The policy is declared as an OSGi service in
 * the policy_component.xml file.
 * 
 * @since 3.5
 */
public class SDKPolicy extends Policy {
	public SDKPolicy() {
		setProfileChooser(new IProfileChooser() {
			public String getProfileId(Shell shell) {
				try {
					return ProvSDKUIActivator.getSelfProfileId();
				} catch (ProvisionException e) {
					return IProfileRegistry.SELF;
				}
			}
		});
		setRepositoryManipulator(new ColocatedRepositoryManipulator(this, PreferenceConstants.PREF_PAGE_SITES) {
			public String getManipulatorLinkLabel() {
				return ProvSDKMessages.ProvSDKUIActivator_SitePrefLink;
			}
		});
		setPlanValidator(new PlanValidator() {
			public boolean continueWorkingWithPlan(ProvisioningPlan plan, Shell shell) {
				if (plan == null)
					return false;
				if (plan.getStatus().getSeverity() == IStatus.CANCEL)
					return false;

				// Special case those statuses where we would never want to open a wizard
				if (plan.getStatus().getCode() == IStatusCodes.NOTHING_TO_UPDATE) {
					ProvUI.reportStatus(plan.getStatus(), StatusManager.BLOCK);
					return false;
				}

				// Allow the wizard to open if there is no error
				if (plan.getStatus().getSeverity() != IStatus.ERROR)
					return true;

				// There is an error.  Check the preference to see whether to continue.
				IPreferenceStore prefs = ProvSDKUIActivator.getDefault().getPreferenceStore();
				String openPlan = prefs.getString(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN);
				if (MessageDialogWithToggle.ALWAYS.equals(openPlan)) {
					return true;
				}
				if (MessageDialogWithToggle.NEVER.equals(openPlan)) {
					ProvUI.reportStatus(plan.getStatus(), StatusManager.SHOW | StatusManager.LOG);
					return false;
				}
				MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoCancelQuestion(shell, ProvSDKMessages.ProvSDKUIActivator_Question, ProvSDKMessages.ProvSDKUIActivator_OpenWizardAnyway, null, false, prefs, PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN);

				// Any answer but yes will stop the performance of the plan, but NO is interpreted to mean, show me the error.
				if (dialog.getReturnCode() == IDialogConstants.NO_ID)
					ProvUI.reportStatus(plan.getStatus(), StatusManager.SHOW | StatusManager.LOG);
				return dialog.getReturnCode() == IDialogConstants.YES_ID;
			}
		});
		// Start with the default query context and configure some settings
		IUViewQueryContext queryContext = new IUViewQueryContext(IUViewQueryContext.AVAILABLE_VIEW_BY_CATEGORY);
		setQueryContext(queryContext);
		ProvSDKUIActivator.getDefault().updateWithPreferences(queryContext);
	}
}
