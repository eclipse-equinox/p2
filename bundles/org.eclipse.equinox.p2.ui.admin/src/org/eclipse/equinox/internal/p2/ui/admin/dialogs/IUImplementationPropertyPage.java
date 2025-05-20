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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIMessages;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * PropertyPage that shows an IU's properties
 *
 * @since 3.4
 */
public class IUImplementationPropertyPage extends PropertyPage {

	private IUImplementationGroup iuGroup;

	@Override
	protected Control createContents(Composite parent) {
		IInstallableUnit iu = ProvUI.getAdapter(getElement(), IInstallableUnit.class);
		if (iu == null) {
			Label label = new Label(parent, SWT.DEFAULT);
			label.setText(ProvAdminUIMessages.No_Property_Item_Selected);
		}
		iuGroup = new IUImplementationGroup(parent, iu, event -> verifyComplete());
		Dialog.applyDialogFont(iuGroup.getComposite());
		verifyComplete();
		return iuGroup.getComposite();
	}

	@Override
	public boolean performOk() {
		return true;
	}

	void verifyComplete() {
		if (iuGroup == null) {
			return;
		}
		IStatus status = iuGroup.verify();
		setValid(status.isOK());
	}
}
