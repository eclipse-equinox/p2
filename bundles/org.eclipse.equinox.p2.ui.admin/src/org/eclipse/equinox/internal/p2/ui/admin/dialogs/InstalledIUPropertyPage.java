/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin.dialogs;

import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.InstalledIUElement;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * PropertyPage that shows an IU's properties
 * 
 * @since 3.4
 */
public class InstalledIUPropertyPage extends PropertyPage {

	private IUProfilePropertiesGroup iuGroup;

	protected Control createContents(Composite parent) {
		Object element = getElement();
		if (!(element instanceof InstalledIUElement)) {
			Label label = new Label(parent, SWT.DEFAULT);
			label.setText(ProvAdminUIMessages.InstalledIUPropertyPage_NoInfoAvailable);
			return label;
		}
		iuGroup = new IUProfilePropertiesGroup(parent, (InstalledIUElement) element, new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				// not editable
			}
		});
		Dialog.applyDialogFont(iuGroup.getComposite());
		return iuGroup.getComposite();
	}

	public boolean performOk() {
		return true;
	}
}
