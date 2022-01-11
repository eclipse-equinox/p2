/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.processors.pgp.PGPPublicKeyStore;
import org.eclipse.equinox.internal.p2.engine.phases.CertificateChecker;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class TrustPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private CertificateChecker certificateChecker;
	private PGPPublicKeyStore trustedKeys;
	private boolean dirty = false;
	private TableViewer viewer;

	public TrustPreferencePage() {
		super(ProvSDKMessages.TrustPreferencePage_title);
	}

	@Override
	public void init(IWorkbench workbench) {
		// nothing to do
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite res = new Composite(parent, SWT.NONE);

		Label pgpLabel = new Label(res, SWT.WRAP);
		pgpLabel.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));
		pgpLabel.setText(ProvSDKMessages.TrustPreferencePage_pgpIntro);

		res.setLayout(new GridLayout(2, false));
		viewer = new TableViewer(res);
		viewer.getTable().setHeaderVisible(true);
		viewer.setContentProvider(new ArrayContentProvider());
		TableViewerColumn idColumn = new TableViewerColumn(viewer, SWT.NONE);
		idColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return Long.toHexString(((PGPPublicKey) element).getKeyID()).toUpperCase();
			}
		});
		idColumn.getColumn().setWidth(16 * 10); // number of chars in a key Id * some heuristic of width
		idColumn.getColumn().setText(ProvSDKMessages.TrustPreferencePage_idColumn);
		TableViewerColumn userColumn = new TableViewerColumn(viewer, SWT.NONE);
		userColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				List<String> userIds = new ArrayList<>();
				((PGPPublicKey) element).getUserIDs().forEachRemaining(userIds::add);
				return String.join(",", userIds); //$NON-NLS-1$
			}
		});
		userColumn.getColumn().setWidth(400);
		userColumn.getColumn().setText(ProvSDKMessages.TrustPreferencePage_userColumn);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		IProvisioningAgent provisioningAgent = ProvSDKUIActivator.getDefault().getProvisioningAgent();
		certificateChecker = new CertificateChecker(provisioningAgent);
		certificateChecker
				.setProfile(provisioningAgent.getService(IProfileRegistry.class).getProfile(IProfileRegistry.SELF));
		trustedKeys = certificateChecker.buildPGPTrustore();
		viewer.setInput(trustedKeys.all());
		Composite buttonComposite = createVerticalButtonBar(res);
		buttonComposite.setLayoutData(new GridData(SWT.DEFAULT, SWT.BEGINNING, false, false));
		Button exportButton = new Button(buttonComposite, SWT.PUSH);
		exportButton.setText(ProvSDKMessages.TrustPreferencePage_export);
		setVerticalButtonLayoutData(exportButton);
		exportButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			PGPPublicKey key = getSelectedKey();
			if (key == null) {
				return;
			}
			FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
			dialog.setText(ProvSDKMessages.TrustPreferencePage_fileExportTitle);
			dialog.setFilterExtensions(new String[] { "*.asc" }); //$NON-NLS-1$
			dialog.setFileName(Long.toHexString(key.getKeyID()).toUpperCase() + ".asc"); //$NON-NLS-1$
			String path = dialog.open();
			if (path == null) {
				return;
			}
			File destinationFile = new File(path);
			try (OutputStream output = new ArmoredOutputStream(new FileOutputStream(destinationFile))) {
				output.write(key.getEncoded());
			} catch (IOException ex) {
				ProvSDKUIActivator.getDefault().getLog()
						.log(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ex.getMessage(), ex));
			}
		}));
		Button addButton = new Button(buttonComposite, SWT.PUSH);
		addButton.setText(ProvSDKMessages.TrustPreferencePage_addPGPKeyButtonLabel);
		addButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
			dialog.setText(ProvSDKMessages.TrustPreferencePage_fileImportTitle);
			dialog.setFilterExtensions(new String[] { "*.asc" }); //$NON-NLS-1$
			String path = dialog.open();
			if (path == null) {
				return;
			}
			trustedKeys.add(new File(path));
			viewer.setInput(trustedKeys.all());
			dirty = true;
		}));
		setVerticalButtonLayoutData(addButton);
		Button removeButton = new Button(buttonComposite, SWT.PUSH);
		removeButton.setText(ProvSDKMessages.TrustPreferencePage_removePGPKeyButtonLabel);
		removeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			trustedKeys.remove(getSelectedKey());
			viewer.setInput(trustedKeys.all());
			dirty = true;
		}));
		setVerticalButtonLayoutData(removeButton);
		viewer.addPostSelectionChangedListener(e -> {
			exportButton.setEnabled(getSelectedKey() != null);
			removeButton.setEnabled(getSelectedKey() != null);
		});
		exportButton.setEnabled(getSelectedKey() != null);
		removeButton.setEnabled(getSelectedKey() != null);
		return res;
	}

	private PGPPublicKey getSelectedKey() {
		ISelection sel = viewer.getSelection();
		if (!(sel instanceof IStructuredSelection)) {
			return null;
		}
		Object o = ((IStructuredSelection) sel).getFirstElement();
		if (!(o instanceof PGPPublicKey)) {
			return null;
		}
		return (PGPPublicKey) o;
	}

	private Composite createVerticalButtonBar(Composite parent) {
		// Create composite.
		Composite composite = new Composite(parent, SWT.NONE);
		initializeDialogUnits(composite);

		// create a layout with spacing and margins appropriate for the font
		// size.
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 5;
		layout.marginHeight = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);

		return composite;
	}

	private GridData setVerticalButtonLayoutData(Button button) {
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = Math.max(widthHint, minSize.x);
		button.setLayoutData(data);
		return data;
	}

	@Override
	public boolean performOk() {
		if (dirty) {
			return certificateChecker.persistTrustedKeys(trustedKeys).isOK();
		}
		return true;
	}

}
