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
import java.util.*;
import java.util.List;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.processors.pgp.PGPPublicKeyStore;
import org.eclipse.equinox.internal.p2.engine.phases.CertificateChecker;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

public class TrustPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String EXPORT_FILTER_PATH = "exportFilterPath"; //$NON-NLS-1$
	private static final String ADD_FILTER_PATH = "addFilterPath"; //$NON-NLS-1$

	private CertificateChecker certificateChecker;
	private PGPPublicKeyStore trustedKeys;
	private Map<PGPPublicKey, Set<Bundle>> contributedTrustedKeys;
	private boolean dirty;
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
		IProvisioningAgent provisioningAgent = ProvSDKUIActivator.getDefault().getProvisioningAgent();
		certificateChecker = new CertificateChecker(provisioningAgent);
		certificateChecker
				.setProfile(provisioningAgent.getService(IProfileRegistry.class).getProfile(IProfileRegistry.SELF));
		trustedKeys = new PGPPublicKeyStore();
		contributedTrustedKeys = certificateChecker.getContributedTrustedKeys();
		trustedKeys = certificateChecker.getPreferenceTrustedKeys();

		Composite res = new Composite(parent, SWT.NONE);
		res.setLayout(new GridLayout(2, false));

		Label pgpLabel = new Label(res, SWT.WRAP);
		pgpLabel.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));
		pgpLabel.setText(ProvSDKMessages.TrustPreferencePage_pgpIntro);

		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		Composite tableComposite = WidgetFactory.composite(SWT.NONE)
				.layoutData(new GridData(SWT.FILL, SWT.FILL, true, true)).layout(tableColumnLayout).create(res);
		Table keyTable = WidgetFactory.table(SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION)
				.headerVisible(true).linesVisible(true).font(parent.getFont()).create(tableComposite);
		viewer = new TableViewer(keyTable);
		keyTable.setHeaderVisible(true);
		viewer.setContentProvider(new ArrayContentProvider());

		TableViewerColumn idColumn = new TableViewerColumn(viewer, SWT.NONE);
		idColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return PGPPublicKeyService.toHex(((PGPPublicKey) element).getFingerprint()).toUpperCase(Locale.ROOT);
			}
		});
		idColumn.getColumn().setText(ProvSDKMessages.TrustPreferencePage_fingerprintColumn);

		TableViewerColumn userColumn = new TableViewerColumn(viewer, SWT.NONE);
		userColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				List<String> userIds = new ArrayList<>();
				((PGPPublicKey) element).getUserIDs().forEachRemaining(userIds::add);
				return String.join(",", userIds); //$NON-NLS-1$
			}
		});
		userColumn.getColumn().setText(ProvSDKMessages.TrustPreferencePage_userColumn);

		TableViewerColumn contributorColumn = new TableViewerColumn(viewer, SWT.NONE);
		contributorColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Set<String> contributors = new LinkedHashSet<>();
				if (trustedKeys.all().contains(element)) {
					contributors.add(ProvSDKMessages.TrustPreferencePage_PreferenceContributor);
				}
				Set<Bundle> bundles = contributedTrustedKeys.get(element);
				if (bundles != null) {
					Set<String> bundleContributors = new TreeSet<>(Policy.getComparator());
					bundles.stream().map(bundle -> getBundleName(bundle)).forEach(bundleContributors::add);
					contributors.addAll(bundleContributors);
				}
				return String.join(", ", contributors); //$NON-NLS-1$
			}
		});
		contributorColumn.getColumn().setText(ProvSDKMessages.TrustPreferencePage_Contributor);

		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		updateInput();

		tableColumnLayout.setColumnData(idColumn.getColumn(), new ColumnWeightData(33));
		tableColumnLayout.setColumnData(userColumn.getColumn(), new ColumnWeightData(33));
		tableColumnLayout.setColumnData(contributorColumn.getColumn(), new ColumnWeightData(33));

		Composite buttonComposite = createVerticalButtonBar(res);
		buttonComposite.setLayoutData(new GridData(SWT.DEFAULT, SWT.BEGINNING, false, false));

		Button exportButton = new Button(buttonComposite, SWT.PUSH);
		exportButton.setText(ProvSDKMessages.TrustPreferencePage_export);
		setVerticalButtonLayoutData(exportButton);
		exportButton.setEnabled(false);

		exportButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			List<PGPPublicKey> keys = getSelectedKeys();
			FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
			dialog.setFilterPath(getFilterPath(EXPORT_FILTER_PATH));
			dialog.setText(ProvSDKMessages.TrustPreferencePage_fileExportTitle);
			dialog.setFilterExtensions(new String[] { "*.asc" }); //$NON-NLS-1$
			dialog.setFileName(
					PGPPublicKeyService.toHex(keys.get(0).getFingerprint()).toUpperCase(Locale.ROOT) + ".asc"); //$NON-NLS-1$
			String path = dialog.open();
			setFilterPath(EXPORT_FILTER_PATH, dialog.getFilterPath());
			if (path == null) {
				return;
			}
			File destinationFile = new File(path);
			try (OutputStream output = new ArmoredOutputStream(new FileOutputStream(destinationFile))) {
				for (PGPPublicKey key : keys) {
					key.encode(output);
				}
			} catch (IOException ex) {
				ProvSDKUIActivator.getDefault().getLog()
						.log(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ex.getMessage(), ex));
			}
		}));

		Button addButton = new Button(buttonComposite, SWT.PUSH);
		addButton.setText(ProvSDKMessages.TrustPreferencePage_addPGPKeyButtonLabel);
		addButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
			dialog.setFilterPath(getFilterPath(ADD_FILTER_PATH));
			dialog.setText(ProvSDKMessages.TrustPreferencePage_fileImportTitle);
			dialog.setFilterExtensions(new String[] { "*.asc" }); //$NON-NLS-1$
			String path = dialog.open();
			setFilterPath(ADD_FILTER_PATH, dialog.getFilterPath());
			if (path == null) {
				return;
			}
			HashSet<PGPPublicKey> oldKeys = new HashSet<>(trustedKeys.all());
			trustedKeys.add(new File(path));
			HashSet<PGPPublicKey> newKeys = new HashSet<>(trustedKeys.all());
			newKeys.removeAll(oldKeys);
			updateInput();
			viewer.setSelection(new StructuredSelection(newKeys.toArray()), true);
			dirty = true;
		}));
		setVerticalButtonLayoutData(addButton);

		Button removeButton = new Button(buttonComposite, SWT.PUSH);
		removeButton.setText(ProvSDKMessages.TrustPreferencePage_removePGPKeyButtonLabel);
		removeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			getSelectedKeys().forEach(trustedKeys::remove);
			updateInput();
			dirty = true;
		}));
		removeButton.setEnabled(false);
		setVerticalButtonLayoutData(removeButton);

		viewer.addPostSelectionChangedListener(e -> {
			exportButton.setEnabled(!getSelectedKeys().isEmpty());
			Collection<PGPPublicKey> keys = trustedKeys.all();
			removeButton.setEnabled(getSelectedKeys().stream().anyMatch(keys::contains));
		});

		return res;
	}

	private void updateInput() {
		Set<PGPPublicKey> input = new TreeSet<>((k1, k2) -> {
			return Arrays.compare(k1.getFingerprint(), k2.getFingerprint());
		});
		input = new LinkedHashSet<>();
		Collection<PGPPublicKey> all = trustedKeys.all();
		input = new TreeSet<>((k1, k2) -> {
			boolean contains1 = all.contains(k1);
			boolean contains2 = all.contains(k2);
			if (contains1 != contains2) {
				if (contains1) {
					return -1;
				}
				return 1;
			}
			return PGPPublicKeyService.toHex(k1.getFingerprint())
					.compareTo(PGPPublicKeyService.toHex(k2.getFingerprint()));
		});
		input.addAll(all);
		input.addAll(contributedTrustedKeys.keySet());
		viewer.setInput(input);
	}

	@SuppressWarnings("unchecked")
	private List<PGPPublicKey> getSelectedKeys() {
		return viewer.getStructuredSelection().toList();
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

	private String getFilterPath(String key) {
		IDialogSettings dialogSettings = DialogSettings
				.getOrCreateSection(ProvSDKUIActivator.getDefault().getDialogSettings(), getClass().getName());
		String filterPath = dialogSettings.get(key);
		if (filterPath == null) {
			filterPath = System.getProperty("user.home"); //$NON-NLS-1$
		}
		return filterPath;
	}

	private void setFilterPath(String key, String filterPath) {
		if (filterPath != null) {
			IDialogSettings dialogSettings = DialogSettings
					.getOrCreateSection(ProvSDKUIActivator.getDefault().getDialogSettings(), getClass().getName());
			dialogSettings.put(key, filterPath);
		}
	}

	private String getBundleName(Bundle bundle) {
		String value = bundle.getHeaders().get(Constants.BUNDLE_NAME);
		return value == null ? bundle.getSymbolicName() : Platform.getResourceString(bundle, value);
	}

	@Override
	protected void performDefaults() {
		trustedKeys = certificateChecker.getPreferenceTrustedKeys();
		updateInput();
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		if (dirty) {
			return certificateChecker.persistTrustedKeys(trustedKeys).isOK();
		}
		return true;
	}

}
