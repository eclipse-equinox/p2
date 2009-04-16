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

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.TextURLDropAdapter;
import org.eclipse.equinox.internal.provisional.p2.ui.IProvHelpContextIds;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.AddRepositoryOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Abstract dialog class for adding repositories of different types. This class
 * assumes the user view of a repository is a name and URI. Individual subclasses 
 * will dictate what kind of repository and how it's created.
 * 
 * @since 3.4
 * 
 */
public abstract class AddRepositoryDialog extends StatusDialog {

	Button okButton;
	Text url, nickname;
	URI addedLocation;
	static final String[] ARCHIVE_EXTENSIONS = new String[] {"*.jar;*.zip"}; //$NON-NLS-1$ 
	static String lastLocalLocation = null;
	static String lastArchiveLocation = null;
	Policy policy;

	public AddRepositoryDialog(Shell parentShell, Policy policy) {
		super(parentShell);
		this.policy = policy;
		setTitle(ProvUIMessages.AddRepositoryDialog_Title);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parentShell, IProvHelpContextIds.ADD_REPOSITORY_DIALOG);
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
		layout.marginTop = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);

		comp.setLayout(layout);
		GridData data = new GridData();
		comp.setLayoutData(data);

		// Name: []
		Label nameLabel = new Label(comp, SWT.NONE);
		nameLabel.setText(ProvUIMessages.AddRepositoryDialog_NameLabel);
		nickname = new Text(comp, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);

		nickname.setLayoutData(data);

		Button localButton = new Button(comp, SWT.PUSH);
		localButton.setText(ProvUIMessages.RepositoryGroup_LocalRepoBrowseButton);
		localButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.APPLICATION_MODAL);
				dialog.setMessage(ProvUIMessages.RepositoryGroup_SelectRepositoryDirectory);
				dialog.setFilterPath(lastLocalLocation);
				String path = dialog.open();
				if (path != null) {
					lastLocalLocation = path;
					url.setText(RepositoryLocationValidator.makeFileURLString(path));
					validateRepositoryURL(false);
				}
			}
		});
		setButtonLayoutData(localButton);

		// Location: []
		Label urlLabel = new Label(comp, SWT.NONE);
		urlLabel.setText(ProvUIMessages.AddRepositoryDialog_LocationLabel);
		url = new Text(comp, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		url.setLayoutData(data);
		DropTarget target = new DropTarget(url, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
		target.setTransfer(new Transfer[] {URLTransfer.getInstance(), FileTransfer.getInstance()});
		target.addDropListener(new TextURLDropAdapter(url, true));
		url.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateRepositoryURL(false);
			}
		});
		url.setText(getInitialLocationText());
		url.setSelection(0, url.getText().length());

		Button archiveButton = new Button(comp, SWT.PUSH);
		archiveButton.setText(ProvUIMessages.RepositoryGroup_ArchivedRepoBrowseButton);
		archiveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				FileDialog dialog = new FileDialog(getShell(), SWT.APPLICATION_MODAL);
				dialog.setText(ProvUIMessages.RepositoryGroup_RepositoryFile);
				dialog.setFilterExtensions(ARCHIVE_EXTENSIONS);
				dialog.setFileName(lastArchiveLocation);
				String path = dialog.open();
				if (path != null) {
					lastArchiveLocation = path;
					url.setText(RepositoryLocationValidator.makeJarURLString(path));
					validateRepositoryURL(false);
				}
			}
		});
		setButtonLayoutData(archiveButton);
		comp.setTabList(new Control[] {nickname, url, localButton, archiveButton});
		Dialog.applyDialogFont(comp);
		return comp;
	}

	/**
	 * Return a location validator appropriate for this dialog.  The
	 * default is to retrieve it from the repository manipulator.
	 * Subclasses may override.
	 * 
	 * @return the validator
	 */
	protected RepositoryLocationValidator getRepositoryLocationValidator() {
		return getRepositoryManipulator().getRepositoryLocationValidator(getShell());
	}

	/**
	 * Return a RepositoryManipulator appropriate for validating and adding the
	 * repository.
	 * 
	 * The default manipulator is described by the policy.  Subclasses may override.
	 * @return the repository manipulator
	 */
	protected RepositoryManipulator getRepositoryManipulator() {
		return policy.getRepositoryManipulator();
	}

	protected void okPressed() {
		IStatus status = addRepository();
		if (status.isOK())
			super.okPressed();

	}

	/**
	 * Get the repository location as currently typed in by the user.  Return null if there
	 * is a problem with the URL.
	 * 
	 * @return the URL currently typed in by the user.
	 */
	protected URI getUserLocation() {
		URI userLocation;
		try {
			userLocation = URIUtil.fromString(url.getText().trim());
		} catch (URISyntaxException e) {
			return null;
		}
		return userLocation;
	}

	/**
	 * Get the location of the repository that was added by this dialog.  Return <code>null</code>
	 * if the dialog has not yet added a repository location.
	 * 
	 * @return the location of the repository that has been added by this dialog, or <code>null</code>
	 * if no repository has been added.
	 */
	public URI getAddedLocation() {
		return addedLocation;
	}

	protected IStatus addRepository() {
		IStatus status = validateRepositoryURL(false);
		if (status.isOK()) {
			addedLocation = getUserLocation();
			AddRepositoryOperation op = getOperation(addedLocation);
			String nick = nickname.getText().trim();
			if (nick.length() > 0)
				op.setNicknames(new String[] {nick});
			ProvisioningOperationRunner.schedule(op, StatusManager.SHOW | StatusManager.LOG);
		}
		return status;
	}

	/**
	 * Get an add operation appropriate for this dialog.  The default behavior
	 * is to retrieve it from the policy, but subclasses may override.
	 * 
	 * @param repositoryLocation to be added
	 * @return the add operation
	 */
	protected AddRepositoryOperation getOperation(URI repositoryLocation) {
		return getRepositoryManipulator().getAddOperation(repositoryLocation);
	}

	/**
	 * Validate the repository URL, returning a status that is appropriate
	 * for showing the user.  The boolean indicates whether the repositories
	 * should be consulted for validating the URL.  For example, it is not 
	 * appropriate to contact the repositories on every keystroke.
	 */
	protected IStatus validateRepositoryURL(final boolean contactRepositories) {
		if (url == null || url.isDisposed())
			return Status.OK_STATUS;
		final IStatus[] status = new IStatus[1];
		status[0] = RepositoryLocationValidator.getInvalidLocationStatus(url.getText().trim());
		final URI userLocation = getUserLocation();
		if (url.getText().length() == 0)
			status[0] = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, RepositoryLocationValidator.LOCAL_VALIDATION_ERROR, ProvUIMessages.RepositoryGroup_URLRequired, null);
		else if (userLocation == null)
			status[0] = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, RepositoryLocationValidator.LOCAL_VALIDATION_ERROR, ProvUIMessages.AddRepositoryDialog_InvalidURL, null);
		else {
			BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
				public void run() {
					status[0] = getRepositoryLocationValidator().validateRepositoryLocation(userLocation, contactRepositories, null);
				}
			});

		}

		// At this point the subclasses may have decided to opt out of
		// this dialog.
		if (status[0].getSeverity() == IStatus.CANCEL) {
			cancelPressed();
		}

		setOkEnablement(status[0].isOK());
		updateStatus(status[0]);
		return status[0];

	}

	protected void updateButtonsEnableState(IStatus status) {
		setOkEnablement(!status.matches(IStatus.ERROR));
	}

	private void setOkEnablement(boolean enable) {
		if (okButton != null && !okButton.isDisposed())
			okButton.setEnabled(enable);
	}

	protected String getInitialLocationText() {
		return "http://"; //$NON-NLS-1$
	}
}
