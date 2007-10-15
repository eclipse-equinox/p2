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

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.ui.ProvUIActivator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * A RepositoryGroup is a reusable UI component that allows repository
 * attributes to be displayed and edited in different UI dialogs.
 * 
 * @since 3.4
 * 
 */
public class RepositoryGroup {

	private Composite composite;
	Text name;
	Text url;
	IRepository repository;

	public RepositoryGroup(Composite parent, IRepository repository, ModifyListener listener) {
		Assert.isNotNull(repository);
		this.repository = repository;
		createGroupComposite(parent, listener);
	}

	protected Composite createGroupComposite(final Composite parent, ModifyListener listener) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		comp.setLayout(layout);
		GridData data = new GridData();
		data.widthHint = 350;
		comp.setLayoutData(data);

		Label nameLabel = new Label(comp, SWT.NONE);
		nameLabel.setText(ProvUIMessages.RepositoryGroup_RepositoryNameFieldLabel);

		name = new Text(comp, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		name.setLayoutData(data);
		name.setEditable(repository.isModifiable());

		Label urlLabel = new Label(comp, SWT.NONE);
		urlLabel.setText(ProvUIMessages.RepositoryGroup_RepositoryURLFieldLabel);
		url = new Text(comp, SWT.BORDER);
		url.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		url.setEditable(repository.isModifiable());

		initializeFields();
		return comp;
	}

	private void initializeFields() {
		if (repository == null) {
			url.setText("http://"); //$NON-NLS-1$
		} else {
			url.setText(repository.getLocation().toExternalForm());
			name.setText(repository.getName());
		}
	}

	public IStatus verify() {
		if (url.getText().trim().length() == 0) {
			return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, ProvUIMessages.RepositoryGroup_URLRequired, null);
		}
		return new Status(IStatus.OK, ProvUIActivator.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$

	}

	/**
	 * Get the url string as shown in the dialog.
	 * 
	 * @return the String representation of the URL.
	 */
	public String getURLString() {
		return url.getText().trim();
	}

	public Composite getComposite() {
		return composite;
	}

	protected IRepository getRepository() {
		return repository;
	}
}
