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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.core.repository.IRepositoryInfo;
import org.eclipse.equinox.p2.core.repository.IWritableRepositoryInfo;
import org.eclipse.equinox.p2.ui.ProvUIActivator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
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

	private static final String EXTENSION = "*.xml"; //$NON-NLS-1$
	private Composite composite;
	Text name;
	Text url;
	IRepositoryInfo repository;

	public RepositoryGroup(Composite parent, IRepositoryInfo repository, ModifyListener listener, boolean chooseFile, String dirPath, String fileName) {
		this.repository = repository;
		createGroupComposite(parent, listener, chooseFile, dirPath, fileName);
	}

	protected Composite createGroupComposite(final Composite parent, ModifyListener listener, final boolean chooseFile, final String dirPath, final String fileName) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		comp.setLayout(layout);
		GridData data = new GridData();
		data.widthHint = 350;
		comp.setLayoutData(data);

		Label nameLabel = new Label(comp, SWT.NONE);
		nameLabel.setText(ProvUIMessages.RepositoryGroup_RepositoryNameFieldLabel);

		name = new Text(comp, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		name.setLayoutData(data);
		name.addModifyListener(listener);
		// TODO: this may not be the right computation for determining
		// writability
		// of the name and other properties of a repository.
		boolean readOnlyInfo = (repository != null && repository.getAdapter(IWritableRepositoryInfo.class) == null);
		if (readOnlyInfo) {
			name.setEditable(false);
		}

		Label urlLabel = new Label(comp, SWT.NONE);
		urlLabel.setText(ProvUIMessages.RepositoryGroup_RepositoryURLFieldLabel);

		url = new Text(comp, SWT.BORDER);
		url.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (repository == null) {
			data = new GridData(GridData.FILL_HORIZONTAL);
			url.setLayoutData(data);
			url.addModifyListener(listener);

			// add a button for setting a local repository
			Button locationButton = new Button(comp, SWT.PUSH);
			locationButton.setText(ProvUIMessages.RepositoryGroup_Browse);
			locationButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					String path;
					if (chooseFile) {
						FileDialog dialog = new FileDialog(parent.getShell(), SWT.APPLICATION_MODAL);
						dialog.setText(ProvUIMessages.RepositoryGroup_RepositoryFile);
						dialog.setFileName(fileName);
						dialog.setFilterPath(dirPath);
						dialog.setFilterExtensions(new String[] {EXTENSION});
						path = dialog.open();
					} else {
						DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
						dialog.setMessage(ProvUIMessages.RepositoryGroup_SelectRepositoryDirectory);
						dialog.setFilterPath(dirPath);
						path = dialog.open();
					}
					if (path != null) {
						url.setText("file:" + path.toLowerCase()); //$NON-NLS-1$
					}
				}
			});

		} else {
			data = new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalSpan = 2;
			url.setLayoutData(data);
			url.setEditable(false);
		}

		initializeFields();
		return comp;
	}

	private void initializeFields() {
		if (repository == null) {
			url.setText("http://"); //$NON-NLS-1$
			name.setText(""); //$NON-NLS-1$
		} else {
			url.setText(repository.getLocation().toExternalForm());
			name.setText(repository.getName());
		}
	}

	public IStatus verify() {
		if (url.getText().trim().length() == 0) {
			return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, ProvUIMessages.RepositoryGroup_URLRequired, null);
		}
		// blank name is ok
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

	/**
	 * Get the repository name string as shown in the dialog.
	 * 
	 * @return the repository name.
	 */
	public String getRepositoryName() {
		return name.getText().trim();
	}

	public Composite getComposite() {
		return composite;
	}
}
