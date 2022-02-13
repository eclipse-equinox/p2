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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.processors.pgp.PGPPublicKeyStore;
import org.eclipse.equinox.internal.p2.engine.phases.CertificateChecker;
import org.eclipse.equinox.internal.p2.ui.dialogs.PGPPublicKeyViewDialog;
import org.eclipse.equinox.internal.p2.ui.viewers.CertificateLabelProvider;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
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
	private Set<Certificate> trustedCertificates;
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
		trustedCertificates = new LinkedHashSet<>(certificateChecker.getPreferenceTrustedCertificates());
		contributedTrustedKeys = certificateChecker.getContributedTrustedKeys();
		trustedKeys = certificateChecker.getPreferenceTrustedKeys();

		Composite res = new Composite(parent, SWT.NONE);
		res.setLayout(new GridLayout(2, false));

		// Ensure that the message supports wrapping for a long text message.
		GridData data = new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1);
		data.widthHint = convertWidthInCharsToPixels(90);
		LabelFactory factory = WidgetFactory.label(SWT.WRAP).text(ProvSDKMessages.TrustPreferencePage_pgpIntro)
				.font(parent.getFont()).layoutData(data);
		factory.create(res);

		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		Composite tableComposite = WidgetFactory.composite(SWT.NONE)
				.layoutData(new GridData(SWT.FILL, SWT.FILL, true, true)).layout(tableColumnLayout).create(res);
		Table keyTable = WidgetFactory.table(SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION)
				.headerVisible(true).linesVisible(true).font(parent.getFont()).create(tableComposite);
		viewer = new TableViewer(keyTable);
		keyTable.setHeaderVisible(true);
		viewer.setContentProvider(new ArrayContentProvider());

		// This column is packed later.
		TableViewerColumn typeColumn = createColumn(viewer, ProvSDKMessages.TrustPreferencePage_TypeColumn,
				key -> "PGP", cert -> "x509", tableColumnLayout, 1); //$NON-NLS-1$ //$NON-NLS-2$

		createColumn(viewer, ProvSDKMessages.TrustPreferencePage_FingerprintIdColumn,
				key -> PGPPublicKeyService.toHex(key.getFingerprint()).toUpperCase(Locale.ROOT),
				cert -> cert.getSerialNumber().toString(), tableColumnLayout, 10);

		createColumn(viewer, ProvSDKMessages.TrustPreferencePage_NameColumn, key -> {
			List<String> userIds = new ArrayList<>();
			key.getUserIDs().forEachRemaining(userIds::add);
			return String.join(", ", userIds); //$NON-NLS-1$
		}, cert -> CertificateLabelProvider.getText(cert), tableColumnLayout, 15);

		createColumn(viewer, ProvSDKMessages.TrustPreferencePage_Contributor, key -> {
			{
				Set<String> contributors = new LinkedHashSet<>();
				if (trustedKeys.all().contains(key)) {
					contributors.add(ProvSDKMessages.TrustPreferencePage_PreferenceContributor);
				}
				Set<Bundle> bundles = contributedTrustedKeys.get(key);
				if (bundles != null) {
					Set<String> bundleContributors = new TreeSet<>(Policy.getComparator());
					bundles.stream().map(bundle -> getBundleName(bundle)).forEach(bundleContributors::add);
					contributors.addAll(bundleContributors);
				}
				return String.join(", ", contributors); //$NON-NLS-1$
			}
		}, cert -> ProvSDKMessages.TrustPreferencePage_PreferenceContributor, tableColumnLayout,
				contributedTrustedKeys.isEmpty() ? 8 : 15);

		createColumn(viewer, ProvSDKMessages.TrustPreferencePage_ValidityColumn, pgp -> {
			if (pgp.getCreationTime().after(Date.from(Instant.now()))) {
				return NLS.bind(ProvSDKMessages.TrustPreferencePage_DateNotYetValid, pgp.getCreationTime());
			}
			long validSeconds = pgp.getValidSeconds();
			if (validSeconds == 0) {
				return ProvSDKMessages.TrustPreferencePage_DateValid;
			}
			Instant expires = pgp.getCreationTime().toInstant().plus(validSeconds, ChronoUnit.SECONDS);
			return expires.isBefore(Instant.now())
					? NLS.bind(ProvSDKMessages.TrustPreferencePage_DateExpiredSince, expires)
					: NLS.bind(ProvSDKMessages.TrustPreferencePage_DataValidExpires, expires);
		}, x509 -> {
			try {
				x509.checkValidity();
				return ProvSDKMessages.TrustPreferencePage_DateValid;
			} catch (CertificateExpiredException expired) {
				return ProvSDKMessages.TrustPreferencePage_DateExpired;
			} catch (CertificateNotYetValidException notYetValid) {
				return ProvSDKMessages.TrustPreferencePage_DateNotYetvalid;
			}
		}, tableColumnLayout, 8);

		updateInput();

		Composite buttonComposite = createVerticalButtonBar(res);
		buttonComposite.setLayoutData(new GridData(SWT.DEFAULT, SWT.BEGINNING, false, false));

		Button exportButton = new Button(buttonComposite, SWT.PUSH);
		exportButton.setText(ProvSDKMessages.TrustPreferencePage_export);
		setVerticalButtonLayoutData(exportButton);
		exportButton.setEnabled(false);

		exportButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			Object element = viewer.getStructuredSelection().getFirstElement();
			FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
			dialog.setFilterPath(getFilterPath(EXPORT_FILTER_PATH));
			dialog.setText(ProvSDKMessages.TrustPreferencePage_fileExportTitle);
			dialog.setFilterExtensions(new String[] { "*.asc" }); //$NON-NLS-1$
			FileDialog destination = new FileDialog(exportButton.getShell(), SWT.SAVE);
			destination.setFilterPath(getFilterPath(EXPORT_FILTER_PATH));
			destination.setText(ProvSDKMessages.TrustPreferencePage_Export);
			if (element instanceof X509Certificate) {
				X509Certificate cert = (X509Certificate) element;
				destination.setFilterExtensions(new String[] { "*.der" }); //$NON-NLS-1$
				destination.setFileName(cert.getSerialNumber().toString() + ".der"); //$NON-NLS-1$
				String path = destination.open();
				setFilterPath(EXPORT_FILTER_PATH, destination.getFilterPath());
				if (path == null) {
					return;
				}
				File destinationFile = new File(path);
				try (FileOutputStream output = new FileOutputStream(destinationFile)) {
					output.write(cert.getEncoded());
				} catch (IOException | CertificateEncodingException ex) {
					ProvSDKUIActivator.getDefault().getLog()
							.log(new Status(IStatus.ERROR, ProvSDKUIActivator.PLUGIN_ID, ex.getMessage(), ex));
				}
			} else {
				PGPPublicKey key = (PGPPublicKey) element;
				destination.setFilterExtensions(new String[] { "*.asc" }); //$NON-NLS-1$
				destination.setFileName(PGPPublicKeyService.toHex(key.getFingerprint()).toUpperCase() + ".asc"); //$NON-NLS-1$
				String path = destination.open();
				setFilterPath(EXPORT_FILTER_PATH, destination.getFilterPath());
				if (path == null) {
					return;
				}
				File destinationFile = new File(path);
				try (OutputStream output = new ArmoredOutputStream(new FileOutputStream(destinationFile))) {
					key.encode(output);
				} catch (IOException ex) {
					ProvSDKUIActivator.getDefault().getLog()
							.log(new Status(IStatus.ERROR, ProvSDKUIActivator.PLUGIN_ID, ex.getMessage(), ex));
				}
			}
		}));

		Button addButton = new Button(buttonComposite, SWT.PUSH);
		addButton.setText(ProvSDKMessages.TrustPreferencePage_addPGPKeyButtonLabel);
		addButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
			dialog.setFilterPath(getFilterPath(ADD_FILTER_PATH));
			dialog.setText(ProvSDKMessages.TrustPreferencePage_fileImportTitle);
			dialog.setFilterExtensions(new String[] { "*.asc;*.der" }); //$NON-NLS-1$
			String path = dialog.open();
			setFilterPath(ADD_FILTER_PATH, dialog.getFilterPath());
			if (path == null) {
				return;
			}

			if (path.endsWith(".der")) { //$NON-NLS-1$
				try {
					CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509"); //$NON-NLS-1$
					try (InputStream input = Files.newInputStream(Paths.get(path))) {
						Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(input);
						trustedCertificates.addAll(certificates);
						updateInput();
						viewer.setSelection(new StructuredSelection(certificates.toArray()), true);
					}
				} catch (IOException | CertificateException ex) {
					ProvSDKUIActivator.getDefault().getLog()
							.log(new Status(IStatus.ERROR, ProvSDKUIActivator.PLUGIN_ID, ex.getMessage(), ex));
				}
			} else {
				HashSet<Object> oldKeys = new HashSet<>(trustedKeys.all());
				trustedKeys.add(new File(path));

				HashSet<Object> newKeys = new HashSet<>(trustedKeys.all());
				newKeys.removeAll(oldKeys);
				updateInput();
				viewer.setSelection(new StructuredSelection(newKeys.toArray()), true);
			}
			dirty = true;
		}));
		setVerticalButtonLayoutData(addButton);

		Button removeButton = new Button(buttonComposite, SWT.PUSH);
		removeButton.setText(ProvSDKMessages.TrustPreferencePage_removePGPKeyButtonLabel);
		removeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			for (Object key : getSelectedKeys()) {
				if (key instanceof PGPPublicKey) {
					trustedKeys.remove((PGPPublicKey) key);
				} else {
					trustedCertificates.remove(key);
				}
			}
			updateInput();
			dirty = true;
		}));
		removeButton.setEnabled(false);
		setVerticalButtonLayoutData(removeButton);

		Runnable details = () -> {
			Object element = viewer.getStructuredSelection().getFirstElement();
			if (element instanceof X509Certificate) {
				// create and open dialog for certificate chain
				CertificateLabelProvider.openDialog(getShell(), (X509Certificate) element);
			} else {
				new PGPPublicKeyViewDialog(getShell(), (PGPPublicKey) element,
						provisioningAgent.getService(PGPPublicKeyService.class)).open();
			}
		};

		Button detailsButton = new Button(buttonComposite, SWT.PUSH);
		detailsButton.setText(ProvSDKMessages.TrustPreferencePage_Details);
		detailsButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> details.run()));
		detailsButton.setEnabled(false);
		setVerticalButtonLayoutData(detailsButton);

		viewer.addPostSelectionChangedListener(e -> {
			List<Object> selectedKeys = getSelectedKeys();
			exportButton.setEnabled(selectedKeys.size() == 1);
			Collection<PGPPublicKey> keys = trustedKeys.all();
			removeButton.setEnabled(
					selectedKeys.stream().anyMatch(o -> keys.contains(o) || trustedCertificates.contains(o)));
			detailsButton.setEnabled(selectedKeys.size() == 1);
		});

		Button trustAllButton = WidgetFactory.button(SWT.CHECK).text(ProvSDKMessages.TrustPreferencePage_TrustAll)
				.font(JFaceResources.getDialogFont()).create(res);
		setButtonLayoutData(trustAllButton).verticalSpan = 2;
		trustAllButton.setSelection(certificateChecker.isTrustAlways());
		trustAllButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (trustAllButton.getSelection()) {
				// Prompt the user to ensure they really understand what they've chosen, the
				// risk, and where the preference is stored if they wish to change it in the
				// future. Also ensure that the default button is no so that they must
				// explicitly click the yes button, not just hit enter.
				MessageDialog messageDialog = new MessageDialog(getShell(),
						ProvSDKMessages.TrustPreferencePage_TrustAllConfirmationTitle, null,
						ProvSDKMessages.TrustPreferencePage_TrustAllConfirmationDescription, MessageDialog.QUESTION,
						new String[] { ProvSDKMessages.TrustPreferencePage_TrustAllYes,
								ProvSDKMessages.TrustPreferencePage_TrustAllNo },
						1) {
					@Override
					public Image getImage() {
						return getWarningImage();
					}
				};
				int result = messageDialog.open();
				if (result != Window.OK) {
					certificateChecker.setTrustAlways(false);
					// Restore the setting.
					trustAllButton.setSelection(false);
				} else {
					certificateChecker.setTrustAlways(true);
				}

			}
		}));

		viewer.addDoubleClickListener(e -> details.run());

		typeColumn.getColumn().pack();

		createMenu();

		return res;
	}

	private void createMenu() {
		Control control = viewer.getControl();
		Menu menu = new Menu(control);
		control.setMenu(menu);
		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(ProvSDKMessages.TrustPreferencePage_CopyFingerprint);
		item.addSelectionListener(widgetSelectedAdapter(e -> {
			Object element = viewer.getStructuredSelection().getFirstElement();
			if (element instanceof PGPPublicKey) {
				Clipboard clipboard = new Clipboard(getShell().getDisplay());
				clipboard.setContents(new Object[] {
						PGPPublicKeyService.toHex(((PGPPublicKey) element).getFingerprint()).toUpperCase(Locale.ROOT) },
						new Transfer[] { TextTransfer.getInstance() });
				clipboard.dispose();
			}
		}));

		viewer.addSelectionChangedListener(
				e -> item.setEnabled(viewer.getStructuredSelection().getFirstElement() instanceof PGPPublicKey));
	}

	private TableViewerColumn createColumn(TableViewer tableViewer, String text, Function<PGPPublicKey, String> pgpMap,
			Function<X509Certificate, String> x509Map, TableColumnLayout tableColumnLayout, int columnWeight) {
		TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
		column.getColumn().setText(text);
		column.setLabelProvider(new PGPOrX509ColumnLabelProvider(pgpMap, x509Map));

		tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(columnWeight));
		return column;
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

		LinkedHashSet<Object> allInput = new LinkedHashSet<>();
		allInput.addAll(trustedCertificates);
		allInput.addAll(input);
		viewer.setInput(allInput);
	}

	@SuppressWarnings("unchecked")
	private List<Object> getSelectedKeys() {
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
		trustedCertificates = new LinkedHashSet<>(certificateChecker.getPreferenceTrustedCertificates());
		trustedKeys = certificateChecker.getPreferenceTrustedKeys();
		updateInput();
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		if (dirty) {
			IStatus persistTrustedCertificates = certificateChecker.persistTrustedCertificates(trustedCertificates);
			IStatus persistTrustedKeys = certificateChecker.persistTrustedKeys(trustedKeys);
			dirty = false;
			return persistTrustedKeys.isOK() && persistTrustedCertificates.isOK();
		}
		return true;
	}

	private static class PGPOrX509ColumnLabelProvider extends ColumnLabelProvider {
		private Function<PGPPublicKey, String> pgpMap;
		private Function<X509Certificate, String> x509map;

		public PGPOrX509ColumnLabelProvider(Function<PGPPublicKey, String> pgpMap,
				Function<X509Certificate, String> x509map) {
			this.pgpMap = pgpMap;
			this.x509map = x509map;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof PGPPublicKey) {
				return pgpMap.apply((PGPPublicKey) element);
			}
			if (element instanceof X509Certificate) {
				return x509map.apply((X509Certificate) element);
			}
			return super.getText(element);
		}
	}

}
