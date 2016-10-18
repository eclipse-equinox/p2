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

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * PropertyPage that shows an IU's properties
 * 
 * @since 3.4
 */
public class IUGeneralInfoPropertyPage extends IUPropertyPage {

	protected Control createIUPage(Composite parent, IInstallableUnit iu) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		createGeneralSection(composite, iu);
		createDescriptionSection(composite, iu);
		createDocumentationSection(composite, iu);

		return composite;
	}

	private void createGeneralSection(Composite parent, IInstallableUnit iu) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		// Get general info in the default locale
		addField(composite, ProvUIMessages.IUGeneralInfoPropertyPage_NameLabel, iu.getProperty(IInstallableUnit.PROP_NAME, null));
		addField(composite, ProvUIMessages.IUGeneralInfoPropertyPage_IdentifierLabel, iu.getId());
		addField(composite, ProvUIMessages.IUGeneralInfoPropertyPage_VersionLabel, iu.getVersion().toString());
		addField(composite, ProvUIMessages.IUGeneralInfoPropertyPage_ProviderLabel, iu.getProperty(IInstallableUnit.PROP_PROVIDER, null));
		addField(composite, ProvUIMessages.IUGeneralInfoPropertyPage_ContactLabel, iu.getProperty(IInstallableUnit.PROP_CONTACT, null));

	}

	private void createDescriptionSection(Composite parent, IInstallableUnit iu) {
		// Get the iu description in the default locale
		String description = iu.getProperty(IInstallableUnit.PROP_DESCRIPTION, null);
		if (description != null && description.length() > 0) {
			Group group = new Group(parent, SWT.NONE);
			group.setText(ProvUIMessages.IUGeneralInfoPropertyPage_DescriptionLabel);
			group.setLayout(new GridLayout());
			group.setLayoutData(new GridData(GridData.FILL_BOTH));

			Text text = new Text(group, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.widthHint = computeWidthLimit(text, 80);
			gd.heightHint = 200;
			text.setEditable(false);
			text.setText(description);
			text.setLayoutData(gd);
		}

	}

	private void createDocumentationSection(Composite parent, IInstallableUnit iu) {
		String docURL = iu.getProperty(IInstallableUnit.PROP_DOC_URL);
		if (docURL != null && docURL.length() > 0) {
			final URL url;
			try {
				url = new URL(docURL);
			} catch (MalformedURLException e) {
				return;
			}
			String filename = (url != null) ? url.getFile() : null;
			if (filename != null && (filename.endsWith(".htm") || filename.endsWith(".html"))) { //$NON-NLS-1$ //$NON-NLS-2$
				// create some space
				new Label(parent, SWT.NONE);
				// Now create a link to the documentation
				Label label = new Label(parent, SWT.NONE);
				label.setText(ProvUIMessages.IUGeneralInfoPropertyPage_DocumentationLink);
				Link link = new Link(parent, SWT.LEFT | SWT.WRAP);
				link.setText(NLS.bind("<a>{0}</a>", url.toExternalForm())); //$NON-NLS-1$
				GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
				gd.widthHint = computeWidthLimit(link, 80);
				link.setLayoutData(gd);
				link.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						showURL(url);
					}
				});
			}
		}
	}

	private void addField(Composite parent, String property, String value) {
		if (value != null && value.length() > 0) {
			Label label = new Label(parent, SWT.NONE);
			label.setText(property);

			Text text = new Text(parent, SWT.WRAP | SWT.READ_ONLY);
			text.setText(value);
			// Needed to get the right color on the Mac.
			// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=258112
			text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			text.setLayoutData(gd);
		}
	}
}
