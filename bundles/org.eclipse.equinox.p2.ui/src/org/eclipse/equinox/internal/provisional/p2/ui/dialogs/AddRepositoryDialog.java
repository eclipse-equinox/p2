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

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

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
	URL[] knownRepositories;
	protected static final int NON_REPO_ERROR = 0;
	protected static final String FILE_PROTOCOL = "file"; //$NON-NLS-1$
	protected static final String FILE_PROTOCOL_PREFIX = "file:"; //$NON-NLS-1$
	protected static final String JAR_PATH_PREFIX = "jar:";//$NON-NLS-1$
	protected static final String JAR_PATH_SUFFIX = "!/"; //$NON-NLS-1$
	static final String[] ARCHIVE_EXTENSIONS = new String[] {"*.jar;*.zip"}; //$NON-NLS-1$ 
	static String lastLocalLocation = null;
	static String lastArchiveLocation = null;

	public AddRepositoryDialog(Shell parentShell, URL[] knownRepositories) {

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
		urlLabel.setText(ProvUIMessages.RepositoryPropertyPage_URLFieldLabel);
		url = new Text(comp, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		url.setLayoutData(data);
		url.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateRepositoryURL(false);
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
					url.setText(FILE_PROTOCOL_PREFIX + path);
					validateRepositoryURL(true);
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
					url.setText(FILE_PROTOCOL_PREFIX + JAR_PATH_PREFIX + path + JAR_PATH_SUFFIX);
					validateRepositoryURL(true);
				}
			}
		});
		setButtonLayoutData(locationButton);
		Dialog.applyDialogFont(comp);
		return comp;
	}

	protected void okPressed() {
		IStatus status = addRepository();
		if (status.isOK())
			super.okPressed();

	}

	/**
	 * Get the URL as currently typed in by the user.  Return null if there
	 * is a problem with the URL.
	 * 
	 * @return the URL currently typed in by the user.
	 */
	protected URL getUserURL() {
		URL userURL;
		try {
			userURL = new URL(url.getText().trim());
		} catch (MalformedURLException e) {
			return null;
		}
		return userURL;
	}

	protected IStatus addRepository() {
		IStatus status = validateRepositoryURL(true);
		if (status.isOK()) {
			ProvisioningOperationRunner.run(getOperation(getUserURL()), getShell());
		}
		return status;
	}

	protected abstract ProvisioningOperation getOperation(URL repoURL);

	/**
	 * Validate the repository URL, returning a status that is appropriate
	 * for showing the user.  The boolean indicates whether the repositories
	 * should be consulted for validating the URL.  For example, it is not 
	 * appropriate to contact the repositories on every keystroke.
	 */
	protected IStatus validateRepositoryURL(boolean contactRepositories) {
		if (url == null || url.isDisposed())
			return Status.OK_STATUS;
		final IStatus[] status = new IStatus[1];
		status[0] = Status.OK_STATUS;
		final URL userURL = getUserURL();
		if (url.getText().length() == 0)
			status[0] = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, NON_REPO_ERROR, ProvUIMessages.RepositoryGroup_URLRequired, null);
		else if (userURL == null)
			status[0] = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, NON_REPO_ERROR, ProvUIMessages.AddRepositoryDialog_InvalidURL, null);
		else {
			for (int i = 0; i < knownRepositories.length; i++) {
				if (knownRepositories[i].toExternalForm().equalsIgnoreCase(userURL.toExternalForm())) {
					status[0] = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, NON_REPO_ERROR, ProvUIMessages.AddRepositoryDialog_DuplicateURL, null);
					break;
				}
			}
			if (status[0].isOK() && contactRepositories)
				try {
					PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) {
							status[0] = ProvisioningUtil.validateMetadataRepositoryLocation(userURL, monitor);
						}
					});
				} catch (InvocationTargetException e) {
					return ProvUI.handleException(e.getCause(), ProvUIMessages.AddRepositoryDialog_URLValidationError, StatusManager.SHOW | StatusManager.LOG);
				} catch (InterruptedException e) {
					// ignore
				}
		}
		// If the repositories themselves didn't know what to do with this
		// URL, consult subclasses.  There may be additional work that could
		// be done to make the location valid.
		if (!status[0].isOK() && status[0].getCode() != NON_REPO_ERROR)
			status[0] = handleInvalidRepositoryURL(userURL, status[0]);

		// At this point the subclasses may have decided to opt out of
		// this dialog.
		if (status[0].getSeverity() == IStatus.CANCEL) {
			cancelPressed();
		}

		setOkEnablement(status[0].isOK());
		updateStatus(status[0]);
		return status[0];

	}

	/**
	 * The repository manager has failed in validating a URL.
	 * Perform any additional handling of the URL and return a status
	 * indicating whether the repository URL is still invalid.  Subclasses
	 * may override when there is additional work, such as repository
	 * generation, repository authenticatoin, or repository repair that may
	 * be appropriate for a given URL.
	 * 
	 * @param url the URL describing the invalid repository
	 * @param status the status returned by the repository manager.
	 * 
	 * @return a status indicating the current status of the repository.
	 * Callers may return the original status.  A status with severity
	 * <code>OK</code> indicates that the caller can proceed with adding
	 * the repository.  A status with severity <code>CANCEL</code> indicates
	 * that the dialog should be cancelled.  Any other severity should be 
	 * reported to the user and indicates an invalid URL.
	 */
	protected IStatus handleInvalidRepositoryURL(URL userURL, IStatus status) {
		return status;
	}

	protected void updateButtonsEnableState(IStatus status) {
		setOkEnablement(!status.matches(IStatus.ERROR));
	}

	private void setOkEnablement(boolean enable) {
		if (okButton != null && !okButton.isDisposed())
			okButton.setEnabled(enable);
	}
}
