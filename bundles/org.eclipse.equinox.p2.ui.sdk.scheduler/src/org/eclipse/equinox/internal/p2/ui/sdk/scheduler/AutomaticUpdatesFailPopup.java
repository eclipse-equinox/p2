/*******************************************************************************
 * Copyright (c) 2023 Spirent Communications and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *		Vasili Gulevich (Spirent Communications) - initial implementation, Bug #254
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * UpdatesFailPopup is an async popup dialog for notifying the user of updates
 * that can't be installed.
 *
 */
class AutomaticUpdatesFailPopup extends UpdatesPopup {

	public AutomaticUpdatesFailPopup(Shell parentShell) {
		super(parentShell, AutomaticUpdateMessages.AutomaticUpdatesFailPopup_ClickToReviewUpdates);
	}

	@Override
	protected Composite createDialogArea(Composite parent) {
		Composite result = super.createDialogArea(parent);
		createConfigureSection(result);
		return result;
	}

	private void createConfigureSection(Composite parent) {
		Link remindLink = new Link(parent, SWT.MULTI | SWT.WRAP | SWT.RIGHT);
		remindLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(),
					PreferenceConstants.PREF_PAGE_AUTO_UPDATES, null, null);
			dialog.open();

		}));
		remindLink.setText(AutomaticUpdateMessages.AutomaticUpdatesPopup_PrefLinkOnly);
		remindLink.setLayoutData(new GridData(GridData.FILL_BOTH));
	}
}
