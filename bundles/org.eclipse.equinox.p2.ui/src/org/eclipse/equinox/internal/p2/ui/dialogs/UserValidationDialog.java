/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.p2.core.UIServices.AuthenticationInfo;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * A dialog to prompt the user for login information such as user name and password.
 */
public class UserValidationDialog extends Dialog {

	private Text username;
	private Text password;
	private AuthenticationInfo result = null;

	private Button saveButton;

	private String titleMessage;
	private Image titleImage;

	private String message;

	private int dialogImageType;

	/**
	 * Creates a new validation dialog that prompts the user for login credentials.
	 * 
	 * @param parentShell the parent shell of this dialog
	 * @param titleMessage the message to be displayed by this dialog's window
	 * @param titleImage the image of this shell, may be <code>null</code>
	 * @param message the message to prompt to the user
	 */
	public UserValidationDialog(Shell parentShell, String titleMessage, Image titleImage, String message) {
		this(null, parentShell, titleMessage, titleImage, message, SWT.ICON_QUESTION);
	}

	/**
	 * Creates a new validation dialog that prompts the user for login credentials.
	 * 
	 * @param lastUsed the authentication information that was originally as an attempt to login
	 * @param parentShell the parent shell of this dialog
	 * @param titleMessage the message to be displayed by this dialog's window
	 * @param titleImage the image of this shell, may be <code>null</code>
	 * @param message the message to prompt to the user
	 */
	public UserValidationDialog(AuthenticationInfo lastUsed, Shell parentShell, String titleMessage, Image titleImage, String message) {
		this(lastUsed, parentShell, titleMessage, titleImage, message, SWT.ICON_WARNING);
	}

	private UserValidationDialog(AuthenticationInfo lastUsed, Shell parentShell, String titleMessage, Image titleImage, String message, int dialogImageType) {
		super(parentShell);
		result = lastUsed;

		this.titleMessage = titleMessage;
		this.titleImage = titleImage;
		this.message = message;
		this.dialogImageType = dialogImageType;
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(titleMessage);
		newShell.setImage(titleImage);
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		Composite container = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createImageSection(container);
		createFieldsSection(container);

		Dialog.applyDialogFont(composite);

		return composite;
	}

	private void createImageSection(Composite composite) {
		Image image = composite.getDisplay().getSystemImage(dialogImageType);
		if (image != null) {
			Label label = new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
			label.setImage(image);
		}
	}

	private void createFieldsSection(Composite composite) {
		Composite fieldContainer = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		fieldContainer.setLayout(layout);
		GridData layoutData = new GridData();
		fieldContainer.setLayoutData(layoutData);

		Label label = new Label(fieldContainer, SWT.WRAP | SWT.LEAD);
		GridData data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		label.setLayoutData(data);
		label.setText(message);

		label = new Label(fieldContainer, SWT.NONE);
		label.setText(ProvUIMessages.UserValidationDialog_UsernameLabel);
		username = new Text(fieldContainer, SWT.BORDER);
		layoutData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		username.setLayoutData(layoutData);
		username.setText(getUserName());

		label = new Label(fieldContainer, SWT.NONE);
		label.setText(ProvUIMessages.UserValidationDialog_PasswordLabel);
		password = new Text(fieldContainer, SWT.PASSWORD | SWT.BORDER);
		layoutData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		password.setLayoutData(layoutData);
		password.setText(getPassword());

		saveButton = new Button(fieldContainer, SWT.CHECK);
		saveButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));
		saveButton.setText(ProvUIMessages.UserValidationDialog_SavePasswordButton);
		saveButton.setSelection(saveResult());
	}

	protected void okPressed() {
		this.result = new AuthenticationInfo(username.getText(), password.getText(), saveButton.getSelection());
		super.okPressed();
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
