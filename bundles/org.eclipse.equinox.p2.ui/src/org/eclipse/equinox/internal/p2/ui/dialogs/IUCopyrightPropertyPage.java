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
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.net.MalformedURLException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.metadata.ICopyright;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * PropertyPage that shows an IU's copyright
 * 
 * @since 3.4
 */
public class IUCopyrightPropertyPage extends IUPropertyPage {

	protected Control createIUPage(Composite parent, IInstallableUnit iu) {
		// Get the copyright in the current locale
		final ICopyright copyright = iu.getCopyright(null);
		if (copyright != null && copyright.getBody().length() > 0) {
			Composite composite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			composite.setLayout(layout);

			Text text = new Text(composite, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.WRAP);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, false, true);
			gd.widthHint = computeWidthLimit(text, 80);
			gd.grabExcessVerticalSpace = true;
			text.setLayoutData(gd);
			text.setText(copyright.getBody());
			text.setEditable(false);

			// If an URL was specified, provide a link to it
			String filename = (copyright.getLocation() != null) ? copyright.getLocation().getPath() : null;
			if (filename != null && (filename.endsWith(".htm") || filename.endsWith(".html"))) { //$NON-NLS-1$ //$NON-NLS-2$
				Label label = new Label(composite, SWT.NONE);
				label.setText(ProvUIMessages.IUCopyrightPropertyPage_ViewLinkLabel);
				// Create a link to the copyright URL
				Link link = new Link(composite, SWT.LEFT | SWT.WRAP);
				link.setText(NLS.bind("<a>{0}</a>", URIUtil.toUnencodedString(copyright.getLocation()))); //$NON-NLS-1$
				gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
				gd.widthHint = computeWidthLimit(link, 80);
				link.setLayoutData(gd);
				link.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						try {
							showURL(copyright.getLocation().toURL());
						} catch (MalformedURLException e1) {
							//cannot show this URL
						}
					}
				});
			}

			return composite;
		}
		Label label = new Label(parent, SWT.NULL);
		label.setText(ProvUIMessages.IUCopyrightPropertyPage_NoCopyright);
		return label;

	}
}
