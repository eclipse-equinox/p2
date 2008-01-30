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
import org.eclipse.equinox.internal.p2.ui.dialogs.IUPropertyPage;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

/**
 * PropertyPage that shows an IU's properties
 * 
 * @since 3.4
 */
public class IUCopyrightPropertyPage extends IUPropertyPage {

	protected Control createIUPage(Composite parent, IInstallableUnit iu) {
		String copyrightText = iu.getProperty(IInstallableUnit.PROP_COPYRIGHT);
		if (copyrightText != null && copyrightText.length() > 0) {
			Text text = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.WRAP);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = computeWidthLimit(text, 80);
			text.setLayoutData(gd);
			text.setText(copyrightText);
			text.setEditable(false);
			return text;
		}
		Label label = new Label(parent, SWT.NULL);
		label.setText(ProvUIMessages.IUCopyrightPropertyPage_NoCopyright);
		return label;

	}
}
