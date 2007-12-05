/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.InstallWizardPage;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUIImages;
import org.eclipse.jface.wizard.Wizard;

/**
 * @since 3.4
 */
public class InstallWizard extends Wizard {

	InstallWizardPage page;
	Profile profile;
	IInstallableUnit[] ius;

	public InstallWizard(Profile profile, IInstallableUnit[] ius) {
		super();
		setWindowTitle(ProvUIMessages.InstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_INSTALL));
		this.profile = profile;
		this.ius = ius;
	}

	public void addPages() {
		page = new InstallWizardPage(ius, profile);
		addPage(page);
	}

	public boolean performFinish() {
		return page.performFinish();
	}

}
