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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.artifact.processors.pgp.PGPPublicKeyStore;
import org.eclipse.equinox.internal.p2.engine.phases.AuthorityChecker;
import org.eclipse.equinox.internal.p2.engine.phases.CertificateChecker;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.p2.repository.Transport.ProtocolRule;
import org.eclipse.equinox.internal.p2.ui.dialogs.PGPPublicKeyViewDialog;
import org.eclipse.equinox.internal.p2.ui.viewers.CertificateLabelProvider;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

public class TrustPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String DEFAULT_AUTHORITY_PREFIX = "https://"; //$NON-NLS-1$
	private static final String EXPORT_FILTER_PATH = "exportFilterPath"; //$NON-NLS-1$
	private static final String ADD_FILTER_PATH = "addFilterPath"; //$NON-NLS-1$

	private static final List<Certificate> TBD = List.of();

	private boolean dirtyCertificates;
	private boolean artifactsTrustAlways;
	private CertificateChecker certificateChecker;
	private Set<Certificate> trustedCertificates;
	private PGPPublicKeyStore trustedKeys;
	private Map<PGPPublicKey, Set<Bundle>> contributedTrustedKeys;
	private TableViewer certificateViewer;

	private boolean dirtyAuthorities;
	private boolean authoritiesTrustAlways;
	private TableViewer authorityViewer;
	private AuthorityChecker authorityChecker;
	private Set<URI> trustedAuthorities;
	private ConcurrentHashMap<URI, List<Certificate>> authorityCertificates = new ConcurrentHashMap<>();
	private Job authorityCertificatesJob;
	private Transport transport;
	private Map<String, ProtocolRule> protocolRules;
	private final List<Runnable> restoreProtocolRules = new ArrayList<>();

	public TrustPreferencePage() {
		super(ProvSDKMessages.TrustPreferencePage_title);
	}

	@Override
	public void init(IWorkbench workbench) {
		// nothing to do
	}

	@Override
	protected Control createContents(Composite parent) {
		var provisioningAgent = ProvSDKUIActivator.getDefault().getProvisioningAgent();
		var profile = provisioningAgent.getService(IProfileRegistry.class).getProfile(IProfileRegistry.SELF);

		authorityChecker = new AuthorityChecker(provisioningAgent, profile);
		trustedAuthorities = new TreeSet<>(authorityChecker.getPreferenceTrustedAuthorities());
		authoritiesTrustAlways = authorityChecker.isTrustAlways();

		var display = parent.getDisplay();
		authorityCertificatesJob = new Job(ProvSDKMessages.TrustPreferencePage_CertificatesJobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				var uris = new LinkedHashSet<URI>();
				authorityCertificates.forEach((key, value) -> {
					if (value == TBD) {
						uris.add(key);
					}
				});

				var certificates = AuthorityChecker.getCertificates(uris, monitor);
				authorityCertificates.putAll(certificates);

				if (!parent.isDisposed()) {
					display.asyncExec(() -> {
						if (!parent.isDisposed()) {
							authorityViewer.refresh(true);
						}
					});
				}

				return Status.OK_STATUS;
			}
		};
		authorityCertificatesJob.setSystem(true);

		if (!trustedAuthorities.isEmpty()) {
			for (var authority : trustedAuthorities) {
				putCertificates(authority, TBD);
			}
		}
		authorityCertificatesJob.schedule();

		var keyService = provisioningAgent.getService(PGPPublicKeyService.class);
		certificateChecker = new CertificateChecker(provisioningAgent);
		certificateChecker.setProfile(profile);
		trustedCertificates = new LinkedHashSet<>(certificateChecker.getPreferenceTrustedCertificates());
		contributedTrustedKeys = certificateChecker.getContributedTrustedKeys();
		trustedKeys = certificateChecker.getPreferenceTrustedKeys();
		artifactsTrustAlways = certificateChecker.isTrustAlways();

		var tabFolder = new TabFolder(parent, SWT.NONE);

		var artifactsTab = new TabItem(tabFolder, SWT.NONE);
		artifactsTab.setText(ProvSDKMessages.TrustPreferencePage_ArtifactsTabName);
		var artifactsComposite = new Composite(tabFolder, SWT.NONE);
		artifactsComposite.setBackground(parent.getBackground());
		artifactsComposite.setLayout(new GridLayout(2, false));
		artifactsTab.setControl(artifactsComposite);
		createArtifactsTab(artifactsComposite, keyService, parent.getFont());

		transport = provisioningAgent.getService(Transport.class);
		protocolRules = new LinkedHashMap<>(transport.getProtocolRules());

		var authoritiesTab = new TabItem(tabFolder, SWT.NONE);
		authoritiesTab.setText(ProvSDKMessages.TrustPreferencePage_AuthoritiesTabName);
		var authoritiesComposite = new Composite(tabFolder, SWT.NONE);
		authoritiesComposite.setBackground(parent.getBackground());
		authoritiesComposite.setLayout(new GridLayout(2, false));
		createAuthoritiesTab(authoritiesComposite, parent.getFont());
		authoritiesTab.setControl(authoritiesComposite);

		return tabFolder;
	}

	private void createArtifactsTab(Composite res, PGPPublicKeyService keyService, Font font) {
		// Ensure that the message supports wrapping for a long text message.
		var data = new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1);
		data.widthHint = convertWidthInCharsToPixels(90);
		var factory = WidgetFactory.label(SWT.WRAP).text(ProvSDKMessages.TrustPreferencePage_pgpIntro).font(font)
				.layoutData(data);
		factory.create(res);

		var tableColumnLayout = new TableColumnLayout();
		var tableComposite = WidgetFactory.composite(SWT.NONE).layoutData(new GridData(SWT.FILL, SWT.FILL, true, true))
				.layout(tableColumnLayout).create(res);
		var table = WidgetFactory.table(SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION)
				.headerVisible(true).linesVisible(true).font(font).create(tableComposite);
		certificateViewer = new TableViewer(table);
		table.setHeaderVisible(true);
		certificateViewer.setContentProvider(new ArrayContentProvider());

		// This column is packed later.
		var typeColumn = createColumn(certificateViewer, ProvSDKMessages.TrustPreferencePage_TypeColumn, key -> "PGP", //$NON-NLS-1$
				cert -> "x509", tableColumnLayout, 1); //$NON-NLS-1$

		createColumn(certificateViewer, ProvSDKMessages.TrustPreferencePage_FingerprintIdColumn,
				key -> PGPPublicKeyService.toHexFingerprint(key), cert -> cert.getSerialNumber().toString(),
				tableColumnLayout, 10);

		createColumn(certificateViewer, ProvSDKMessages.TrustPreferencePage_NameColumn, key -> {
			List<String> userIds = new ArrayList<>();
			key.getUserIDs().forEachRemaining(userIds::add);
			return String.join(", ", userIds); //$NON-NLS-1$
		}, cert -> CertificateLabelProvider.getText(cert), tableColumnLayout, 15);

		createColumn(certificateViewer, ProvSDKMessages.TrustPreferencePage_Contributor, key -> {
			{
				var contributors = new LinkedHashSet<String>();
				if (trustedKeys.all().contains(key)) {
					contributors.add(ProvSDKMessages.TrustPreferencePage_PreferenceContributor);
				}
				var bundles = contributedTrustedKeys.get(key);
				if (bundles != null) {
					var bundleContributors = new TreeSet<String>(Policy.getComparator());
					bundles.stream().map(bundle -> getBundleName(bundle)).forEach(bundleContributors::add);
					contributors.addAll(bundleContributors);
				}
				return String.join(", ", contributors); //$NON-NLS-1$
			}
		}, cert -> ProvSDKMessages.TrustPreferencePage_PreferenceContributor, tableColumnLayout,
				contributedTrustedKeys.isEmpty() ? 8 : 15);

		createColumn(certificateViewer, ProvSDKMessages.TrustPreferencePage_ValidityColumn, pgp -> {
			if (keyService != null) {
				var verifiedRevocationDate = keyService.getVerifiedRevocationDate(pgp);
				if (verifiedRevocationDate != null) {
					return NLS.bind(ProvSDKMessages.TrustPreferencePage_RevokedPGPKey, verifiedRevocationDate);
				}
			}
			if (pgp.getCreationTime().after(Date.from(Instant.now()))) {
				return NLS.bind(ProvSDKMessages.TrustPreferencePage_DateNotYetValid, pgp.getCreationTime());
			}
			var validSeconds = pgp.getValidSeconds();
			if (validSeconds == 0) {
				return ProvSDKMessages.TrustPreferencePage_DateValid;
			}
			var expires = pgp.getCreationTime().toInstant().plus(validSeconds, ChronoUnit.SECONDS);
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

		updateCertificateInput();

		var buttonComposite = createVerticalButtonBar(res);
		buttonComposite.setLayoutData(new GridData(SWT.DEFAULT, SWT.BEGINNING, false, false));

		var exportButton = new Button(buttonComposite, SWT.PUSH);
		exportButton.setText(ProvSDKMessages.TrustPreferencePage_export);
		setVerticalButtonLayoutData(exportButton);
		exportButton.setEnabled(false);

		exportButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			var element = certificateViewer.getStructuredSelection().getFirstElement();
			var dialog = new FileDialog(getShell(), SWT.SAVE);
			dialog.setFilterPath(getFilterPath(EXPORT_FILTER_PATH));
			dialog.setText(ProvSDKMessages.TrustPreferencePage_fileExportTitle);
			dialog.setFilterExtensions(new String[] { "*.asc" }); //$NON-NLS-1$
			var destination = new FileDialog(exportButton.getShell(), SWT.SAVE);
			destination.setFilterPath(getFilterPath(EXPORT_FILTER_PATH));
			destination.setText(ProvSDKMessages.TrustPreferencePage_Export);
			if (element instanceof X509Certificate certificate) {
				destination.setFilterExtensions(new String[] { "*.der" }); //$NON-NLS-1$
				destination.setFileName(certificate.getSerialNumber().toString() + ".der"); //$NON-NLS-1$
				var path = destination.open();
				setFilterPath(EXPORT_FILTER_PATH, destination.getFilterPath());
				if (path == null) {
					return;
				}
				var destinationFile = new File(path);
				try (var output = new FileOutputStream(destinationFile)) {
					output.write(certificate.getEncoded());
				} catch (IOException | CertificateEncodingException ex) {
					ProvSDKUIActivator.getDefault().getLog()
							.log(new Status(IStatus.ERROR, ProvSDKUIActivator.PLUGIN_ID, ex.getMessage(), ex));
				}
			} else {
				var key = (PGPPublicKey) element;
				destination.setFilterExtensions(new String[] { "*.asc" }); //$NON-NLS-1$
				destination.setFileName(PGPPublicKeyService.toHexFingerprint(key) + ".asc"); //$NON-NLS-1$
				var path = destination.open();
				setFilterPath(EXPORT_FILTER_PATH, destination.getFilterPath());
				if (path == null) {
					return;
				}
				var destinationFile = new File(path);
				try (var output = new ArmoredOutputStream(new FileOutputStream(destinationFile))) {
					key.encode(output);
				} catch (IOException ex) {
					ProvSDKUIActivator.getDefault().getLog()
							.log(new Status(IStatus.ERROR, ProvSDKUIActivator.PLUGIN_ID, ex.getMessage(), ex));
				}
			}
		}));

		var addButton = new Button(buttonComposite, SWT.PUSH);
		addButton.setText(ProvSDKMessages.TrustPreferencePage_addPGPKeyButtonLabel);
		addButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			var dialog = new FileDialog(getShell(), SWT.OPEN);
			dialog.setFilterPath(getFilterPath(ADD_FILTER_PATH));
			dialog.setText(ProvSDKMessages.TrustPreferencePage_fileImportTitle);
			dialog.setFilterExtensions(new String[] { "*.asc;*.der" }); //$NON-NLS-1$
			var path = dialog.open();
			setFilterPath(ADD_FILTER_PATH, dialog.getFilterPath());
			if (path == null) {
				return;
			}

			if (path.endsWith(".der")) { //$NON-NLS-1$
				try {
					var certificateFactory = CertificateFactory.getInstance("X.509"); //$NON-NLS-1$
					try (var input = Files.newInputStream(Paths.get(path))) {
						var certificates = certificateFactory.generateCertificates(input);
						trustedCertificates.addAll(certificates);
						updateCertificateInput();
						certificateViewer.setSelection(new StructuredSelection(certificates.toArray()), true);
					}
				} catch (IOException | CertificateException ex) {
					ProvSDKUIActivator.getDefault().getLog()
							.log(new Status(IStatus.ERROR, ProvSDKUIActivator.PLUGIN_ID, ex.getMessage(), ex));
				}
			} else {
				var oldKeys = new HashSet<>(trustedKeys.all());
				trustedKeys.add(new File(path));

				var newKeys = new HashSet<>(trustedKeys.all());
				newKeys.removeAll(oldKeys);
				updateCertificateInput();
				certificateViewer.setSelection(new StructuredSelection(newKeys.toArray()), true);
			}
			dirtyCertificates = true;
		}));
		setVerticalButtonLayoutData(addButton);

		var removeButton = new Button(buttonComposite, SWT.PUSH);
		removeButton.setText(ProvSDKMessages.TrustPreferencePage_removePGPKeyButtonLabel);
		removeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			for (var key : getSelectedKeys()) {
				if (key instanceof PGPPublicKey pgp) {
					trustedKeys.remove(pgp);
				} else {
					trustedCertificates.remove(key);
				}
			}
			updateCertificateInput();
			dirtyCertificates = true;
		}));
		removeButton.setEnabled(false);
		setVerticalButtonLayoutData(removeButton);

		Runnable details = () -> {
			Object element = certificateViewer.getStructuredSelection().getFirstElement();
			if (element instanceof X509Certificate certificate) {
				// create and open dialog for certificate chain
				CertificateLabelProvider.openDialog(getShell(), certificate);
			} else {
				new PGPPublicKeyViewDialog(getShell(), (PGPPublicKey) element, keyService).open();
			}
		};

		var detailsButton = new Button(buttonComposite, SWT.PUSH);
		detailsButton.setText(ProvSDKMessages.TrustPreferencePage_Details);
		detailsButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> details.run()));
		detailsButton.setEnabled(false);
		setVerticalButtonLayoutData(detailsButton);

		certificateViewer.addPostSelectionChangedListener(e -> {
			var selectedKeys = getSelectedKeys();
			exportButton.setEnabled(selectedKeys.size() == 1);
			var keys = trustedKeys.all();
			removeButton.setEnabled(
					selectedKeys.stream().anyMatch(o -> keys.contains(o) || trustedCertificates.contains(o)));
			detailsButton.setEnabled(selectedKeys.size() == 1);
		});

		var trustAllButton = WidgetFactory.button(SWT.CHECK).text(ProvSDKMessages.TrustPreferencePage_TrustAll)
				.font(JFaceResources.getDialogFont()).create(res);
		setButtonLayoutData(trustAllButton).verticalSpan = 2;
		trustAllButton.setSelection(artifactsTrustAlways);
		trustAllButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (trustAllButton.getSelection()) {
				// Prompt the user to ensure they really understand what they've chosen, the
				// risk, and where the preference is stored if they wish to change it in the
				// future. Also ensure that the default button is no so that they must
				// explicitly click the yes button, not just hit enter.
				var messageDialog = new MessageDialog(getShell(),
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
				var result = messageDialog.open();
				if (result != Window.OK) {
					// Restore the setting.
					trustAllButton.setSelection(false);
				} else {
					artifactsTrustAlways = true;
					dirtyCertificates = true;
				}
			} else {
				artifactsTrustAlways = false;
				dirtyCertificates = true;
			}
		}));

		certificateViewer.addDoubleClickListener(e -> details.run());

		typeColumn.getColumn().pack();

		var menu = new Menu(table);
		table.setMenu(menu);
		var item = new MenuItem(menu, SWT.PUSH);
		item.setText(ProvSDKMessages.TrustPreferencePage_CopyFingerprint);
		item.addSelectionListener(widgetSelectedAdapter(e -> {
			var element = certificateViewer.getStructuredSelection().getFirstElement();
			if (element instanceof PGPPublicKey pgp) {
				var clipboard = new Clipboard(getShell().getDisplay());
				clipboard.setContents(new Object[] { PGPPublicKeyService.toHexFingerprint(pgp) },
						new Transfer[] { TextTransfer.getInstance() });
				clipboard.dispose();
			}
		}));

		certificateViewer.addSelectionChangedListener(e -> item
				.setEnabled(certificateViewer.getStructuredSelection().getFirstElement() instanceof PGPPublicKey));
	}

	private void createAuthoritiesTab(Composite res, Font font) {
		// Ensure that the message supports wrapping for a long text message.
		var data = new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1);
		data.widthHint = convertWidthInCharsToPixels(90);
		var factory = WidgetFactory.label(SWT.WRAP).text(ProvSDKMessages.TrustPreferencePage_AuthoritiesTabDescription)
				.font(font).layoutData(data);
		factory.create(res);

		var tableColumnLayout = new TableColumnLayout();
		var tableComposite = WidgetFactory.composite(SWT.NONE).layoutData(new GridData(SWT.FILL, SWT.FILL, true, true))
				.layout(tableColumnLayout).create(res);
		var table = WidgetFactory.table(SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION)
				.headerVisible(true).linesVisible(true).font(font).create(tableComposite);
		authorityViewer = new TableViewer(table);
		table.setHeaderVisible(true);
		authorityViewer.setContentProvider(new ArrayContentProvider());

		createColumn(authorityViewer, ProvSDKMessages.TrustPreferencePage_AuthorityColumnTitle,
				new ColumnLabelProvider(), tableColumnLayout, 10);

		var securedColumn = createColumn(authorityViewer, ProvSDKMessages.TrustPreferencePage_SecuredColumnTitle,
				new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						var certificates = getCertificates((URI) element);
						if (certificates == TBD) {
							return ProvSDKMessages.TrustPreferencePage_ComputingAuthoritySecurity;
						} else if (certificates.isEmpty()) {
							return ProvSDKMessages.TrustPreferencePage_InsecureAuthority;
						} else {
							return ProvSDKMessages.TrustPreferencePage_SecureAuthority;
						}
					}
				}, tableColumnLayout, 1);

		updateAuthorityInput();

		var buttonComposite = createVerticalButtonBar(res);
		buttonComposite.setLayoutData(new GridData(SWT.DEFAULT, SWT.BEGINNING, false, false));

		var addButton = new Button(buttonComposite, SWT.PUSH);
		addButton.setText(ProvSDKMessages.TrustPreferencePage_addPGPKeyButtonLabel);
		addButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			var inputDialog = new InputDialog(getShell(), ProvSDKMessages.TrustPreferencePage_AddAuthorityTitle,
					ProvSDKMessages.TrustPreferencePage_AddAuthorityDescription, DEFAULT_AUTHORITY_PREFIX, it -> {
						try {
							new URI(it);
						} catch (URISyntaxException ex) {
							if (DEFAULT_AUTHORITY_PREFIX.equals(it)) {
								return ProvSDKMessages.TrustPreferencePage_EmptyHostNameMessage;
							}
							return ex.getLocalizedMessage();
						}
						return null;
					});
			var open = inputDialog.open();
			if (open == Window.OK) {
				var authorityURI = URI.create(inputDialog.getValue());

				// Avoid URIs like https://host/ in favor of https://host which actual works.
				var authorityChain = AuthorityChecker.getAuthorityChain(authorityURI);
				var mainAuthority = authorityChain.get(0);
				if (authorityChain.size() == 2) {
					if ((mainAuthority + "/").equals(authorityChain.get(1).toString())) { //$NON-NLS-1$
						authorityURI = mainAuthority;
					}
				}

				trustedAuthorities.add(authorityURI);
				var filteredAuthorities = AuthorityChecker.getFilteredAuthorities(trustedAuthorities);
				trustedAuthorities.clear();
				trustedAuthorities.addAll(filteredAuthorities);
				authorityCertificates.put(mainAuthority, TBD);
				authorityCertificatesJob.schedule();
				updateAuthorityInput();
				dirtyAuthorities = true;
			}

		}));
		setVerticalButtonLayoutData(addButton);

		var removeButton = new Button(buttonComposite, SWT.PUSH);
		removeButton.setText(ProvSDKMessages.TrustPreferencePage_removePGPKeyButtonLabel);
		removeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			trustedAuthorities.removeAll(getSelectedAuthorities());
			updateAuthorityInput();
			dirtyAuthorities = true;
		}));
		removeButton.setEnabled(false);
		setVerticalButtonLayoutData(removeButton);

		Runnable details = () -> {
			var element = authorityViewer.getStructuredSelection().getFirstElement();
			var certificates = getCertificates((URI) element);
			if (!certificates.isEmpty()) {
				var certificate = certificates.get(0);
				CertificateLabelProvider.openDialog(getShell(), (X509Certificate) certificate);
			}
		};

		var detailsButton = new Button(buttonComposite, SWT.PUSH);
		detailsButton.setText(ProvSDKMessages.TrustPreferencePage_Details);
		detailsButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> details.run()));
		detailsButton.setEnabled(false);
		setVerticalButtonLayoutData(detailsButton);

		authorityViewer.addPostSelectionChangedListener(e -> {
			var selectedAuthorities = getSelectedAuthorities();
			removeButton.setEnabled(!selectedAuthorities.isEmpty());
			detailsButton.setEnabled(
					!selectedAuthorities.isEmpty() && !getCertificates(selectedAuthorities.get(0)).isEmpty());
		});

		var trustAllButton = WidgetFactory.button(SWT.CHECK).text(ProvSDKMessages.TrustPreferencePage_TrustAll)
				.font(JFaceResources.getDialogFont()).create(res);
		setButtonLayoutData(trustAllButton).verticalSpan = 2;
		trustAllButton.setSelection(authoritiesTrustAlways);
		trustAllButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (trustAllButton.getSelection()) {
				// Prompt the user to ensure they really understand what they've chosen, the
				// risk, and where the preference is stored if they wish to change it in the
				// future. Also ensure that the default button is no so that they must
				// explicitly click the yes button, not just hit enter.
				var messageDialog = new MessageDialog(getShell(),
						ProvSDKMessages.TrustPreferencePage_TrustAllAuthoritiesConfirmationTitle, null,
						ProvSDKMessages.TrustPreferencePage_TrustAllAuthoritiesMessage, MessageDialog.QUESTION,
						new String[] { ProvSDKMessages.TrustPreferencePage_ConfirmTrustAllAuthorities,
								ProvSDKMessages.TrustPreferencePage_RejectTrustAllAuthorities },
						1) {
					@Override
					public Image getImage() {
						return getWarningImage();
					}
				};
				var result = messageDialog.open();
				if (result != Window.OK) {
					// Restore the setting.
					trustAllButton.setSelection(false);
				} else {
					authoritiesTrustAlways = true;
					dirtyAuthorities = true;
				}
			} else {
				authoritiesTrustAlways = false;
				dirtyAuthorities = true;
			}
		}));

		if (!protocolRules.isEmpty()) {
			var defaultProtocolRules = transport.getDefaultProtocolRules();
			var groupLayoutData = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
			groupLayoutData.verticalIndent = 5;
			var protocolRulesGroup = WidgetFactory.group(SWT.NONE).text(ProvSDKMessages.TrustPreferencePage_ProtocolRulesGroupLabel)
					.layout(new GridLayout(2, false)).layoutData(groupLayoutData).create(res);
			for (var entry : protocolRules.entrySet()) {
				var protocol = entry.getKey();
				WidgetFactory.label(SWT.NONE).text(protocol + ':').create(protocolRulesGroup);

				var combo = new Combo(protocolRulesGroup, SWT.READ_ONLY);
				var items = new LinkedHashMap<String, ProtocolRule>();
				var defaultItem = NLS.bind(ProvSDKMessages.TrustPreferencePage_DefaultProtocolRuleQualifier, getProtocolRuleLabel(defaultProtocolRules.get(protocol)));
				items.put(defaultItem, null);
				for (var value : ProtocolRule.values()) {
					items.put(getProtocolRuleLabel(value), value);
				}
				combo.setItems(items.keySet().toArray(String[]::new));

				var rule = entry.getValue();
				restoreProtocolRules.add(() -> combo.setText(rule == null ? defaultItem : getProtocolRuleLabel(rule)));

				combo.addModifyListener(e -> {
					var newRule = items.get(combo.getText());
					protocolRules.put(protocol, newRule);
				});
			}

			restoreProtocolRules.forEach(Runnable::run);
		}

		authorityViewer.addDoubleClickListener(e -> details.run());

		securedColumn.getColumn().pack();

		var menu = new Menu(table);
		table.setMenu(menu);
		var item = new MenuItem(menu, SWT.PUSH);
		item.setText(ProvSDKMessages.TrustPreferencePage_CopyLinkMenuItem);
		item.addSelectionListener(widgetSelectedAdapter(e -> {
			URI uri = getSelectedAuthorities().get(0);
			Clipboard clipboard = new Clipboard(getShell().getDisplay());
			clipboard.setContents(new Object[] { uri.toString() }, new Transfer[] { TextTransfer.getInstance() });
			clipboard.dispose();
		}));

		authorityViewer.addSelectionChangedListener(e -> item.setEnabled(!getSelectedAuthorities().isEmpty()));
	}

	private TableViewerColumn createColumn(TableViewer tableViewer, String text, Function<PGPPublicKey, String> pgpMap,
			Function<X509Certificate, String> x509Map, TableColumnLayout tableColumnLayout, int columnWeight) {
		var column = new TableViewerColumn(tableViewer, SWT.NONE);
		column.getColumn().setText(text);
		column.setLabelProvider(new PGPOrX509ColumnLabelProvider(pgpMap, x509Map));

		tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(columnWeight));
		return column;
	}

	private TableViewerColumn createColumn(TableViewer tableViewer, String text, ColumnLabelProvider labelProvider,
			TableColumnLayout tableColumnLayout, int columnWeight) {
		var column = new TableViewerColumn(tableViewer, SWT.NONE);
		column.getColumn().setText(text);
		column.setLabelProvider(labelProvider);

		tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(columnWeight));
		return column;
	}

	private void updateInput() {
		updateCertificateInput();
		updateAuthorityInput();
	}

	private void updateCertificateInput() {
		Collection<PGPPublicKey> all = trustedKeys.all();
		var input = new TreeSet<PGPPublicKey>((k1, k2) -> {
			var contains1 = all.contains(k1);
			var contains2 = all.contains(k2);
			if (contains1 != contains2) {
				if (contains1) {
					return -1;
				}
				return 1;
			}
			return PGPPublicKeyService.toHexFingerprint(k1).compareTo(PGPPublicKeyService.toHexFingerprint(k2));
		});
		input.addAll(all);
		input.addAll(contributedTrustedKeys.keySet());

		var allInput = new LinkedHashSet<>();
		allInput.addAll(trustedCertificates);
		allInput.addAll(input);
		certificateViewer.setInput(allInput);
	}

	private void updateAuthorityInput() {
		authorityViewer.setInput(trustedAuthorities);
	}

	private void putCertificates(URI uri, List<Certificate> certificates) {
		authorityCertificates.put(AuthorityChecker.getAuthorityChain(uri).get(0), certificates);
	}

	private List<Certificate> getCertificates(URI uri) {
		return authorityCertificates.get(AuthorityChecker.getAuthorityChain(uri).get(0));
	}

	@SuppressWarnings("unchecked")
	private List<Object> getSelectedKeys() {
		return certificateViewer.getStructuredSelection().toList();
	}

	@SuppressWarnings("unchecked")
	private List<URI> getSelectedAuthorities() {
		return authorityViewer.getStructuredSelection().toList();
	}

	private Composite createVerticalButtonBar(Composite parent) {
		// Create composite.
		var composite = new Composite(parent, SWT.NONE);
		initializeDialogUnits(composite);

		// create a layout with spacing and margins appropriate for the font
		// size.
		var layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 5;
		layout.marginHeight = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);

		return composite;
	}

	private GridData setVerticalButtonLayoutData(Button button) {
		var data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		var widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		var minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = Math.max(widthHint, minSize.x);
		button.setLayoutData(data);
		return data;
	}

	private String getFilterPath(String key) {
		var dialogSettings = DialogSettings.getOrCreateSection(ProvSDKUIActivator.getDefault().getDialogSettings(),
				getClass().getName());
		var filterPath = dialogSettings.get(key);
		if (filterPath == null) {
			filterPath = System.getProperty("user.home"); //$NON-NLS-1$
		}
		return filterPath;
	}

	private void setFilterPath(String key, String filterPath) {
		if (filterPath != null) {
			var dialogSettings = DialogSettings.getOrCreateSection(ProvSDKUIActivator.getDefault().getDialogSettings(),
					getClass().getName());
			dialogSettings.put(key, filterPath);
		}
	}

	private String getBundleName(Bundle bundle) {
		var value = bundle.getHeaders().get(Constants.BUNDLE_NAME);
		return value == null ? bundle.getSymbolicName() : Platform.getResourceString(bundle, value);
	}

	@Override
	protected void performDefaults() {
		trustedCertificates = new LinkedHashSet<>(certificateChecker.getPreferenceTrustedCertificates());
		trustedKeys = certificateChecker.getPreferenceTrustedKeys();
		trustedAuthorities = authorityChecker.getPreferenceTrustedAuthorities();
		protocolRules = new LinkedHashMap<>(transport.getProtocolRules());
		restoreProtocolRules.forEach(Runnable::run);

		updateInput();
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		if (dirtyCertificates) {
			certificateChecker.setTrustAlways(artifactsTrustAlways);
			certificateChecker.persistTrustedCertificates(trustedCertificates);
			certificateChecker.persistTrustedKeys(trustedKeys);
			dirtyCertificates = false;
		}

		if (dirtyAuthorities) {
			authorityChecker.setTrustAlways(authoritiesTrustAlways);
			authorityChecker.persistTrustedAuthorities(trustedAuthorities);
			dirtyAuthorities = false;
		}

		transport.setProtocolRules(protocolRules);

		return true;
	}

	private static String getProtocolRuleLabel(ProtocolRule rule) {
		if (rule == null) {
			return ProvSDKMessages.TrustPreferencePage_AllowProtocolRule;
		}
		switch (rule) {
		case ALLOW:
			return ProvSDKMessages.TrustPreferencePage_AllowProtocolRule;
		case REDIRECT:
			return ProvSDKMessages.TrustPreferencePage_RedirectProtocolRule;
		case BLOCK:
		default:
			return ProvSDKMessages.TrustPreferencePage_BlockProtocolRule;
		}
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
			if (element instanceof PGPPublicKey pgp) {
				return pgpMap.apply(pgp);
			}
			if (element instanceof X509Certificate certificate) {
				return x509map.apply(certificate);
			}
			return super.getText(element);
		}
	}
}
