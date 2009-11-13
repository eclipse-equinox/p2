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
package org.eclipse.equinox.p2.ui;

import java.net.URI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.p2.operations.*;
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
 * @since 2.0
 */

public class ColocatedRepositoryManipulator extends RepositoryManipulator {

	String prefPageId = null;

	public ColocatedRepositoryManipulator(String prefPageId) {
		this.prefPageId = prefPageId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#manipulateRepositories(org.eclipse.swt.widgets.Shell)
	 */
	public boolean manipulateRepositories(Shell shell, final ProvisioningUI ui) {
		if (prefPageId != null) {
			PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(shell, prefPageId, null, null);
			dialog.open();
		} else {
			TitleAreaDialog dialog = new TitleAreaDialog(shell) {
				RepositoryManipulationPage page;

				protected Control createDialogArea(Composite parent) {
					page = new RepositoryManipulationPage();
					page.setProvisioningUI(ui);
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
	 * @see org.eclipse.equinox.p2.ui.RepositoryManipulator#getAddOperation(java.net.URI)
	 */
	public AddRepositoryJob getAddOperation(URI repoLocation, ProvisioningUI ui) {
		return new AddColocatedRepositoryJob(ProvUIMessages.ColocatedRepositoryManipulator_AddSiteOperationLabel, ui.getSession(), repoLocation);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getKnownRepositories()
	 */
	public URI[] getKnownRepositories(ProvisioningSession session) {
		return session.getMetadataRepositoryManager().getKnownRepositories(getMetadataRepositoryFlags());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.ui.RepositoryManipulator#getRemoveOperation(java.net.URI[])
	 */
	public RemoveRepositoryJob getRemoveOperation(URI[] reposToRemove, ProvisioningUI ui) {
		return new RemoveColocatedRepositoryJob(ProvUIMessages.ColocatedRepositoryManipulator_RemoveSiteOperationLabel, ui.getSession(), reposToRemove);
	}

	protected IStatus validateRepositoryLocationWithManager(ProvisioningSession session, URI location, IProgressMonitor monitor) {
		return session.getMetadataRepositoryManager().validateRepositoryLocation(location, monitor);
	}
}
