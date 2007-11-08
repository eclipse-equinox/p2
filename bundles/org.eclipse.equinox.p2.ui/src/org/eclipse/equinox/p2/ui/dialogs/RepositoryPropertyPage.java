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
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.ui.ProvUI;
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

	private IRepository repository;
	private Composite composite;
	private Text name;
	private Text url;

	protected Control createContents(Composite parent) {
		this.repository = getRepository();
		if (repository == null) {
			Label label = new Label(parent, SWT.DEFAULT);
			label.setText(ProvUIMessages.RepositoryPropertyPage_NoRepoSelected);
			return label;
		}
		if (!repository.isModifiable()) {
			noDefaultAndApplyButton();
		}

		composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData();
		data.widthHint = 350;
		composite.setLayoutData(data);

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText(ProvUIMessages.RepositoryGroup_RepositoryNameFieldLabel);

		name = new Text(composite, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		name.setLayoutData(data);
		name.setEditable(repository.isModifiable());

		Label urlLabel = new Label(composite, SWT.NONE);
		urlLabel.setText(ProvUIMessages.RepositoryGroup_RepositoryURLFieldLabel);
		url = new Text(composite, SWT.BORDER);
		url.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		url.setEditable(false);

		initializeFields();
		Dialog.applyDialogFont(composite);
		verifyComplete();
		return composite;
	}

	protected void verifyComplete() {
		if (url.getText().trim().length() == 0) {
			setValid(false);
			setErrorMessage(ProvUIMessages.RepositoryGroup_URLRequired);
		}
		setValid(true);
		setErrorMessage(null);
	}

	public boolean performOk() {
		if (repository.isModifiable()) {
			repository.setName(name.getText().trim());
		}
		return super.performOk();
	}

	private void initializeFields() {
		if (repository == null) {
			url.setText("http://"); //$NON-NLS-1$
		} else {
			url.setText(repository.getLocation().toExternalForm());
			name.setText(repository.getName());
		}
	}

	protected IRepository getRepository() {
		if (repository == null) {
			repository = (IRepository) ProvUI.getAdapter(getElement(), IRepository.class);
		}
		return repository;
	}
}
