/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     EclipseSource
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.installer.ui;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.installer.InstallerActivator;
import org.eclipse.equinox.internal.p2.installer.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public final class ProxiesDialog {

	private IProxyData data;
	private Label typeLabel;
	private Combo typeCombo;
	private Label hostLabel;
	private Text hostText;
	private Label portLabel;
	private Text portText;

	private Label userIdLabel;
	private Text userIdText;
	private Label passwordLabel;
	private Text passwordText;
	private Button okButton;
	private Button cancelButton;
	private final IProxyService service;
	private Shell shell;
	private List<String> types;
	private Label statuslabel;

	public ProxiesDialog(IProxyService service) {
		if (service == null) {
			throw new IllegalArgumentException();
		}

		this.service = service;
		initTypes();
	}

	public IProxyData getValue() {
		return data;
	}

	public void open() {
		this.data = service.getProxyData(IProxyData.HTTP_PROXY_TYPE);
		if (data == null) {
			openMessage(Messages.ProxiesDialog_FailedToReadProxySettingsMessage, SWT.ICON_ERROR | SWT.OK);
			return;
		}

		Shell activeShell = Display.getDefault().getActiveShell();
		shell = new Shell(activeShell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.MIN | SWT.RESIZE);
		//Computes the bounds
		Rectangle bounds = null;
		if (activeShell == null) {
			bounds = new Rectangle(300, 200, 600, 400);
		} else {
			Rectangle parentBounds = activeShell.getBounds();
			bounds = new Rectangle(parentBounds.x + 100, parentBounds.y + 100, 600, 400);
		}
		shell.setBounds(bounds);

		shell.setText(Messages.ProxiesDialog_DialogTitle);
		shell.setLayout(new GridLayout());

		createDialogArea(shell);
		createButtonBar(shell);

		shell.pack();
		shell.open();
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(4, false));
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));

		typeLabel = new Label(composite, SWT.NONE);
		typeLabel.setText(Messages.ProxiesDialog_ShemaLabel);
		typeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		typeCombo = new Combo(composite, SWT.BORDER);
		typeCombo.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 3, 1));
		//Currently only these three proxy types are supported.
		for (int i = 0; i < types.size(); i++) {
			typeCombo.add(types.get(i));
		}

		typeCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String selection = typeCombo.getText();
				IProxyData selectedProxy = service.getProxyData(selection);
				if (selectedProxy == null) {
					updateStatus(new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, IStatus.OK, Messages.ProxiesDialog_UnknownProxyTypeMessage, null));
				} else {
					data = selectedProxy;
					applyData();
				}
			}
		});

		hostLabel = new Label(composite, SWT.NONE);
		hostLabel.setText(Messages.ProxiesDialog_HostLabel);
		hostLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		hostText = new Text(composite, SWT.BORDER);
		GridData gdata = new GridData(SWT.FILL, SWT.TOP, true, false);
		gdata.widthHint = 250;
		hostText.setLayoutData(gdata);

		portLabel = new Label(composite, SWT.NONE);
		portLabel.setText(Messages.ProxiesDialog_PortLabel);
		portText = new Text(composite, SWT.BORDER);
		gdata = new GridData();
		gdata.widthHint = 30;
		portText.setLayoutData(gdata);

		userIdLabel = new Label(composite, SWT.NONE);
		userIdLabel.setText(Messages.ProxiesDialog_UserLabel);
		userIdLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		userIdText = new Text(composite, SWT.BORDER);
		userIdText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));

		passwordLabel = new Label(composite, SWT.NONE);
		passwordLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 1, 1));
		passwordLabel.setText(Messages.ProxiesDialog_PasswordLabel);
		passwordText = new Text(composite, SWT.BORDER);
		passwordText.setEchoChar('*');
		passwordText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 3, 1));

		ModifyListener validationListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateStatus();
			}
		};
		typeCombo.addModifyListener(validationListener);
		hostText.addModifyListener(validationListener);
		portText.addModifyListener(validationListener);
		userIdText.addModifyListener(validationListener);
		passwordText.addModifyListener(validationListener);

		//Initialize the UI with the selected data
		applyData();
		hostText.setFocus();
		updateStatus();
		return composite;
	}

	private void createButtonBar(Composite parent) {
		Composite buttonBar = new Composite(parent, SWT.NONE);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalAlignment = SWT.RIGHT;
		buttonBar.setLayoutData(gridData);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonBar.setLayout(layout);

		statuslabel = new Label(buttonBar, SWT.NONE);
		gridData = new GridData(300, SWT.DEFAULT);
		statuslabel.setLayoutData(gridData);

		okButton = new Button(buttonBar, SWT.PUSH);
		gridData = new GridData(100, SWT.DEFAULT);
		okButton.setLayoutData(gridData);
		okButton.setText(Messages.ProxiesDialog_OkLabel);
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				okPressed();
				shell.dispose();
			}
		});

		cancelButton = new Button(buttonBar, SWT.PUSH);
		cancelButton.setLayoutData(gridData);
		cancelButton.setText(Messages.Dialog_CancelButton);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.dispose();
			}
		});
	}

	public void create() {
		validateHostName();
	}

	private String toString(String str) {
		return str == null ? "" : str; //$NON-NLS-1$
	}

	private void applyData() {
		typeCombo.setText(toString(data.getType()));
		hostText.setText(toString(data.getHost()));
		if (data.getPort() != -1) {
			portText.setText(toString(Integer.toString(data.getPort())));
		} else {
			portText.setText(""); //$NON-NLS-1$
		}
		userIdText.setText(toString(data.getUserId()));
		passwordText.setText(toString(data.getPassword()));
	}

	private boolean validateHostName() {
		try {
			new URI(hostText.getText());
		} catch (URISyntaxException e) {
			updateStatus(new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, IStatus.OK, Messages.ProxiesDialog_InvalitHostMessage, null));
			return false;
		}
		return true;
	}

	protected void okPressed() {
		data.setHost(hostText.getText());
		data.setPort(Integer.parseInt(portText.getText()));
		data.setUserid(userIdText.getText());
		data.setPassword(passwordText.getText());
		try {
			if (service != null) {
				service.setProxyData(new IProxyData[] {data});
			} else {
				openMessage(Messages.ProxiesDialog_ServiceNotAvailableMessage, SWT.ICON_ERROR | SWT.OK);
			}
		} catch (Exception e) {
			LogHelper.log(new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, e.getMessage(), e));
			openMessage(Messages.ProxiesDialog_FailedToSetProxyMessage + e.getLocalizedMessage(), SWT.ICON_ERROR | SWT.OK);
		}
	}

	protected void updateStatus() {
		if (!validateHostName()) {
			return;
		}
		if (hostText.getText().length() == 0) {
			updateStatus(new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, IStatus.OK, Messages.ProxiesDialog_EmptyHostMessage, null));
			return;
		}

		if (userIdText.getText().length() == 0) {
			updateStatus(new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, IStatus.OK, Messages.ProxiesDialog_EmptyUserMessage, null));
			return;
		}

		if (passwordText.getText().length() == 0) {
			updateStatus(new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, IStatus.OK, Messages.ProxiesDialog_EmptyPasswordMessage, null));
			return;
		}

		try {
			//Port checks
			String portAsString = portText.getText();
			if (portAsString == null || portAsString.length() == 0) {
				updateStatus(new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, IStatus.OK, Messages.ProxiesDialog_EmptyProtMessage, null));
				return;
			}
			int port = Integer.parseInt(portAsString);
			if (port < 0) {
				updateStatus(new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, IStatus.OK, Messages.ProxiesDialog_NegativValue, null));
				return;
			}
		} catch (NumberFormatException e) {
			updateStatus(new Status(IStatus.ERROR, InstallerActivator.PI_INSTALLER, IStatus.OK, Messages.ProxiesDialog_WrongFormat, null));
			return;
		}
		updateStatus(Status.OK_STATUS);
	}

	private void updateStatus(IStatus status) {

		if (okButton != null) {
			okButton.setEnabled(status.isOK());
		}
		if (statuslabel != null) {
			String statusText = status.isOK() ? "" : Messages.ProxiesDialog_StatusPrefix + status.getMessage(); //$NON-NLS-1$
			statuslabel.setText(statusText);
		}
	}

	private void openMessage(String msg, int style) {
		MessageBox messageBox = new MessageBox(Display.getDefault().getActiveShell(), style);
		messageBox.setMessage(msg);
		messageBox.open();
	}

	private void initTypes() {
		types = new ArrayList<String>();
		types.add(IProxyData.HTTP_PROXY_TYPE);
		types.add(IProxyData.HTTPS_PROXY_TYPE);
		types.add(IProxyData.SOCKS_PROXY_TYPE);
	}
}
