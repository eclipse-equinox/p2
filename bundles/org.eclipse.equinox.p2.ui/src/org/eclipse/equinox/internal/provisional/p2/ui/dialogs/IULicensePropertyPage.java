/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.IUPropertyPage;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

/**
 * PropertyPage that shows an IU's properties
 * 
 * @since 3.4
 */
public class IULicensePropertyPage extends IUPropertyPage {

	protected Control createIUPage(Composite parent, IInstallableUnit iu) {
		String licenseText = iu.getProperty(IInstallableUnit.PROP_LICENSE);
		if (licenseText != null && licenseText.length() > 0) {
			Text text = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.WRAP);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = computeWidthLimit(text, 80);
			text.setLayoutData(gd);
			text.setText(licenseText);
			text.setEditable(false);
			return text;
		}
		Label label = new Label(parent, SWT.NULL);
		label.setText(ProvUIMessages.IULicensePropertyPage_NoLicense);
		return label;

	}
}
