/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIMessages;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * PropertyPage that shows an IU's properties
 *
 * @since 3.4
 */
public class ProfilePropertyPage extends PropertyPage {

	private ProfileGroup profileGroup;

	@Override
	protected Control createContents(Composite parent) {
		IProfile profile = ProvUI.getAdapter(getElement(), IProfile.class);
		if (profile == null) {
			Label label = new Label(parent, SWT.DEFAULT);
			label.setText(ProvAdminUIMessages.No_Property_Item_Selected);
		}
		// Assume that we do not edit profile properties for now
		noDefaultAndApplyButton();

		profileGroup = new ProfileGroup(parent, profile, null);
		Dialog.applyDialogFont(profileGroup.getComposite());
		return profileGroup.getComposite();
	}
}
