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
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.LicenseManager;
import org.eclipse.equinox.p2.ui.ProvUIImages;
import org.eclipse.equinox.p2.ui.query.IQueryProvider;

/**
 * @since 3.4
 */
public class UpdateWizard extends UpdateOrInstallWizard {

	IQueryProvider queryProvider;

	public UpdateWizard(String profileId, IInstallableUnit[] ius, LicenseManager licenseManager, IQueryProvider queryProvider) {
		super(profileId, ius, licenseManager);
		this.queryProvider = queryProvider;
		setWindowTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_UPDATE));
	}

	protected UpdateOrInstallWizardPage createMainPage(String profileId, IInstallableUnit[] ius) {
		return new UpdateWizardPage(ius, profileId, queryProvider, this);
	}
}
