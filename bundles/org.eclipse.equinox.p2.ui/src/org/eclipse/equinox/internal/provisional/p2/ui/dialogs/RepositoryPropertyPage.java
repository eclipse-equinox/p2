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

import org.eclipse.equinox.internal.provisional.p2.ui.model.IRepositoryElement;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * PropertyPage that shows a repository's properties
 * 
 * @since 3.4
 */
public class RepositoryPropertyPage extends PropertyPage {

	private IRepositoryElement repositoryElement;
	private Composite composite;
	private Text name;
	private Text url;
	private Text description;

	protected Control createContents(Composite parent) {
		this.repositoryElement = getRepositoryElement();
		if (repositoryElement == null) {
			Label label = new Label(parent, SWT.DEFAULT);
			label.setText(ProvUIMessages.RepositoryPropertyPage_NoRepoSelected);
			return label;
		}
		noDefaultAndApplyButton();

		composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData();
		data.widthHint = 350;
		composite.setLayoutData(data);

		Label urlLabel = new Label(composite, SWT.NONE);
		urlLabel.setText(ProvUIMessages.RepositoryPropertyPage_URLFieldLabel);
		url = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
		url.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText(ProvUIMessages.RepositoryPropertyPage_NameFieldLabel);
		name = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
		name.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label descriptionLabel = new Label(composite, SWT.NONE);
		descriptionLabel.setText(ProvUIMessages.RepositoryPropertyPage_DescriptionFieldLabel);
		data = new GridData();
		data.verticalAlignment = SWT.TOP;
		descriptionLabel.setLayoutData(data);
		description = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = SWT.TOP;
		data.grabExcessVerticalSpace = true;
		description.setLayoutData(data);

		initializeFields();
		Dialog.applyDialogFont(composite);
		return composite;
	}

	private void initializeFields() {
		// Shouldn't happen since we checked this before creating any controls
		if (repositoryElement == null)
			return;
		url.setText(repositoryElement.getLocation().toExternalForm());
		name.setText(repositoryElement.getName());
		description.setText(repositoryElement.getDescription());
	}

	protected IRepositoryElement getRepositoryElement() {
		if (repositoryElement == null) {
			repositoryElement = (IRepositoryElement) ProvUI.getAdapter(getElement(), IRepositoryElement.class);
		}
		return repositoryElement;
	}
}
