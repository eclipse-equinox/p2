/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.net.URI;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * Class for showing a repository name and location
 * 
 * @since 3.4
 * 
 */
public class RepositoryNameAndLocationDialog extends StatusDialog {

	Button okButton;
	Text url, nickname;
	Policy policy;
	URI location;
	String name;

	public RepositoryNameAndLocationDialog(Shell parentShell, Policy policy) {
		super(parentShell);
		this.policy = policy;
		setTitle(ProvUIMessages.RepositoryNameAndLocationDialog_Title);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	protected Control createDialogArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		initializeDialogUnits(comp);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginTop = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);

		nickname = createNameField(comp);
		url = createLocationField(comp);

		comp.setLayout(layout);
		GridData data = new GridData();
		comp.setLayoutData(data);

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
		if (handleOk())
			super.okPressed();
	}

	protected boolean handleOk() {
		IStatus status = validateRepositoryURL(false);
		location = getUserLocation();
		name = nickname.getText().trim();
		return status.isOK();
	}

	/**
	 * Get the repository location as currently typed in by the user.  Return null if there
	 * is a problem with the URL.
	 * 
	 * @return the URL currently typed in by the user.
	 */
	protected URI getUserLocation() {
		return RepositoryLocationValidator.locationFromString(url.getText().trim());
	}

	/**
	 * Get the location of the repository that was entered by the user.
	 * Return <code>null</code> if no location was provided.
	 * 
	 * @return the location of the repository that has been provided by the user.
	 */
	public URI getLocation() {
		return location;
	}

	/**
	 * Get the name of the repository that was entered by the user.
	 * Return <code>null</code> if no name was provided.
	 * 
	 * @return the name of the repository that has been provided by the user.
	 */
	public String getName() {
		return name;
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

	protected String getInitialNameText() {
		return ""; //$NON-NLS-1$
	}

	protected Text createNameField(Composite parent) {
		// Name: []
		Label nameLabel = new Label(parent, SWT.NONE);
		nameLabel.setText(ProvUIMessages.AddRepositoryDialog_NameLabel);
		nickname = new Text(parent, SWT.BORDER);
		nickname.setText(getInitialNameText());
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);

		nickname.setLayoutData(data);
		return nickname;
	}

	protected Text createLocationField(Composite parent) {
		// Location: []
		Label urlLabel = new Label(parent, SWT.NONE);
		urlLabel.setText(ProvUIMessages.AddRepositoryDialog_LocationLabel);
		url = new Text(parent, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
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
		return url;
	}
}
