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
import org.eclipse.equinox.internal.p2.ui.dialogs.RevertProfileWizardPage;
import org.eclipse.equinox.p2.ui.ProvUIImages;
import org.eclipse.equinox.p2.ui.query.IProvElementQueryProvider;
import org.eclipse.jface.wizard.Wizard;

/**
 * @since 3.4
 */
public class RevertWizard extends Wizard {

	RevertProfileWizardPage page;
	String profileId;
	IProvElementQueryProvider queryProvider;

	public RevertWizard(String profileId, IProvElementQueryProvider queryProvider) {
		super();
		setWindowTitle(ProvUIMessages.RevertDialog_Title);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_REVERT));
		this.profileId = profileId;
		this.queryProvider = queryProvider;
	}

	public void addPages() {
		page = new RevertProfileWizardPage(profileId, queryProvider);
		addPage(page);
	}

	public boolean performFinish() {
		return page.performFinish();
	}

}
