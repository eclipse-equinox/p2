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
package org.eclipse.equinox.internal.provisional.p2.ui.policy;

import java.net.URI;
import org.eclipse.equinox.internal.p2.ui.DefaultMetadataURLValidator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.RepositoryManipulationPage;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Provides a repository manipulator that interprets URLs as colocated
 * artifact and metadata repositories.  If a preference id has been
 * set, the manipulator will open a pref page to manipulate sites.  If it has
 * not been set, then a dialog will be opened.
 * 
 * @since 3.5
 */

public class ColocatedRepositoryManipulator extends RepositoryManipulator {

	Policy policy;
	String prefPageId = null;

	public ColocatedRepositoryManipulator(Policy policy, String prefPageId) {
		this.policy = policy;
		this.prefPageId = prefPageId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getManipulatorButtonLabel()
	 */
	public String getManipulatorButtonLabel() {
		return ProvUIMessages.ColocatedRepositoryManipulator_ManageSites;

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#manipulateRepositories(org.eclipse.swt.widgets.Shell)
	 */
	public boolean manipulateRepositories(Shell shell) {
		if (prefPageId != null) {
			PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(shell, prefPageId, null, null);
			dialog.open();
		} else {
			TitleAreaDialog dialog = new TitleAreaDialog(shell) {
				RepositoryManipulationPage page;

				protected Control createDialogArea(Composite parent) {
					page = new RepositoryManipulationPage();
					page.setPolicy(policy);
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

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getAddOperation(java.net.URI)
	 */
	public AddRepositoryOperation getAddOperation(URI repoLocation) {
		return new AddColocatedRepositoryOperation(getAddOperationLabel(), repoLocation);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getAddOperationLabel()
	 */
	public String getAddOperationLabel() {
		return ProvUIMessages.ColocatedRepositoryManipulator_AddSiteOperationLabel;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getKnownRepositories()
	 */
	public URI[] getKnownRepositories() {
		try {
			return ProvisioningUtil.getMetadataRepositories(policy.getQueryContext().getMetadataRepositoryFlags());
		} catch (ProvisionException e) {
			return new URI[0];
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getRemoveOperation(java.net.URI[])
	 */
	public RemoveRepositoryOperation getRemoveOperation(URI[] reposToRemove) {
		return new RemoveColocatedRepositoryOperation(getRemoveOperationLabel(), reposToRemove);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getRemoveOperationLabel()
	 */
	public String getRemoveOperationLabel() {
		return ProvUIMessages.ColocatedRepositoryManipulator_RemoveSiteOperationLabel;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getURLValidator(org.eclipse.swt.widgets.Shell)
	 */
	public RepositoryLocationValidator getRepositoryLocationValidator(Shell shell) {
		DefaultMetadataURLValidator validator = new DefaultMetadataURLValidator();
		validator.setKnownRepositoriesFlag(policy.getQueryContext().getMetadataRepositoryFlags());
		return validator;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getManipulatorLinkLabel()
	 */
	public String getManipulatorLinkLabel() {
		return ProvUIMessages.ColocatedRepositoryManipulator_GotoPrefs;

	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getSiteNotFoundCorrectionString()
	 */
	public String getRepositoryNotFoundInstructionString() {
		return ProvUIMessages.ColocatedRepositoryManipulator_SiteNotFoundDescription;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getManipulatorInstructionString()
	 */
	public String getManipulatorInstructionString() {
		return ProvUIMessages.ColocatedRepositoryManipulator_NoContentExplanation;
	}

}
