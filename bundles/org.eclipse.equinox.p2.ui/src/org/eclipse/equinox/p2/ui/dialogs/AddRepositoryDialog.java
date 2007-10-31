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

import java.net.URL;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.ui.ProvUIActivator;
import org.eclipse.equinox.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.p2.ui.operations.ProvisioningOperation;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * Abstract dialog class for adding repositories of different types. This class
 * assumes the user view of a repository is a name and URL (and possibly other
 * info as this class develops). Individual subclasses will dictate what kind of
 * repository and how it's created.
 * 
 * @since 3.4
 * 
 */
public abstract class AddRepositoryDialog extends StatusDialog {

	Button okButton;
	Text url;
	IRepository[] knownRepositories;
	static final String[] ARCHIVE_EXTENSIONS = new String[] {"*.jar;*.zip"}; //$NON-NLS-1$ 
	static String lastLocalLocation = null;
	static String lastArchiveLocation = null;

	public AddRepositoryDialog(Shell parentShell, IRepository[] knownRepositories) {

		super(parentShell);
		this.knownRepositories = knownRepositories;
		setTitle(ProvUIMessages.AddRepositoryDialog_Title);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	protected Control createDialogArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		initializeDialogUnits(comp);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		comp.setLayout(layout);
		GridData data = new GridData();
		comp.setLayoutData(data);

		Label urlLabel = new Label(comp, SWT.NONE);
		urlLabel.setText(ProvUIMessages.RepositoryGroup_RepositoryURLFieldLabel);
		url = new Text(comp, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		url.setLayoutData(data);
		url.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				verifyComplete();
			}
		});
		url.setText("http://"); //$NON-NLS-1$
		url.setSelection(0, url.getText().length());

		// add vertical buttons for setting archive or local repos
		Composite buttonParent = new Composite(comp, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 5;
		layout.marginHeight = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		buttonParent.setLayout(layout);
		Button locationButton = new Button(buttonParent, SWT.PUSH);
		locationButton.setText(ProvUIMessages.RepositoryGroup_LocalRepoBrowseButton);
		locationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.APPLICATION_MODAL);
				dialog.setMessage(ProvUIMessages.RepositoryGroup_SelectRepositoryDirectory);
				dialog.setFilterPath(lastLocalLocation);
				String path = dialog.open();
				if (path != null) {
					lastLocalLocation = path;
					url.setText("file:" + path); //$NON-NLS-1$
				}
			}
		});
		setButtonLayoutData(locationButton);
		locationButton = new Button(buttonParent, SWT.PUSH);
		locationButton.setText(ProvUIMessages.RepositoryGroup_ArchivedRepoBrowseButton);
		locationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				FileDialog dialog = new FileDialog(getShell(), SWT.APPLICATION_MODAL);
				dialog.setText(ProvUIMessages.RepositoryGroup_RepositoryFile);
				dialog.setFilterExtensions(ARCHIVE_EXTENSIONS);
				dialog.setFileName(lastArchiveLocation);
				String path = dialog.open();
				if (path != null) {
					lastArchiveLocation = path;
					url.setText("jar:file:" + path + "!/"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		});
		setButtonLayoutData(locationButton);
		Dialog.applyDialogFont(comp);
		return comp;
	}

	protected void okPressed() {
		addRepository();
		super.okPressed();
	}

	protected void addRepository() {
		URL newURL = makeRepositoryURL(url.getText().trim());
		if (newURL != null)
			ProvisioningOperationRunner.execute(getOperation(newURL), getShell(), null);
	}

	protected abstract ProvisioningOperation getOperation(URL repoURL);

	protected abstract URL makeRepositoryURL(String urlString);

	void verifyComplete() {
		if (okButton == null) {
			return;
		}
		String urlText = url.getText().trim();
		IStatus status = Status.OK_STATUS;
		if (urlText.length() == 0) {
			status = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, ProvUIMessages.RepositoryGroup_URLRequired, null);
		} else {
			for (int i = 0; i < knownRepositories.length; i++) {
				URL repURL = knownRepositories[i].getLocation();
				if (repURL != null && repURL.equals(urlText)) {
					status = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, IStatus.OK, ProvUIMessages.AddRepositoryDialog_DuplicateURL, null);
					break;
				}
			}
		}
		setOkEnablement(status.isOK());
		updateStatus(status);
	}

	protected void updateButtonsEnableState(IStatus status) {
		setOkEnablement(!status.matches(IStatus.ERROR));
	}

	private void setOkEnablement(boolean enable) {
		if (okButton != null && !okButton.isDisposed())
			okButton.setEnabled(enable);
	}
}
