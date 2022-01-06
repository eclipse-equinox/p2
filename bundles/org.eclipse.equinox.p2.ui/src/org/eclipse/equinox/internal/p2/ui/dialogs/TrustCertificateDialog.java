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
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Function;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.viewers.CertificateLabelProvider;
import org.eclipse.equinox.internal.provisional.security.ui.X500PrincipalHelper;
import org.eclipse.equinox.internal.provisional.security.ui.X509CertificateViewDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * A dialog that displays a certificate chain and asks the user if they trust
 * the certificate providers.
 */
public class TrustCertificateDialog extends SelectionDialog {

	private Object inputElement;
	private IStructuredContentProvider contentProvider;

	private static final int SIZING_SELECTION_WIDGET_HEIGHT = 250;
	private static final int SIZING_SELECTION_WIDGET_WIDTH = 300;

	CheckboxTableViewer listViewer;

	private TreeViewer certificateChainViewer;
	protected TreeNode parentElement;
	protected Object selectedCertificate;
	private Button detailsButton;

	public TrustCertificateDialog(Shell parentShell, Object input) {
		super(parentShell);
		inputElement = input;
		this.contentProvider = new TreeNodeContentProvider();
		setTitle(ProvUIMessages.TrustCertificateDialog_Title);
		setMessage(containsPGPKeys(inputElement) ? ProvUIMessages.TrustCertificateDialog_MessageWithPGP
				: ProvUIMessages.TrustCertificateDialog_Message);
		setShellStyle(SWT.DIALOG_TRIM | SWT.MODELESS | SWT.RESIZE | getDefaultOrientation());
	}

	private static boolean containsPGPKeys(Object inputElement) {
		if (inputElement instanceof PGPPublicKey) {
			return true;
		} else if (inputElement instanceof PGPPublicKey[]) {
			return ((PGPPublicKey[]) inputElement).length > 0;
		} else if (inputElement instanceof Iterable) {
			Iterator<?> iterator = ((Iterable<?>) inputElement).iterator();
			while (iterator.hasNext()) {
				if (containsPGPKeys(iterator.next())) {
					return true;
				}
			}
		} else if (inputElement instanceof TreeNode) {
			return containsPGPKeys(((TreeNode) inputElement).getValue());
		} else if (inputElement instanceof TreeNode[]) {
			for (TreeNode child : (TreeNode[]) inputElement) {
				if (containsPGPKeys(child)) {
					return true;
				}
			}
		}
		return false;
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
			if (element instanceof TreeNode) {
				element = ((TreeNode) element).getValue();
			}
			if (element instanceof PGPPublicKey) {
				return pgpMap.apply((PGPPublicKey) element);
			}
			if (element instanceof X509Certificate) {
				return x509map.apply((X509Certificate) element);
			}
			return super.getText(element);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = createUpperDialogArea(parent);
		certificateChainViewer = new TreeViewer(composite, SWT.BORDER);
		GridLayout layout = new GridLayout();
		certificateChainViewer.getTree().setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		certificateChainViewer.getTree().setLayoutData(data);
		certificateChainViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		certificateChainViewer.setContentProvider(new TreeNodeContentProvider());
		certificateChainViewer.setLabelProvider(new CertificateLabelProvider());
		certificateChainViewer.addSelectionChangedListener(getChainSelectionListener());
		if (inputElement instanceof Object[]) {
			ISelection selection = null;
			Object[] nodes = (Object[]) inputElement;
			if (nodes.length > 0) {
				selection = new StructuredSelection(nodes[0]);
				certificateChainViewer.setInput(new TreeNode[] { (TreeNode) nodes[0] });
				selectedCertificate = nodes[0];
			}
			listViewer.setSelection(selection);
		}
		listViewer.addDoubleClickListener(getDoubleClickListener());
		listViewer.addSelectionChangedListener(getParentSelectionListener());
		createButtons(composite);
		detailsButton.setEnabled(selectedCertificate instanceof X509Certificate);
		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, ProvUIMessages.TrustCertificateDialog_AcceptSelectedButtonLabel,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		super.getOkButton().setEnabled(false);
	}

	private void createButtons(Composite composite) {
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		buttonComposite.setLayout(new RowLayout());
		// Details button to view certificate chain
		detailsButton = new Button(buttonComposite, SWT.NONE);
		detailsButton.setText(ProvUIMessages.TrustCertificateDialog_Details);
		detailsButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				Object o = selectedCertificate;
				if (selectedCertificate instanceof TreeNode) {
					o = ((TreeNode) selectedCertificate).getValue();
				}
				if (o instanceof X509Certificate) {
					X509Certificate cert = (X509Certificate) o;
					X509CertificateViewDialog certificateDialog = new X509CertificateViewDialog(getShell(), cert);
					certificateDialog.open();
				}
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				widgetDefaultSelected(e);
			}
		});

		Button exportButton = new Button(buttonComposite, SWT.NONE);
		exportButton.setText(ProvUIMessages.TrustCertificateDialog_Export);
		exportButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				Object o = selectedCertificate;
				if (selectedCertificate instanceof TreeNode) {
					o = ((TreeNode) selectedCertificate).getValue();
				}
				FileDialog destination = new FileDialog(detailsButton.getShell(), SWT.SAVE);
				destination.setText(ProvUIMessages.TrustCertificateDialog_Export);
				if (o instanceof X509Certificate) {
					X509Certificate cert = (X509Certificate) o;
					destination.setFilterExtensions(new String[] { "*.der" }); //$NON-NLS-1$
					destination.setFileName(cert.getSerialNumber().toString() + ".der"); //$NON-NLS-1$
					String path = destination.open();
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
				} else if (o instanceof PGPPublicKey) {
					PGPPublicKey key = (PGPPublicKey) o;
					destination.setFilterExtensions(new String[] { "*.asc" }); //$NON-NLS-1$
					destination.setFileName(userFriendlyFingerPrint(key).replace(" ", "") + ".asc"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					String path = destination.open();
					if (path == null) {
						return;
					}
					File destinationFile = new File(path);
					try (OutputStream output = new ArmoredOutputStream(new FileOutputStream(destinationFile))) {
						output.write(key.getEncoded());
					} catch (IOException ex) {
						ProvUIActivator.getDefault().getLog()
								.log(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ex.getMessage(), ex));
					}
				}
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				widgetDefaultSelected(e);
			}

		});
	}

	private Composite createUpperDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		initializeDialogUnits(composite);
		createMessageArea(composite);

		listViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = SIZING_SELECTION_WIDGET_HEIGHT;
		data.widthHint = SIZING_SELECTION_WIDGET_WIDTH;
		listViewer.getTable().setLayoutData(data);

		listViewer.setContentProvider(contentProvider);
		TableViewerColumn typeColumn = new TableViewerColumn(listViewer, SWT.NONE);
		typeColumn.getColumn().setWidth(80);
		typeColumn.getColumn().setText(ProvUIMessages.TrustCertificateDialog_ObjectType);
		typeColumn.setLabelProvider(new PGPOrX509ColumnLabelProvider(key -> "PGP", cert -> "x509")); //$NON-NLS-1$ //$NON-NLS-2$
		TableViewerColumn idColumn = new TableViewerColumn(listViewer, SWT.NONE);
		idColumn.getColumn().setWidth(200);
		idColumn.getColumn().setText(ProvUIMessages.TrustCertificateDialog_Id);
		idColumn.setLabelProvider(new PGPOrX509ColumnLabelProvider(TrustCertificateDialog::userFriendlyFingerPrint,
				cert -> cert.getSerialNumber().toString()));
		TableViewerColumn signerColumn = new TableViewerColumn(listViewer, SWT.NONE);
		signerColumn.getColumn().setText(ProvUIMessages.TrustCertificateDialog_Name);
		signerColumn.getColumn().setWidth(400);
		signerColumn.setLabelProvider(new PGPOrX509ColumnLabelProvider(pgp -> {
			java.util.List<String> users = new ArrayList<>();
			pgp.getUserIDs().forEachRemaining(users::add);
			return String.join(",", users); //$NON-NLS-1$
		}, x509 -> {
			X500PrincipalHelper principalHelper = new X500PrincipalHelper(x509.getSubjectX500Principal());
			return principalHelper.getCN() + "; " + principalHelper.getOU() + "; " //$NON-NLS-1$ //$NON-NLS-2$
					+ principalHelper.getO();
		}));
		listViewer.getTable().setHeaderVisible(true);

		Menu menu = new Menu(listViewer.getTable());
		listViewer.getTable().setMenu(menu);
		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(ProvUIMessages.TrustCertificateDialog_CopyFingerprint);
		item.addSelectionListener(widgetSelectedAdapter(e -> {
			Object o = ((IStructuredSelection) listViewer.getSelection()).getFirstElement();
			if (o instanceof TreeNode) {
				o = ((TreeNode) o).getValue();
			}
			if (o instanceof PGPPublicKey) {
				PGPPublicKey key = (PGPPublicKey) o;
				Clipboard clipboard = new Clipboard(getShell().getDisplay());
				clipboard.setContents(new Object[] { userFriendlyFingerPrint(key) },
						new Transfer[] { TextTransfer.getInstance() });
				clipboard.dispose();
			}
		}));
		listViewer.addSelectionChangedListener(e -> item.setEnabled(containsPGPKeys(e.getSelection())));

		addSelectionButtons(composite);

		listViewer.setInput(inputElement);

		if (!getInitialElementSelections().isEmpty()) {
			checkInitialSelections();
		}

		Dialog.applyDialogFont(composite);

		return composite;
	}

	/**
	 * Visually checks the previously-specified elements in this dialog's list
	 * viewer.
	 */
	private void checkInitialSelections() {
		Iterator<?> itemsToCheck = getInitialElementSelections().iterator();
		while (itemsToCheck.hasNext()) {
			listViewer.setChecked(itemsToCheck.next(), true);
		}
	}

	/**
	 * Add the selection and deselection buttons to the dialog.
	 *
	 * @param composite org.eclipse.swt.widgets.Composite
	 */
	private void addSelectionButtons(Composite composite) {
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(new GridData(SWT.END, SWT.TOP, true, false));

		Button selectButton = createButton(buttonComposite, IDialogConstants.SELECT_ALL_ID,
				ProvUIMessages.TrustCertificateDialog_SelectAll, false);

		SelectionListener listener = widgetSelectedAdapter(e -> {
			listViewer.setAllChecked(true);
			getOkButton().setEnabled(true);
		});
		selectButton.addSelectionListener(listener);

		Button deselectButton = createButton(buttonComposite, IDialogConstants.DESELECT_ALL_ID,
				ProvUIMessages.TrustCertificateDialog_DeselectAll, false);

		listener = widgetSelectedAdapter(e -> {
			listViewer.setAllChecked(false);
			getOkButton().setEnabled(false);
		});
		deselectButton.addSelectionListener(listener);
	}

	private ISelectionChangedListener getChainSelectionListener() {
		return event -> {
			ISelection selection = event.getSelection();
			if (selection instanceof StructuredSelection) {
				selectedCertificate = ((StructuredSelection) selection).getFirstElement();
				detailsButton.setEnabled(selectedCertificate instanceof X509Certificate);
			}
		};
	}

	public TreeViewer getCertificateChainViewer() {
		return certificateChainViewer;
	}

	private IDoubleClickListener getDoubleClickListener() {
		return event -> {
			StructuredSelection selection = (StructuredSelection) event.getSelection();
			Object selectedElement = selection.getFirstElement();
			if (selectedElement instanceof TreeNode) {
				TreeNode treeNode = (TreeNode) selectedElement;
				if (treeNode.getValue() instanceof X509Certificate) {
					// create and open dialog for certificate chain
					X509CertificateViewDialog certificateViewDialog = new X509CertificateViewDialog(getShell(),
							(X509Certificate) treeNode.getValue());
					certificateViewDialog.open();
				}
			}
		};
	}

	private ISelectionChangedListener getParentSelectionListener() {
		return event -> {
			ISelection selection = event.getSelection();
			if (selection instanceof StructuredSelection) {
				TreeNode firstElement = (TreeNode) ((StructuredSelection) selection).getFirstElement();
				getCertificateChainViewer().setInput(new TreeNode[] { firstElement });
				getOkButton().setEnabled(listViewer.getChecked(firstElement));
				getCertificateChainViewer().refresh();
			}
		};
	}

	/**
	 * The <code>ListSelectionDialog</code> implementation of this
	 * <code>Dialog</code> method builds a list of the selected elements for later
	 * retrieval by the client and closes this dialog.
	 */
	@Override
	protected void okPressed() {
		// Get the input children.
		Object[] children = contentProvider.getElements(inputElement);

		// Build a list of selected children.
		if (children != null) {
			ArrayList<Object> list = new ArrayList<>();
			for (Object element : children) {
				if (listViewer.getChecked(element)) {
					list.add(element);
				}
			}
			setResult(list);
		}
		super.okPressed();
	}

	private static String userFriendlyFingerPrint(PGPPublicKey key) {
		if (key == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for (byte b : key.getFingerprint()) {
			builder.append(String.format("%02X", Byte.toUnsignedInt(b))); //$NON-NLS-1$
		}
		return builder.toString();
	}
}
