/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.Iterator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * PropertyPage that shows an IU's license
 * 
 * @since 3.4
 */
public class IULicensePropertyPage extends IUPropertyPage {

	protected Control createIUPage(Composite parent, IInstallableUnit iu) {
		// Get the license in the default locale
		Iterator<ILicense> licenses = iu.getLicenses(null).iterator();
		final ILicense license = licenses.hasNext() ? licenses.next() : null;
		//FIXME
		if (license != null && license.getBody().length() > 0) {
			Composite composite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			composite.setLayout(layout);

			Text text = new Text(composite, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.WRAP);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.widthHint = computeWidthLimit(text, 80);
			gd.heightHint = computeHeightLimit(text, 20);
			text.setLayoutData(gd);
			text.setText(license.getBody());
			text.setEditable(false);

			// If an URL was specified, provide a link to it
			String filename = (license.getLocation() != null) ? license.getLocation().getPath() : null;
			if (filename != null && (filename.endsWith(".htm") || filename.endsWith(".html"))) { //$NON-NLS-1$ //$NON-NLS-2$
				Label label = new Label(composite, SWT.NONE);
				label.setText(ProvUIMessages.IULicensePropertyPage_ViewLicenseLabel);
				// Create a link to the license URL
				Link link = new Link(composite, SWT.LEFT | SWT.WRAP);
				link.setText(NLS.bind("<a>{0}</a>", URIUtil.toUnencodedString(license.getLocation()))); //$NON-NLS-1$
				gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
				gd.widthHint = computeWidthLimit(link, 80);
				link.setLayoutData(gd);
				link.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						try {
							showURL(license.getLocation().toURL());
						} catch (Exception e1) {
							//can't browse invalid location
						}
					}
				});
			}

			return composite;
		}
		Label label = new Label(parent, SWT.NULL);
		label.setText(ProvUIMessages.IULicensePropertyPage_NoLicense);
		return label;

	}
}
