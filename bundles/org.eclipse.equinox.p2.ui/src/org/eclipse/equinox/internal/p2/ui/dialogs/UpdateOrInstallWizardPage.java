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
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.viewers.IUColumnConfig;

public abstract class UpdateOrInstallWizardPage extends ProfileModificationWizardPage {

	protected UpdateOrInstallWizard wizard;
	private static final int DEFAULT_COLUMN_WIDTH = 150;

	protected UpdateOrInstallWizardPage(String id, IInstallableUnit[] ius, String profileId, ProvisioningPlan plan, UpdateOrInstallWizard wizard) {
		super(id, ius, profileId, plan);
		this.wizard = wizard;
	}

	protected IUColumnConfig[] getColumnConfig() {
		initializeDialogUnits(getShell());
		int pixels = convertHorizontalDLUsToPixels(DEFAULT_COLUMN_WIDTH);
		return new IUColumnConfig[] {new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, pixels), new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, pixels), new IUColumnConfig(ProvUIMessages.ProvUI_SizeColumnTitle, IUColumnConfig.COLUMN_SIZE, pixels / 2)};
	}

	protected void checkedIUsChanged() {
		wizard.iusChanged(getCheckedIUs());
		super.checkedIUsChanged();
	}

}