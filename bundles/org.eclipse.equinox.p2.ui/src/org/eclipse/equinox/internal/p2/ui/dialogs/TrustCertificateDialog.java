/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *      Fabian Steeg <steeg@hbz-nrw.de> - Bug 474099 - Require certificate selection to confirm dialog
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.*;
import java.security.cert.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.viewers.CertificateLabelProvider;
import org.eclipse.equinox.internal.provisional.security.ui.X500PrincipalHelper;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * A dialog that displays a certificate chain and asks the user if they trust
 * the certificate providers. It also supports prompting about unsigned content.
 */
public class TrustCertificateDialog extends SelectionDialog {
	private static final String EXPORT_FILTER_PATH = "exportFilterPath"; //$NON-NLS-1$

	private CheckboxTableViewer certificateViewer;

	private TreeViewer certificateChainViewer;

	private TableViewer artifactViewer;

	private Button detailsButton;

	private Button exportButton;

	private boolean rememberSelectedSigners = true;

	private boolean trustAlways;

	private final Map<TreeNode, List<IArtifactKey>> artifactMap = new LinkedHashMap<>();

	private final Map<PGPPublicKey, Date> revocationMap = new LinkedHashMap<>();

	public TrustCertificateDialog(Shell parentShell, Object input) {
		super(parentShell);

		setShellStyle(SWT.DIALOG_TRIM | SWT.MODELESS | SWT.RESIZE | SWT.MAX | SWT.ON_TOP | getDefaultOrientation());

		if (input instanceof TreeNode[]) {
			init(null, (TreeNode[]) input);
		}

		setTitle(ProvUIMessages.TrustCertificateDialog_Title);

		boolean unsignedContent = artifactMap.keySet().stream().map(TreeNode::getValue).anyMatch(Objects::isNull);
		boolean pgpContent = containsInstance(input, PGPPublicKey.class);
		boolean revokedPGPContent = pgpContent && !revocationMap.isEmpty();
		boolean certifcateContent = containsInstance(input, Certificate.class);

		List<String> messages = new ArrayList<>();
		if (certifcateContent || pgpContent) {
			messages.add(ProvUIMessages.TrustCertificateDialog_Message);
		}
		if (unsignedContent) {
			messages.add(ProvUIMessages.TrustCertificateDialog_MessageUnsigned);
		}
		if (revokedPGPContent) {
			messages.add(ProvUIMessages.TrustCertificateDialog_MessageRevoked);
		}
		if (certifcateContent || pgpContent) {
			messages.add(ProvUIMessages.TrustCertificateDialog_MessageNameWarning);
		}
		if (pgpContent) {
			messages.add(ProvUIMessages.TrustCertificateDialog_MessagePGP);
		}
		setMessage(String.join("  ", messages)); //$NON-NLS-1$

		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(parentShell, IProvHelpContextIds.TRUST_DIALOG);
		}
	}

	private void init(TreeNode parent, TreeNode[] treeNodes) {
		for (TreeNode node : treeNodes) {
			IArtifactKey[] associatedArtifacts = getInstance(node, IArtifactKey[].class);
			if (associatedArtifacts != null || parent == null) {
				artifactMap.put(node, associatedArtifacts == null ? List.of() : Arrays.asList(associatedArtifacts));
			}

			Date revocation = getInstance(node, Date.class);
			if (revocation != null) {
				PGPPublicKey pgp = getInstance(node, PGPPublicKey.class);
				if (pgp != null) {
					revocationMap.put(pgp, revocation);
				}
			}

			TreeNode[] children = node.getChildren();
			if (children != null) {
				init(node, children);
			}
		}
	}

	public boolean isRememberSelectedSigners() {
		return rememberSelectedSigners;
	}

	public boolean isTrustAlways() {
		return trustAlways;
	}

	@Override
	protected Label createMessageArea(Composite composite) {
		// Ensure that the message supports wrapping for a long text message.
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.widthHint = convertWidthInCharsToPixels(120);
		LabelFactory factory = WidgetFactory.label(SWT.WRAP).font(composite.getFont()).layoutData(data);
		if (getMessage() != null) {
			factory.text(getMessage());
		}
		return factory.create(composite);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite mainComposite = (Composite) super.createDialogArea(parent);
		Dialog.applyDialogFont(mainComposite);
		initializeDialogUnits(mainComposite);

		createMessageArea(mainComposite);

		SashForm sashForm = new SashForm(mainComposite, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createCertificateViewerArea(createSashFormArea(sashForm));

		// Create this area only if we have keys or certificates.
		boolean containsCertificates = containsInstance(artifactMap.keySet(), PGPPublicKey.class)
				|| containsInstance(artifactMap.keySet(), Certificate.class);
		if (containsCertificates) {
			createCertficateChainViewerArea(createSashFormArea(sashForm));
		}

		// Sort the set of all artifacts and create the lower area only if there are
		// artifacts.
		Comparator<Object> comparator = Policy.getComparator();
		Set<IArtifactKey> artifacts = artifactMap.values().stream().flatMap(Collection::stream)
				.collect(Collectors.toCollection(() -> new TreeSet<>((a1, a2) -> {
					int result = comparator.compare(a1.getId(), a2.getId());
					if (result == 0) {
						result = a1.getVersion().compareTo(a2.getVersion());
						if (result == 0) {
							result = a1.getClassifier().compareTo(a2.getClassifier());
						}
					}
					return result;
				})));
		if (!artifacts.isEmpty()) {
			crreateArtifactViewerArea(createSashFormArea(sashForm), artifacts);
		}

		// Set weights based on the children's preferred size.
		Control[] children = sashForm.getChildren();
		int[] weights = new int[children.length];
		for (int i = 0; i < children.length; ++i) {
			weights[i] = children[i].computeSize(SWT.DEFAULT, SWT.DEFAULT, false).y;
		}
		sashForm.setWeights(weights);

		if (!getInitialElementSelections().isEmpty()) {
			checkInitialSelections();
		}

		if (!artifactMap.isEmpty()) {
			certificateViewer.setSelection(new StructuredSelection(artifactMap.keySet().iterator().next()));
		}

		return mainComposite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, ProvUIMessages.TrustCertificateDialog_AcceptSelectedButtonLabel,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		updateOkButton();
	}

	private void createButtons(Composite composite) {
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		buttonComposite.setLayout(new RowLayout());

		detailsButton = new Button(buttonComposite, SWT.NONE);
		detailsButton.setText(ProvUIMessages.TrustCertificateDialog_Details);
		detailsButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				X509Certificate cert = getInstance(certificateChainViewer.getSelection(), X509Certificate.class);
				if (cert != null) {
					CertificateLabelProvider.openDialog(getShell(), cert);
				}
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				widgetDefaultSelected(e);
			}
		});

		exportButton = new Button(buttonComposite, SWT.NONE);
		exportButton.setText(ProvUIMessages.TrustCertificateDialog_Export);
		exportButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				ISelection selection = certificateChainViewer.getSelection();
				X509Certificate cert = getInstance(selection, X509Certificate.class);
				PGPPublicKey key = getInstance(selection, PGPPublicKey.class);
				if (cert != null || key != null) {
					FileDialog destination = new FileDialog(exportButton.getShell(), SWT.SAVE);
					destination.setFilterPath(getFilterPath(EXPORT_FILTER_PATH));
					destination.setText(ProvUIMessages.TrustCertificateDialog_ExportDialogTitle);
					if (cert != null) {
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
							ProvUIActivator.getDefault().getLog()
									.log(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ex.getMessage(), ex));
						}
					} else {
						destination.setFilterExtensions(new String[] { "*.asc" }); //$NON-NLS-1$
						destination.setFileName(userFriendlyFingerPrint(key) + ".asc"); //$NON-NLS-1$
						String path = destination.open();
						setFilterPath(EXPORT_FILTER_PATH, destination.getFilterPath());
						if (path == null) {
							return;
						}
						File destinationFile = new File(path);
						try (OutputStream output = new ArmoredOutputStream(new FileOutputStream(destinationFile))) {
							key.encode(output);
						} catch (IOException ex) {
							ProvUIActivator.getDefault().getLog()
									.log(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ex.getMessage(), ex));
						}
					}
				}
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				widgetDefaultSelected(e);
			}
		});
	}

	private Composite createSashFormArea(SashForm sashForm) {
		Composite composite = new Composite(sashForm, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		return composite;
	}

	private void createCertificateViewerArea(Composite composite) {

		TableColumnLayout tableColumnLayout = new TableColumnLayout(true);
		Composite tableComposite = WidgetFactory.composite(SWT.NONE)
				.layoutData(new GridData(SWT.FILL, SWT.FILL, true, true)).layout(tableColumnLayout).create(composite);
		Table table = WidgetFactory
				.table(SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK)
				.headerVisible(true).linesVisible(true).font(composite.getFont()).create(tableComposite);
		certificateViewer = new CheckboxTableViewer(table);
		certificateViewer.setContentProvider(new TreeNodeContentProvider());

		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = convertHeightInCharsToPixels(Math.min(artifactMap.keySet().size() + 2, 6)) * 3 / 2;
		data.widthHint = convertWidthInCharsToPixels(120);
		tableComposite.setLayoutData(data);

		// This column is packed later.
		TableViewerColumn typeColumn = createColumn(certificateViewer, ProvUIMessages.TrustCertificateDialog_ObjectType,
				new PGPOrX509ColumnLabelProvider(key -> "PGP", cert -> "x509", //$NON-NLS-1$ //$NON-NLS-2$
						ProvUIMessages.TrustCertificateDialog_Unsigned),
				tableColumnLayout, 1);

		createColumn(certificateViewer, ProvUIMessages.TrustCertificateDialog_Id,
				new PGPOrX509ColumnLabelProvider(TrustCertificateDialog::userFriendlyFingerPrint,
						cert -> cert.getSerialNumber().toString(), ProvUIMessages.TrustCertificateDialog_NotApplicable),
				tableColumnLayout, 10);

		createColumn(certificateViewer, ProvUIMessages.TrustCertificateDialog_Name,
				new PGPOrX509ColumnLabelProvider(pgp -> {
					java.util.List<String> users = new ArrayList<>();
					pgp.getUserIDs().forEachRemaining(users::add);
					return String.join(", ", users); //$NON-NLS-1$
				}, x509 -> {
					X500PrincipalHelper principalHelper = new X500PrincipalHelper(x509.getSubjectX500Principal());
					return principalHelper.getCN() + "; " + principalHelper.getOU() + "; " //$NON-NLS-1$ //$NON-NLS-2$
							+ principalHelper.getO();
				}, ProvUIMessages.TrustCertificateDialog_Unknown), tableColumnLayout, 15);

		createColumn(certificateViewer, ProvUIMessages.TrustCertificateDialog_dates,
				new PGPOrX509ColumnLabelProvider(pgp -> {

					Date verifiedRevocationDate = revocationMap.get(pgp);
					if (verifiedRevocationDate != null) {
						return NLS.bind(ProvUIMessages.TrustCertificateDialog_revoked, verifiedRevocationDate);
					}

					if (pgp.getCreationTime().after(Date.from(Instant.now()))) {
						return NLS.bind(ProvUIMessages.TrustCertificateDialog_NotYetValidStartDate,
								pgp.getCreationTime());
					}
					long validSeconds = pgp.getValidSeconds();
					if (validSeconds == 0) {
						return ProvUIMessages.TrustCertificateDialog_valid;
					}
					Instant expires = pgp.getCreationTime().toInstant().plus(validSeconds, ChronoUnit.SECONDS);
					return expires.isBefore(Instant.now())
							? NLS.bind(ProvUIMessages.TrustCertificateDialog_expiredSince, expires)
							: NLS.bind(ProvUIMessages.TrustCertificateDialog_validExpires, expires);
				}, x509 -> {
					try {
						x509.checkValidity();
						return ProvUIMessages.TrustCertificateDialog_valid;
					} catch (CertificateExpiredException expired) {
						return ProvUIMessages.TrustCertificateDialog_expired;
					} catch (CertificateNotYetValidException notYetValid) {
						return ProvUIMessages.TrustCertificateDialog_notYetValid;
					}
				}, ProvUIMessages.TrustCertificateDialog_NotApplicable), tableColumnLayout, 10);

		createMenu(certificateViewer);

		addSelectionButtons(composite);

		certificateViewer.addDoubleClickListener(e -> {
			StructuredSelection selection = (StructuredSelection) e.getSelection();
			X509Certificate cert = getInstance(selection, X509Certificate.class);
			if (cert != null) {
				// create and open dialog for certificate chain
				CertificateLabelProvider.openDialog(getShell(), cert);
			}
		});

		certificateViewer.addSelectionChangedListener(e -> {
			if (certificateChainViewer != null) {
				TreeNode treeNode = getInstance(e.getSelection(), TreeNode.class);
				if (treeNode != null) {
					certificateChainViewer.setInput(new TreeNode[] { treeNode });
					certificateChainViewer.setSelection(new StructuredSelection(treeNode));
				} else {
					certificateChainViewer.setInput(new TreeNode[] {});
				}
			}

			updateOkButton();
		});

		certificateViewer.setInput(artifactMap.keySet().toArray(TreeNode[]::new));

		typeColumn.getColumn().pack();
	}

	private void createCertficateChainViewerArea(Composite composite) {
		certificateChainViewer = new TreeViewer(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData data = new GridData(GridData.FILL_BOTH);
		int treeSize = artifactMap.keySet().stream().map(TrustCertificateDialog::computeTreeSize)
				.max(Integer::compareTo).orElse(0);
		data.heightHint = convertHeightInCharsToPixels(Math.min(treeSize, 5));
		data.widthHint = convertWidthInCharsToPixels(120);
		certificateChainViewer.getTree().setLayoutData(data);

		certificateChainViewer.setAutoExpandLevel(3);
		certificateChainViewer.setContentProvider(new TreeNodeContentProvider());
		certificateChainViewer.setLabelProvider(new CertificateLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof TreeNode) {
					Object o = ((TreeNode) element).getValue();
					if (o instanceof PGPPublicKey) {
						PGPPublicKey key = (PGPPublicKey) o;
						String userFriendlyFingerPrint = userFriendlyFingerPrint(key);
						java.util.List<String> users = new ArrayList<>();
						key.getUserIDs().forEachRemaining(users::add);
						String userIDs = String.join(", ", users); //$NON-NLS-1$
						if (!userIDs.isEmpty()) {
							return userFriendlyFingerPrint + " [" + userIDs + "]"; //$NON-NLS-1$//$NON-NLS-2$
						}
						return userFriendlyFingerPrint;
					} else if (o == null) {
						return ProvUIMessages.TrustCertificateDialog_Unsigned;
					}
				}

				return super.getText(element);
			}
		});

		certificateChainViewer.addSelectionChangedListener(event -> {
			ISelection selection = event.getSelection();
			boolean containsCertificate = containsInstance(selection, X509Certificate.class);
			detailsButton.setEnabled(containsCertificate);
			exportButton.setEnabled(containsCertificate || containsInstance(selection, PGPPublicKey.class));
		});

		createMenu(certificateChainViewer);

		createButtons(composite);
	}

	private void crreateArtifactViewerArea(Composite composite, Set<IArtifactKey> artifacts) {
		TableColumnLayout tableColumnLayout = new TableColumnLayout(true);
		Composite tableComposite = WidgetFactory.composite(SWT.NONE)
				.layoutData(new GridData(SWT.FILL, SWT.FILL, true, true)).layout(tableColumnLayout).create(composite);
		Table table = WidgetFactory.table(SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION)
				.headerVisible(true).linesVisible(true).font(composite.getFont()).create(tableComposite);
		artifactViewer = new TableViewer(table);
		artifactViewer.setContentProvider(ArrayContentProvider.getInstance());

		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = convertHeightInCharsToPixels(Math.min(artifacts.size() + 1, 10)) * 3 / 2;
		data.widthHint = convertWidthInCharsToPixels(120);
		tableComposite.setLayoutData(data);

		Font font = table.getFont();
		FontData[] fontDatas = font.getFontData();
		for (FontData fontData : fontDatas) {
			fontData.setStyle(fontData.getStyle() | SWT.BOLD);
		}
		Font boldFont = new Font(table.getDisplay(), fontDatas);
		composite.addDisposeListener(e -> boldFont.dispose());

		Function<IArtifactKey, Font> fontProvider = e -> {
			for (Object object : certificateViewer.getCheckedElements()) {
				List<IArtifactKey> list = artifactMap.get(object);
				if (list != null && list.contains(e)) {
					return boldFont;
				}
			}
			return null;
		};

		certificateViewer.addCheckStateListener(e -> artifactViewer.refresh(true));

		artifactViewer.addPostSelectionChangedListener(e -> {
			if (table.isFocusControl()) {
				List<?> selection = e.getStructuredSelection().toList();
				List<TreeNode> newSelection = new ArrayList<>();
				LOOP: for (Map.Entry<TreeNode, List<IArtifactKey>> entry : artifactMap.entrySet()) {
					List<IArtifactKey> value = entry.getValue();
					if (value != null) {
						for (IArtifactKey key : value) {
							if (selection.contains(key)) {
								newSelection.add(entry.getKey());
								continue LOOP;
							}
						}
					}
				}

				certificateViewer.setSelection(new StructuredSelection(newSelection), true);
			}
		});

		certificateViewer.addPostSelectionChangedListener(e -> {
			if (!table.isFocusControl()) {
				Set<IArtifactKey> associatedArtifacts = new LinkedHashSet<>();
				for (Object object : e.getStructuredSelection()) {
					List<IArtifactKey> list = artifactMap.get(object);
					if (list != null) {
						associatedArtifacts.addAll(list);
					}
				}

				artifactViewer.setSelection(new StructuredSelection(associatedArtifacts.toArray()), true);

				// Reorder the artifacts so that the selected ones are first.
				LinkedHashSet<IArtifactKey> newInput = new LinkedHashSet<>(artifacts);
				newInput.retainAll(associatedArtifacts);
				newInput.addAll(artifacts);
				artifactViewer.setInput(newInput);

				artifactViewer.setSelection(new StructuredSelection(associatedArtifacts.toArray()), true);
			}
		});

		TableViewerColumn classifierColumn = createColumn(artifactViewer,
				ProvUIMessages.TrustCertificateDialog_Classifier,
				new ArtifactLabelProvider(a -> a.getClassifier(), fontProvider), tableColumnLayout, 1);
		createColumn(artifactViewer, ProvUIMessages.TrustCertificateDialog_ArtifactId,
				new ArtifactLabelProvider(a -> a.getId(), fontProvider), tableColumnLayout, 10);
		createColumn(artifactViewer, ProvUIMessages.TrustCertificateDialog_Version,
				new ArtifactLabelProvider(a -> a.getVersion().toString(), fontProvider), tableColumnLayout, 10);

		artifactViewer.setInput(artifacts);

		classifierColumn.getColumn().pack();
	}

	private void createMenu(StructuredViewer viewer) {
		Control control = viewer.getControl();
		Menu menu = new Menu(control);
		control.setMenu(menu);
		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(ProvUIMessages.TrustCertificateDialog_CopyFingerprint);
		item.addSelectionListener(widgetSelectedAdapter(e -> {
			PGPPublicKey key = getInstance(viewer.getSelection(), PGPPublicKey.class);
			if (key != null) {
				Clipboard clipboard = new Clipboard(getShell().getDisplay());
				clipboard.setContents(new Object[] { userFriendlyFingerPrint(key) },
						new Transfer[] { TextTransfer.getInstance() });
				clipboard.dispose();
			}
		}));
		viewer.addSelectionChangedListener(
				e -> item.setEnabled(containsInstance(e.getSelection(), PGPPublicKey.class)));
	}

	private TableViewerColumn createColumn(TableViewer tableViewer, String text, ColumnLabelProvider labelProvider,
			TableColumnLayout tableColumnLayout, int columnWeight) {
		TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
		column.getColumn().setText(text);
		column.setLabelProvider(labelProvider);
		tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(columnWeight));
		return column;
	}

	/**
	 * Visually checks the previously-specified elements in this dialog's list
	 * viewer.
	 */
	private void checkInitialSelections() {
		Iterator<?> itemsToCheck = getInitialElementSelections().iterator();
		while (itemsToCheck.hasNext()) {
			certificateViewer.setChecked(itemsToCheck.next(), true);
			if (artifactViewer != null) {
				artifactViewer.refresh(true);
			}
		}
	}

	/**
	 * Add the selection and deselection buttons to the dialog.
	 *
	 * @param composite org.eclipse.swt.widgets.Composite
	 */
	private void addSelectionButtons(Composite composite) {
		int horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);

		Composite buttonArea = new Composite(composite, SWT.NONE);
		GridLayout buttonAreaLayout = new GridLayout();
		buttonAreaLayout.numColumns = 2;
		buttonAreaLayout.marginWidth = 0;
		buttonAreaLayout.horizontalSpacing = horizontalSpacing;
		buttonArea.setLayout(buttonAreaLayout);
		buttonArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Composite leftButtonArea = new Composite(buttonArea, SWT.NONE);
		GridLayout leftButtonAreaLayout = new GridLayout();
		leftButtonAreaLayout.numColumns = 0;
		leftButtonAreaLayout.marginWidth = 0;
		leftButtonAreaLayout.horizontalSpacing = horizontalSpacing;
		leftButtonArea.setLayout(leftButtonAreaLayout);
		leftButtonArea.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));

		if (containsInstance(artifactMap.keySet(), PGPPublicKey.class)
				|| containsInstance(artifactMap.keySet(), Certificate.class)) {
			Button rememberSelectionButton = createCheckButton(leftButtonArea,
					ProvUIMessages.TrustCertificateDialog_RememberSigners);
			rememberSelectionButton.setSelection(rememberSelectedSigners);
			rememberSelectionButton.addSelectionListener(widgetSelectedAdapter(e -> {
				rememberSelectedSigners = rememberSelectionButton.getSelection();
			}));
		}

		Button trustAlwaysButton = createCheckButton(leftButtonArea, ProvUIMessages.TrustCertificateDialog_AlwaysTrust);
		trustAlwaysButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (trustAlwaysButton.getSelection()) {
				// Prompt the user to ensure they really understand what they've chosen, the
				// risk, and where the preference is stored if they wish to change it in the
				// future. Also ensure that the default button is no so that they must
				// explicitly click the yes button, not just hit enter.
				MessageDialog messageDialog = new MessageDialog(getShell(),
						ProvUIMessages.TrustCertificateDialog_AlwaysTrustConfirmationTitle, null,
						ProvUIMessages.TrustCertificateDialog_AlwaysTrustConfirmationMessage, MessageDialog.QUESTION,
						new String[] { ProvUIMessages.TrustCertificateDialog_AlwaysTrustYes,
								ProvUIMessages.TrustCertificateDialog_AlwaysTrustNo },
						1) {
					@Override
					public Image getImage() {
						return getWarningImage();
					}
				};
				int result = messageDialog.open();
				if (result != Window.OK) {
					// Restore the checkbox state.
					trustAlwaysButton.setSelection(false);
				} else {
					certificateViewer.setAllChecked(true);
					if (artifactViewer != null) {
						artifactViewer.refresh(true);
					}
					updateOkButton();
				}
			}
			trustAlways = trustAlwaysButton.getSelection();
		}));

		Composite rightButtonArea = new Composite(buttonArea, SWT.NONE);
		GridLayout rightButtonAreaLayout = new GridLayout();
		rightButtonAreaLayout.numColumns = 0;
		rightButtonAreaLayout.marginWidth = 0;
		rightButtonAreaLayout.horizontalSpacing = horizontalSpacing;
		rightButtonArea.setLayout(rightButtonAreaLayout);
		rightButtonArea.setLayoutData(new GridData(SWT.END, SWT.TOP, true, false));

		Button selectButton = createButton(rightButtonArea, IDialogConstants.SELECT_ALL_ID,
				ProvUIMessages.TrustCertificateDialog_SelectAll, false);
		selectButton.addSelectionListener(widgetSelectedAdapter(e -> {
			certificateViewer.setAllChecked(true);
			if (artifactViewer != null) {
				artifactViewer.refresh(true);
			}
			updateOkButton();
		}));

		Button deselectButton = createButton(rightButtonArea, IDialogConstants.DESELECT_ALL_ID,
				ProvUIMessages.TrustCertificateDialog_DeselectAll, false);
		deselectButton.addSelectionListener(widgetSelectedAdapter(e -> {
			certificateViewer.setAllChecked(false);
			if (artifactViewer != null) {
				artifactViewer.refresh(true);
			}
			updateOkButton();
		}));
	}

	protected Button createCheckButton(Composite parent, String label) {
		((GridLayout) parent.getLayout()).numColumns++;
		Button button = WidgetFactory.button(SWT.CHECK).text(label).font(JFaceResources.getDialogFont()).create(parent);
		setButtonLayoutData(button);
		return button;
	}

	private String getFilterPath(String key) {
		IDialogSettings dialogSettings = DialogSettings
				.getOrCreateSection(ProvUIActivator.getDefault().getDialogSettings(), getClass().getName());
		String filterPath = dialogSettings.get(key);
		if (filterPath == null) {
			filterPath = System.getProperty("user.home"); //$NON-NLS-1$
		}
		return filterPath;
	}

	private void setFilterPath(String key, String filterPath) {
		if (filterPath != null) {
			IDialogSettings dialogSettings = DialogSettings
					.getOrCreateSection(ProvUIActivator.getDefault().getDialogSettings(), getClass().getName());
			dialogSettings.put(key, filterPath);
		}
	}

	private void updateOkButton() {
		Button okButton = getOkButton();
		if (okButton != null) {
			certificateViewer.getCheckedElements();
			Object[] checkedElements = certificateViewer.getCheckedElements();
			Set<IArtifactKey> artifacts = artifactMap.values().stream().flatMap(Collection::stream)
					.collect(Collectors.toSet());
			if (artifacts.isEmpty()) {
				okButton.setEnabled(checkedElements.length > 0);
			} else {
				for (Object checkElement : checkedElements) {
					artifacts.removeAll(artifactMap.get(checkElement));
				}
				okButton.setEnabled(artifacts.isEmpty());
			}
		}
	}

	@Override
	protected void okPressed() {
		if (!revocationMap.isEmpty()) {
			// Prompt the user to ensure they really understand that they've chosen to
			// install content signed with a revoked PGP key.
			MessageDialog messageDialog = new MessageDialog(getShell(),
					ProvUIMessages.TrustCertificateDialogQuestionTrustRevokedKeyTitle, null,
					ProvUIMessages.TrustCertificateDialogQuestionTrustRevokedKeyQuestion, MessageDialog.QUESTION,
					new String[] { ProvUIMessages.TrustCertificateDialogQuestionTrustRevokedKeyAccept,
							ProvUIMessages.TrustCertificateDialogQuestionTrustRevokedKeyReject },
					1) {
				@Override
				public Image getImage() {
					return getWarningImage();
				}
			};
			if (messageDialog.open() != Window.OK) {
				return;
			}
		}

		setResult(Arrays.asList(certificateViewer.getCheckedElements()));
		super.okPressed();
	}

	private static class PGPOrX509ColumnLabelProvider extends ColumnLabelProvider {
		private Function<PGPPublicKey, String> pgpMap;
		private Function<X509Certificate, String> x509map;
		private String unsignedValue;

		public PGPOrX509ColumnLabelProvider(Function<PGPPublicKey, String> pgpMap,
				Function<X509Certificate, String> x509map, String unsignedValue) {
			this.pgpMap = pgpMap;
			this.x509map = x509map;
			this.unsignedValue = unsignedValue;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof TreeNode) {
				element = ((TreeNode) element).getValue();
			}
			if (element instanceof PGPPublicKey) {
				return pgpMap.apply((PGPPublicKey) element);
			}
			if (element instanceof X509Certificate) {
				return x509map.apply((X509Certificate) element);
			}

			if (element == null) {
				return unsignedValue;
			}
			return super.getText(element);
		}
	}

	private static class ArtifactLabelProvider extends ColumnLabelProvider {
		private Function<IArtifactKey, String> labelProvider;
		private Function<IArtifactKey, Font> fontProvider;

		public ArtifactLabelProvider(Function<IArtifactKey, String> labelProvider,
				Function<IArtifactKey, Font> fontProvider) {
			this.labelProvider = labelProvider;
			this.fontProvider = fontProvider;
		}

		@Override
		public String getText(Object element) {
			return labelProvider.apply((IArtifactKey) element);
		}

		@Override
		public Font getFont(Object element) {
			return fontProvider.apply((IArtifactKey) element);
		}
	}

	private static int computeTreeSize(TreeNode node) {
		int count = 1;
		TreeNode[] children = node.getChildren();
		if (children != null) {
			for (TreeNode child : children) {
				count += computeTreeSize(child);
			}
		}
		return count;
	}

	private static <T> T getInstance(Object element, Class<T> type, Predicate<T> filter) {
		if (type.isInstance(element)) {
			if (filter == null || filter.test(type.cast(element))) {
				return type.cast(element);
			}
		} else if (element instanceof Iterable) {
			for (Object object : ((Iterable<?>) element)) {
				T instance = getInstance(object, type, filter);
				if (instance != null) {
					return instance;
				}
			}
		} else if (element instanceof TreeNode) {
			if (element instanceof IAdaptable adaptable) {
				T instance = adaptable.getAdapter(type);
				if (instance != null) {
					return instance;
				}
			}
			return getInstance(((TreeNode) element).getValue(), type, filter);
		} else if (element instanceof TreeNode[]) {
			for (TreeNode child : (TreeNode[]) element) {
				T instance = getInstance(child, type, filter);
				if (instance != null) {
					return instance;
				}
			}
		}
		return null;
	}

	private static <T> T getInstance(Object element, Class<T> type) {
		return getInstance(element, type, null);
	}

	private static boolean containsInstance(Object element, Class<?> type) {
		return getInstance(element, type) != null;
	}

	private static String userFriendlyFingerPrint(PGPPublicKey key) {
		if (key == null) {
			return null;
		}
		return PGPPublicKeyService.toHexFingerprint(key);
	}
}
