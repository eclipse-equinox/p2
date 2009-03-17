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
package org.eclipse.equinox.p2.examples.rcp.sdkbundlevisibility.p2;

import org.eclipse.equinox.internal.provisional.p2.ui.IProvHelpContextIds;
import org.eclipse.equinox.internal.provisional.p2.ui.QueryableMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.InstallWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

/**
 * InstallNewSoftwareHandler invokes the install wizard
 * 
 * @since 3.5
 */
public class InstallNewSoftwareHandler extends PreloadingRepositoryHandler {

	/**
	 * The constructor.
	 */
	public InstallNewSoftwareHandler() {
		super();
	}

	protected void doExecute(String profileId, QueryableMetadataRepositoryManager manager) {
		InstallWizard wizard = new InstallWizard(Policy.getDefault(), profileId, null, null, manager);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.INSTALL_WIZARD);

		dialog.open();
	}

	protected boolean waitForPreload() {
		// If there is no way for the user to manipulate repositories,
		// then we may as well wait for existing repos to load so that
		// content is available.  If the user can manipulate the
		// repositories, then we don't wait, because we don't know which
		// ones they want to work with.
		return Policy.getDefault().getRepositoryManipulator() == null;
	}
}
