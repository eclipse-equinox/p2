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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.cert.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.phases.AuthorityChecker;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataWriter;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.viewers.CertificateLabelProvider;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * A dialog that displays authority URIs and ask users if they trust content
 * from these sources.
 */
public class TrustAuthorityDialog extends SelectionDialog {
	private static final String EXPORT_FILTER_PATH = "exportFilterPath"; //$NON-NLS-1$

	private CheckboxTreeViewer authorityViewer;

	private TreeViewer certificateChainViewer;

	private TableViewer iuViewer;

	private boolean trustAlways;

	private boolean rememberSelectedAuthorities = true;

	private boolean allSecured = true;

	private final List<TreeNode> rootAuthorities = new ArrayList<>();

	private final List<TreeNode> sites = new ArrayList<>();

	private final Map<TreeNode, List<IInstallableUnit>> iuMap = new LinkedHashMap<>();

	private final Set<TreeNode> checkedAuthorities = new HashSet<>();

	public TrustAuthorityDialog(Shell parentShell, Object input) {
		super(parentShell);

		setShellStyle(SWT.DIALOG_TRIM | SWT.MODELESS | SWT.RESIZE | SWT.MAX | SWT.ON_TOP | getDefaultOrientation());

		if (input instanceof TreeNode[] nodes) {
			init(null, nodes);
		}

		setTitle(ProvUIMessages.TrustAuthorityDialog_TrustAuthoritiesTitle);

		var messages = new ArrayList<String>();
		messages.add(ProvUIMessages.TrustAuthorityDialog_TrustAuthorityMainMessage);
		if (!allSecured) {
			messages.add(ProvUIMessages.TrustAuthorityDialog_TrustInsecureAuthorityMessage);
		}
		messages.add(ProvUIMessages.TrustAuthorityDialog_TrustAuthorityDescriptionMessage);
		setMessage(String.join("  ", messages)); //$NON-NLS-1$

		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(parentShell,
					IProvHelpContextIds.TRUST_AUTHORITIES_DIALOG);
		}
	}

	private void init(TreeNode parent, TreeNode[] treeNodes) {
		for (var node : treeNodes) {
			var ius = getInstance(node, IInstallableUnit[].class);
			if (ius != null) {
				iuMap.put(node, Arrays.asList(ius));
			}

			if (parent == null) {
				rootAuthorities.add(node);
			}

			var certficates = getInstance(node, Certificate[].class);
			if (certficates == null || certficates.length == 0) {
				allSecured = false;
			}

			var children = node.getChildren();
			if (children.length > 0) {
				init(node, children);
			} else {
				sites.add(node);
			}
		}
	}

	public boolean isRememberSelectedAuthorities() {
		return rememberSelectedAuthorities;
	}

	public boolean isTrustAlways() {
		return trustAlways;
	}

	@Override
	protected Label createMessageArea(Composite composite) {
		// Ensure that the message supports wrapping for a long text message.
		var data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.widthHint = convertWidthInCharsToPixels(120);
		var factory = WidgetFactory.label(SWT.WRAP).font(composite.getFont()).layoutData(data);
		if (getMessage() != null) {
			factory.text(getMessage());
		}
		return factory.create(composite);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		var mainComposite = (Composite) super.createDialogArea(parent);
		Dialog.applyDialogFont(mainComposite);
		initializeDialogUnits(mainComposite);

		createMessageArea(mainComposite);

		var sashForm = new SashForm(mainComposite, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createAuthorityViewerArea(createSashFormArea(sashForm));

		// Create this area only if we have keys or certificates.
		var containsCertificates = containsInstance(iuMap.keySet(), PGPPublicKey.class)
				|| containsInstance(iuMap.keySet(), Certificate.class);
		containsCertificates = true;
		if (containsCertificates) {
			createCertficateChainViewerArea(createSashFormArea(sashForm));
		}

		// Sort the set of all IUs and create the lower area only if there are
		// IUs.
		var ius = iuMap.values().stream().flatMap(Collection::stream)
				.collect(Collectors.toCollection(() -> new TreeSet<>()));
		if (!ius.isEmpty()) {
			crreateIUViewerArea(createSashFormArea(sashForm), ius);
		}

		// Set weights based on the children's preferred size.
		var children = sashForm.getChildren();
		var weights = new int[children.length];
		for (var i = 0; i < children.length; ++i) {
			weights[i] = children[i].computeSize(SWT.DEFAULT, SWT.DEFAULT, false).y;
		}
		sashForm.setWeights(weights);

		if (!getInitialElementSelections().isEmpty()) {
			checkInitialSelections();
		}

		if (!iuMap.isEmpty()) {
			authorityViewer.setSelection(new StructuredSelection(iuMap.keySet().iterator().next()));
		}

		return mainComposite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, ProvUIMessages.TrustAuthorityDialog_TrustSelectedCheckbox, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		updateOkButton();
	}

	private Composite createSashFormArea(SashForm sashForm) {
		var composite = new Composite(sashForm, SWT.NONE);
		var layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		return composite;
	}

	private void setAllChecked(boolean checked) {
		checkedAuthorities.clear();
		if (checked) {
			for (TreeNode authority : rootAuthorities) {
				for (TreeNode child : authority.getChildren()) {
					checkedAuthorities.add(child);
					checkedAuthorities.addAll(getAllChildren(child));
				}
			}
		}
		authorityViewer.refresh(true);
	}

	private void createAuthorityViewerArea(Composite composite) {
		var treeColumnLayout = new TreeColumnLayout(true);
		var treeComposite = WidgetFactory.composite(SWT.NONE).layoutData(new GridData(SWT.FILL, SWT.FILL, true, true))
				.layout(treeColumnLayout).create(composite);
		var tree = WidgetFactory
				.tree(SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK)
				.headerVisible(true).linesVisible(true).font(composite.getFont()).create(treeComposite);
		authorityViewer = new CheckboxTreeViewer(tree);
		authorityViewer.setContentProvider(new TreeNodeContentProvider());

		tree.addKeyListener(KeyListener.keyPressedAdapter(e -> {
			if (e.stateMask == SWT.CTRL && e.keyCode == 'a') {
				authorityViewer.setSelection(new StructuredSelection(iuMap.keySet().toArray()));
				e.doit = false;
			}
		}));

		authorityViewer.addCheckStateListener(e -> {
			var node = (TreeNode) e.getElement();
			if (node.getChildren().length == 0) {
				node = node.getParent();
			}

			if (e.getChecked()) {
				checkedAuthorities.add(node);
				checkedAuthorities.addAll(getAllChildren(node));
			} else {
				checkedAuthorities.remove(node);
				checkedAuthorities.removeAll(getAllChildren(node));
			}

			authorityViewer.refresh(true);
			updateOkButton();
		});

		authorityViewer.setCheckStateProvider(new ICheckStateProvider() {
			@Override
			public boolean isGrayed(Object element) {
				var node = (TreeNode) element;
				return node.getParent() == null && !checkedAuthorities.contains(node)
						&& checkedAuthorities.containsAll(Arrays.asList(node.getChildren()));
			}

			@Override
			public boolean isChecked(Object element) {
				return checkedAuthorities.contains(element) || isGrayed(element);
			}
		});

		var data = new GridData(GridData.FILL_BOTH);
		data.heightHint = convertHeightInCharsToPixels(Math.min(iuMap.keySet().size() + 2, 6)) * 3 / 2;
		data.widthHint = convertWidthInCharsToPixels(120);
		treeComposite.setLayoutData(data);

		// This column is packed later.
		createColumn(authorityViewer, ProvUIMessages.TrustAuthorityDialog_AuthorityColumnTitle,
				new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						var treeNode = (TreeNode) element;
						var text = treeNode.getValue().toString();
						var parent = treeNode.getParent();
						if (parent != null) {
							var prefix = parent.getValue().toString();
							if (text.startsWith(prefix)) {
								return text.substring(prefix.length());
							}
						}
						return text;
					}
				}, treeColumnLayout, 1);

		var unitsColumn = createColumn(authorityViewer, ProvUIMessages.TrustAuthorityDialog_UnitsColumnTitle,
				new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						var ius = getInstance(element, IInstallableUnit[].class);
						return ius == null ? ProvUIMessages.TrustAuthorityDialog_ComputingAuthorityCertficate
								: Integer.toString(ius.length);
					}
				}, treeColumnLayout, 2);
		unitsColumn.getColumn().setAlignment(SWT.RIGHT);

		var securedColumn = createColumn(authorityViewer, ProvUIMessages.TrustAuthorityDialog_SecuredColumnTitle,
				new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						var certificates = getInstance(element, Certificate[].class);
						return certificates != null && certificates.length > 1
								? ProvUIMessages.TrustAuthorityDialog_AuthoritySecure
								: ProvUIMessages.TrustAuthorityDialog_AuthorityInsecure;
					}
				}, treeColumnLayout, 2);

		addSelectionButtons(composite);

		authorityViewer.addDoubleClickListener(e -> {
			var selection = (StructuredSelection) e.getSelection();
			var certificate = getInstance(selection, X509Certificate.class);
			if (certificate != null) {
				CertificateLabelProvider.openDialog(getShell(), certificate);
			}
		});

		authorityViewer.addSelectionChangedListener(e -> {
			if (certificateChainViewer != null) {
				var input = new ArrayList<TreeNode>();
				var treeNode = getInstance(e.getSelection(), TreeNode.class);
				if (treeNode != null) {
					var certificates = getInstance(treeNode, Certificate[].class);
					if (certificates != null && certificates.length > 0) {
						TreeNode root = null;
						for (var i = certificates.length; --i >= 0;) {
							var child = root;
							root = new TreeNode(certificates[i]);
							if (child != null) {
								root.setChildren(new TreeNode[] { child });
							}
						}
						input.add(root);
					}
				}
				certificateChainViewer.setInput(input.toArray(TreeNode[]::new));
				if (!input.isEmpty()) {
					certificateChainViewer.setSelection(new StructuredSelection(input.get(0)));
				}
			}
		});

		authorityViewer.setInput(rootAuthorities.toArray(TreeNode[]::new));

		tree.setVisible(false);
		authorityViewer.expandAll();
		// typeColumn.getColumn().pack();
		unitsColumn.getColumn().pack();
		securedColumn.getColumn().pack();
		authorityViewer.collapseAll();
		tree.setVisible(true);

		createMenu(authorityViewer);
	}

	private void createCertficateChainViewerArea(Composite composite) {
		certificateChainViewer = new TreeViewer(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		var data = new GridData(GridData.FILL_BOTH);
		data.heightHint = convertHeightInCharsToPixels(2);
		data.widthHint = convertWidthInCharsToPixels(120);
		certificateChainViewer.getTree().setLayoutData(data);

		certificateChainViewer.setAutoExpandLevel(2);
		certificateChainViewer.setContentProvider(new TreeNodeContentProvider());
		certificateChainViewer.setLabelProvider(new CertificateLabelProvider());

		certificateChainViewer.addDoubleClickListener(e -> {
			var selection = (StructuredSelection) e.getSelection();
			var certificate = getInstance(selection, X509Certificate.class);
			if (certificate != null) {
				CertificateLabelProvider.openDialog(getShell(), certificate);
			}
		});

		var buttonComposite = new Composite(composite, SWT.NONE);
		buttonComposite.setLayout(new RowLayout());

		var detailsButton = new Button(buttonComposite, SWT.NONE);
		detailsButton.setEnabled(false);
		detailsButton.setText(ProvUIMessages.TrustAuthorityDialog_CertificateDetailsButton);
		detailsButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				var certificate = getInstance(certificateChainViewer.getSelection(), X509Certificate.class);
				if (certificate != null) {
					CertificateLabelProvider.openDialog(getShell(), certificate);
				}
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				widgetDefaultSelected(e);
			}
		});

		var exportButton = new Button(buttonComposite, SWT.NONE);
		exportButton.setEnabled(false);
		exportButton.setText(ProvUIMessages.TrustAuthorityDialog_CertificateExportButton);
		exportButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				var selection = certificateChainViewer.getSelection();
				var certificate = getInstance(selection, X509Certificate.class);
				var key = getInstance(selection, PGPPublicKey.class);
				if (certificate != null || key != null) {
					var destination = new FileDialog(exportButton.getShell(), SWT.SAVE);
					destination.setFilterPath(getFilterPath(EXPORT_FILTER_PATH));
					destination.setText(ProvUIMessages.TrustAuthorityDialog_ExportDialogTitle);
					if (certificate != null) {
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

		certificateChainViewer.addSelectionChangedListener(event -> {
			var selection = event.getSelection();
			var containsCertificate = containsInstance(selection, X509Certificate.class);
			detailsButton.setEnabled(containsCertificate);
			exportButton.setEnabled(containsCertificate || containsInstance(selection, PGPPublicKey.class));
		});
	}

	private void crreateIUViewerArea(Composite composite, Set<IInstallableUnit> ius) {
		var tableColumnLayout = new TableColumnLayout(true);
		var tableComposite = WidgetFactory.composite(SWT.NONE).layoutData(new GridData(SWT.FILL, SWT.FILL, true, true))
				.layout(tableColumnLayout).create(composite);
		var table = WidgetFactory.table(SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION)
				.headerVisible(true).linesVisible(true).font(composite.getFont()).create(tableComposite);
		iuViewer = new TableViewer(table);
		iuViewer.setContentProvider(ArrayContentProvider.getInstance());

		table.addKeyListener(KeyListener.keyPressedAdapter(e -> {
			if (e.stateMask == SWT.CTRL && e.keyCode == 'a') {
				iuViewer.setSelection(new StructuredSelection(ius.toArray()));
				e.doit = false;
			}
		}));

		var data = new GridData(GridData.FILL_BOTH);
		data.heightHint = convertHeightInCharsToPixels(Math.min(ius.size() + 1, 10)) * 3 / 2;
		data.widthHint = convertWidthInCharsToPixels(120);
		tableComposite.setLayoutData(data);

		var font = table.getFont();
		var fontDatas = font.getFontData();
		for (var fontData : fontDatas) {
			fontData.setStyle(fontData.getStyle() | SWT.BOLD);
		}
		var boldFont = new Font(table.getDisplay(), fontDatas);
		composite.addDisposeListener(e -> boldFont.dispose());

		var buttonComposite = new Composite(composite, SWT.NONE);
		buttonComposite.setLayout(new RowLayout());

		var detailsButton = new Button(buttonComposite, SWT.NONE);
		detailsButton.setEnabled(false);
		detailsButton.setText(ProvUIMessages.TrustAuthorityDialog_IUDetailsButton);

		Runnable openIUDialog = () -> {
			@SuppressWarnings("unchecked")
			List<IInstallableUnit> list = iuViewer.getStructuredSelection().toList();
			var iuDialog = new IUDialog(getShell(), list); // $NON-NLS-2$
			iuDialog.open();
		};
		detailsButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				openIUDialog.run();
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				widgetDefaultSelected(e);
			}
		});

		Function<IInstallableUnit, Font> fontProvider = e -> {
			for (var object : authorityViewer.getCheckedElements()) {
				var list = iuMap.get(object);
				if (list != null && list.contains(e)) {
					return boldFont;
				}
			}
			return null;
		};

		authorityViewer.addCheckStateListener(e -> iuViewer.refresh(true));

		iuViewer.addPostSelectionChangedListener(e -> {
			var selection = e.getStructuredSelection().toList();
			detailsButton.setEnabled(!selection.isEmpty());
			if (table.isFocusControl()) {
				var newSelection = new ArrayList<TreeNode>();
				LOOP: for (var entry : iuMap.entrySet()) {
					var value = entry.getValue();
					if (value != null) {
						for (var key : value) {
							if (selection.contains(key)) {
								newSelection.add(entry.getKey());
								continue LOOP;
							}
						}
					}
				}
				authorityViewer.setSelection(new StructuredSelection(newSelection), true);
			}
		});

		iuViewer.addDoubleClickListener(e -> {
			openIUDialog.run();
		});

		authorityViewer.addPostSelectionChangedListener(e -> {
			if (!table.isFocusControl()) {
				var associatedIUs = new LinkedHashSet<IInstallableUnit>();
				for (var object : e.getStructuredSelection()) {
					var list = iuMap.get(object);
					if (list != null) {
						associatedIUs.addAll(list);
					}
				}

				iuViewer.setSelection(new StructuredSelection(associatedIUs.toArray()), true);

				// Reorder the IUs so that the selected ones are first.
				var newInput = new LinkedHashSet<>(ius);
				newInput.retainAll(associatedIUs);
				newInput.addAll(ius);
				iuViewer.setInput(newInput);

				iuViewer.setSelection(new StructuredSelection(associatedIUs.toArray()), true);
			}
		});

		var idColumn = createColumn(iuViewer, ProvUIMessages.TrustAuthorityDialog_IUColumnTitle,
				new IULabelProvider(a -> a.getId(), fontProvider), tableColumnLayout, 1);
		createColumn(iuViewer, ProvUIMessages.TrustAuthorityDialog_IUVersionColumnTitle,
				new IULabelProvider(a -> a.getVersion().toString(), fontProvider), tableColumnLayout, 10);

		iuViewer.setInput(ius);

		idColumn.getColumn().pack();
	}

	private void createMenu(StructuredViewer viewer) {
		var control = viewer.getControl();
		var menu = new Menu(control);
		control.setMenu(menu);
		var item = new MenuItem(menu, SWT.PUSH);
		item.setText(ProvUIMessages.TrustAuthorityDialog_AuthorityCopyLinkMenu);
		item.addSelectionListener(widgetSelectedAdapter(e -> {
			var uri = getInstance(viewer.getSelection(), URI.class);
			if (uri != null) {
				var clipboard = new Clipboard(getShell().getDisplay());
				clipboard.setContents(new Object[] { uri.toString() }, new Transfer[] { TextTransfer.getInstance() });
				clipboard.dispose();
			}
		}));
		viewer.addSelectionChangedListener(e -> item.setEnabled(containsInstance(e.getSelection(), URI.class)));
	}

	private TreeViewerColumn createColumn(TreeViewer treeViewer, String text, ColumnLabelProvider labelProvider,
			TreeColumnLayout treeColumnLayout, int columnWeight) {
		var column = new TreeViewerColumn(treeViewer, SWT.NONE);
		column.getColumn().setText(text);
		column.setLabelProvider(labelProvider);
		treeColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(columnWeight));
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

	/**
	 * Visually checks the previously-specified elements in this dialog's list
	 * viewer.
	 */
	private void checkInitialSelections() {
		for (var element : getInitialElementSelections()) {
			authorityViewer.setChecked(element, true);
			if (iuViewer != null) {
				iuViewer.refresh(true);
			}
		}
	}

	/**
	 * Add the selection and deselection buttons to the dialog.
	 *
	 * @param composite org.eclipse.swt.widgets.Composite
	 */
	private void addSelectionButtons(Composite composite) {
		var horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);

		var buttonArea = new Composite(composite, SWT.NONE);
		var buttonAreaLayout = new GridLayout();
		buttonAreaLayout.numColumns = 2;
		buttonAreaLayout.marginWidth = 0;
		buttonAreaLayout.horizontalSpacing = horizontalSpacing;
		buttonArea.setLayout(buttonAreaLayout);
		buttonArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		var leftButtonArea = new Composite(buttonArea, SWT.NONE);
		var leftButtonAreaLayout = new GridLayout();
		leftButtonAreaLayout.numColumns = 0;
		leftButtonAreaLayout.marginWidth = 0;
		leftButtonAreaLayout.horizontalSpacing = horizontalSpacing;
		leftButtonArea.setLayout(leftButtonAreaLayout);
		leftButtonArea.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));

		var rememberSelectionButton = createCheckButton(leftButtonArea,
				ProvUIMessages.TrustAuthorityDialog_RememberSelectedAuthoritiesCheckbox);
		rememberSelectionButton.setSelection(rememberSelectedAuthorities);
		rememberSelectionButton.addSelectionListener(widgetSelectedAdapter(e -> {
			rememberSelectedAuthorities = rememberSelectionButton.getSelection();
		}));

		var trustAlwaysButton = createCheckButton(leftButtonArea,
				ProvUIMessages.TrustAuthorityDialog_TrustAllAuthoritiesCheckbox);
		trustAlwaysButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (trustAlwaysButton.getSelection()) {
				// Prompt the user to ensure they really understand what they've chosen, the
				// risk, and where the preference is stored if they wish to change it in the
				// future. Also ensure that the default button is no so that they must
				// explicitly click the yes button, not just hit enter.
				var messageDialog = new MessageDialog(getShell(),
						ProvUIMessages.TrustAuthorityDialog_TrustAllAuthoritiesConfirmationTitle, null,
						ProvUIMessages.TrustAuthorityDialog_TrustAllAuthoritiesConfirmationDescription,
						MessageDialog.QUESTION,
						new String[] { ProvUIMessages.TrustAuthorityDialog_AcceptTrustAllAuthorities,
								ProvUIMessages.TrustAuthorityDialog_RejectTrustAllAuthorities },
						1) {
					@Override
					public Image getImage() {
						return getWarningImage();
					}
				};
				var result = messageDialog.open();
				if (result != Window.OK) {
					// Restore the checkbox state.
					trustAlwaysButton.setSelection(false);
				} else {
					setAllChecked(true);
					if (iuViewer != null) {
						iuViewer.refresh(true);
					}
					updateOkButton();
				}
			}
			trustAlways = trustAlwaysButton.getSelection();
		}));

		var rightButtonArea = new Composite(buttonArea, SWT.NONE);
		var rightButtonAreaLayout = new GridLayout();
		rightButtonAreaLayout.numColumns = 0;
		rightButtonAreaLayout.marginWidth = 0;
		rightButtonAreaLayout.horizontalSpacing = horizontalSpacing;
		rightButtonArea.setLayout(rightButtonAreaLayout);
		rightButtonArea.setLayoutData(new GridData(SWT.END, SWT.TOP, true, false));

		var selectButton = createButton(rightButtonArea, IDialogConstants.SELECT_ALL_ID,
				ProvUIMessages.TrustAuthorityDialog_AuthoritiesSelectAllButton, false);
		selectButton.addSelectionListener(widgetSelectedAdapter(e -> {
			setAllChecked(true);
			if (iuViewer != null) {
				iuViewer.refresh(true);
			}
			updateOkButton();
		}));

		var deselectButton = createButton(rightButtonArea, IDialogConstants.DESELECT_ALL_ID,
				ProvUIMessages.TrustAuthorityDialog_AuthoritiesDeselectAllButton, false);
		deselectButton.addSelectionListener(widgetSelectedAdapter(e -> {
			setAllChecked(false);
			if (iuViewer != null) {
				iuViewer.refresh(true);
			}
			updateOkButton();
		}));

		var expandAllButton = createButton(rightButtonArea, IDialogConstants.CLIENT_ID,
				ProvUIMessages.TrustAuthorityDialog_AuthoritiesExpandAllButton, false);
		expandAllButton.addSelectionListener(widgetSelectedAdapter(e -> {
			authorityViewer.expandAll();
		}));

		var collapseAllButton = createButton(rightButtonArea, IDialogConstants.CLIENT_ID + 1,
				ProvUIMessages.TrustAuthorityDialog_AuthoritiesCollapseAllButton, false);
		collapseAllButton.addSelectionListener(widgetSelectedAdapter(e -> {
			authorityViewer.collapseAll();
		}));
	}

	protected Button createCheckButton(Composite parent, String label) {
		((GridLayout) parent.getLayout()).numColumns++;
		var button = WidgetFactory.button(SWT.CHECK).text(label).font(JFaceResources.getDialogFont()).create(parent);
		setButtonLayoutData(button);
		return button;
	}

	private String getFilterPath(String key) {
		var dialogSettings = DialogSettings.getOrCreateSection(ProvUIActivator.getDefault().getDialogSettings(),
				getClass().getName());
		var filterPath = dialogSettings.get(key);
		if (filterPath == null) {
			filterPath = System.getProperty("user.home"); //$NON-NLS-1$
		}
		return filterPath;
	}

	private void setFilterPath(String key, String filterPath) {
		if (filterPath != null) {
			var dialogSettings = DialogSettings.getOrCreateSection(ProvUIActivator.getDefault().getDialogSettings(),
					getClass().getName());
			dialogSettings.put(key, filterPath);
		}
	}

	private void updateOkButton() {
		var okButton = getOkButton();
		if (okButton != null) {
			var siteURIs = sites.stream().map(node -> (URI) node.getValue())
					.collect(Collectors.toCollection(() -> new HashSet<>()));
			var checkedAuthorityURIs = checkedAuthorities.stream().map(node -> (URI) node.getValue())
					.collect(Collectors.toSet());
			siteURIs.removeIf(uri -> {
				for (var authority : AuthorityChecker.getAuthorityChain(uri)) {
					if (checkedAuthorityURIs.contains(authority)) {
						return true;
					}
				}
				return false;
			});

			okButton.setEnabled(siteURIs.isEmpty());
		}
	}

	@Override
	protected void okPressed() {
		setResult(new ArrayList<>(checkedAuthorities));
		super.okPressed();
	}

	@Override
	public URI[] getResult() {
		return Arrays.stream(super.getResult()).filter(
				node -> rootAuthorities.contains(node) || rootAuthorities.contains(((TreeNode) node).getParent()))
				.map(it -> (URI) (((TreeNode) it).getValue())).toArray(URI[]::new);
	}

	private static class IULabelProvider extends ColumnLabelProvider {
		private Function<IInstallableUnit, String> labelProvider;
		private Function<IInstallableUnit, Font> fontProvider;

		public IULabelProvider(Function<IInstallableUnit, String> labelProvider,
				Function<IInstallableUnit, Font> fontProvider) {
			this.labelProvider = labelProvider;
			this.fontProvider = fontProvider;
		}

		@Override
		public String getText(Object element) {
			return labelProvider.apply((IInstallableUnit) element);
		}

		@Override
		public Font getFont(Object element) {
			return fontProvider.apply((IInstallableUnit) element);
		}
	}

	private static List<TreeNode> getAllChildren(TreeNode treeNode) {
		var result = new ArrayList<TreeNode>();
		getAllChildren(result, treeNode);
		return result;
	}

	private static void getAllChildren(List<TreeNode> result, TreeNode treeNode) {
		for (TreeNode child : treeNode.getChildren()) {
			result.add(child);
			getAllChildren(result, child);
		}
	}

	private static <T> T getInstance(Object element, Class<T> type, Predicate<T> filter) {
		if (type.isInstance(element)) {
			if (filter == null || filter.test(type.cast(element))) {
				return type.cast(element);
			}
		} else if (element instanceof Iterable<?> elements) {
			for (var object : elements) {
				T instance = getInstance(object, type, filter);
				if (instance != null) {
					return instance;
				}
			}
		} else if (element instanceof TreeNode treeNode) {
			if (element instanceof IAdaptable adaptable) {
				var instance = adaptable.getAdapter(type);
				if (instance != null) {
					return instance;
				}

				var array = (Object[]) adaptable.getAdapter(type.arrayType());
				if (array != null && array.length > 0) {
					return type.cast(array[0]);
				}
			}
			return getInstance(treeNode.getValue(), type, filter);
		} else if (element instanceof TreeNode[] treeNodes) {
			for (var child : treeNodes) {
				var instance = getInstance(child, type, filter);
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

	private static final class IUDialog extends TitleAreaDialog {
		private static final Pattern INSTRUCTION_PATTERN = Pattern.compile("<instruction .*?</instruction>", //$NON-NLS-1$
				Pattern.MULTILINE | Pattern.DOTALL);

		private final String xml;

		private final Collection<IInstallableUnit> ius;

		public IUDialog(Shell parentShell, Collection<IInstallableUnit> ius) {
			super(parentShell);
			this.ius = ius;
			setShellStyle(SWT.TITLE | SWT.MAX | SWT.RESIZE | SWT.BORDER | SWT.APPLICATION_MODAL);
			this.xml = getIUDetails(ius);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(ProvUIMessages.TrustAuthorityDialog_IUDetailsDialogTitle);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			var layout = (GridLayout) parent.getLayout();
			layout.marginHeight = 5;
			layout.marginWidth = 5;
			var text = new StyledText(parent, SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
			text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			text.setLayoutData(new GridData(GridData.FILL_BOTH));
			text.setText(xml);
			text.setEditable(false);

			var styleRanges = new ArrayList<StyleRange>();
			for (var matcher = INSTRUCTION_PATTERN.matcher(xml); matcher.find();) {
				var styleRange = new StyleRange();
				styleRange.start = matcher.start();
				styleRange.length = matcher.end() - styleRange.start;
				styleRange.fontStyle = SWT.BOLD;
				styleRanges.add(styleRange);
			}

			text.setStyleRanges(styleRanges.toArray(StyleRange[]::new));

			var size = ius.size();
			if (size == 1) {
				var iu = ius.iterator().next();
				setTitle(iu.getId() + " - " + iu.getVersion()); //$NON-NLS-1$
			} else {
				setTitle(NLS.bind(ProvUIMessages.TrustAuthorityDialog_IUDetailsDialogCountMessage, size));
			}

			setMessage(ProvUIMessages.TrustAuthorityDialog_IUDetailDialogDescriptionMessage);

			return text;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
		}

		private static String getIUDetails(Collection<IInstallableUnit> ius) {
			var out = new ByteArrayOutputStream();
			new MetadataWriter(out, null) {
				@Override
				protected void writeInstallableUnit(IInstallableUnit resolvedIU) {
					var iu = resolvedIU.unresolved();
					start(INSTALLABLE_UNIT_ELEMENT);
					attribute(ID_ATTRIBUTE, iu.getId());
					attribute(VERSION_ATTRIBUTE, iu.getVersion());
					attribute(SINGLETON_ATTRIBUTE, iu.isSingleton(), true);

					writeMetaRequirements(iu.getMetaRequirements());
					writeArtifactKeys(iu.getArtifacts());
					if (iu.getTouchpointType() != null) {
						writeTouchpointType(iu.getTouchpointType());
					}
					writeTouchpointData(iu.getTouchpointData());
					end(INSTALLABLE_UNIT_ELEMENT);
					flush();
				}
			}.writeInstallableUnits(ius.iterator(), ius.size());
			return new String(out.toByteArray(), StandardCharsets.UTF_8);
		}
	}
}
