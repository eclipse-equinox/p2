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
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.ui.ProvUIImages;
import org.eclipse.jface.wizard.Wizard;

/**
 * @since 3.4
 */
public class RevertProfileWizard extends Wizard {

	RevertProfileWizardPage page;
	Profile profile;

	public RevertProfileWizard(Profile profile) {
		super();
		setWindowTitle(ProvSDKMessages.RevertDialog_Title);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_REVERT));
		this.profile = profile;
	}

	public void addPages() {
		page = new RevertProfileWizardPage(profile);
		addPage(page);
	}

	public boolean performFinish() {
		return page.performFinish();
	}

}
