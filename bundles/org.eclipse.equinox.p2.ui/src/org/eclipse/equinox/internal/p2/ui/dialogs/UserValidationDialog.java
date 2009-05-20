/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI.AuthenticationInfo;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * A dialog to prompt the user for login information such as user name and password.
 */
public class UserValidationDialog extends MessageDialog {

	private Text username;
	private Text password;
	private AuthenticationInfo result = null;

	private Button saveButton;

	public UserValidationDialog(Shell parentShell, String titleMessage, Image titleImage, String message, String[] buttonLabels) {
		super(parentShell, titleMessage, titleImage, message, MessageDialog.QUESTION, buttonLabels, 0);
	}

	public UserValidationDialog(AuthenticationInfo lastUsed, Shell parentShell, String titleMessage, Image titleImage, String message, String[] buttonLabels) {
		super(parentShell, titleMessage, titleImage, message, MessageDialog.WARNING, buttonLabels, 0);
		result = lastUsed;
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		Composite fieldContainer = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		fieldContainer.setLayout(layout);
		GridData layoutData = new GridData();
		fieldContainer.setLayoutData(layoutData);

		Label label = new Label(fieldContainer, SWT.NONE);
		label.setText(ProvUIMessages.UserValidationDialog_UsernameLabel);
		username = new Text(fieldContainer, SWT.BORDER);
		layoutData = new GridData(GridData.FILL_HORIZONTAL);
		layoutData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		layoutData.horizontalAlignment = SWT.END;
		username.setLayoutData(layoutData);
		username.setText(getUserName());

		label = new Label(fieldContainer, SWT.NONE);
		label.setText(ProvUIMessages.UserValidationDialog_PasswordLabel);
		password = new Text(fieldContainer, SWT.PASSWORD | SWT.BORDER);
		layoutData = new GridData(GridData.FILL_HORIZONTAL);
		layoutData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		layoutData.horizontalAlignment = SWT.END;
		password.setLayoutData(layoutData);
		password.setText(getPassword());

		Composite checkboxContainer = new Composite(composite, SWT.NONE);
		layout = new GridLayout();
		checkboxContainer.setLayout(layout);
		layoutData = new GridData();
		layoutData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		checkboxContainer.setLayoutData(layoutData);
		saveButton = new Button(checkboxContainer, SWT.CHECK);
		saveButton.setText(ProvUIMessages.UserValidationDialog_SavePasswordButton);
		saveButton.setSelection(saveResult());

		username.setFocus();

		return composite;
	}

	protected void buttonPressed(int buttonId) {
		if (buttonId == getDefaultButtonIndex())
			this.result = new AuthenticationInfo(username.getText(), password.getText(), saveButton.getSelection());
		super.buttonPressed(buttonId);
	}

	/**
	 * Returns the authentication information given by the user, or null if the user cancelled
	 * @return the authentication information given by the user, or null if the user cancelled
	 */
	public AuthenticationInfo getResult() {
		return result;
	}

	private String getUserName() {
		return result != null ? result.getUserName() : ""; //$NON-NLS-1$
	}

	private String getPassword() {
		return result != null ? result.getPassword() : ""; //$NON-NLS-1$
	}

	private boolean saveResult() {
		return result != null ? result.saveResult() : false;
	}
}
