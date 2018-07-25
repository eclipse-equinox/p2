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

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.viewers.CertificateLabelProvider;
import org.eclipse.equinox.internal.provisional.security.ui.X509CertificateViewDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * A dialog that displays a certificate chain and asks the user if they
 * trust the certificate providers.
 */
public class TrustCertificateDialog extends SelectionDialog {

	private Object inputElement;
	private IStructuredContentProvider contentProvider;
	private ILabelProvider labelProvider;

	private final static int SIZING_SELECTION_WIDGET_HEIGHT = 250;
	private final static int SIZING_SELECTION_WIDGET_WIDTH = 300;

	CheckboxTableViewer listViewer;

	private TreeViewer certificateChainViewer;
	private Button detailsButton;
	protected TreeNode parentElement;
	protected Object selectedCertificate;

	public TrustCertificateDialog(Shell parentShell, Object input, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
		super(parentShell);
		inputElement = input;
		this.contentProvider = contentProvider;
		this.labelProvider = labelProvider;
		setTitle(ProvUIMessages.TrustCertificateDialog_Title);
		setMessage(ProvUIMessages.TrustCertificateDialog_Message);
		setShellStyle(SWT.DIALOG_TRIM | SWT.MODELESS | SWT.RESIZE | getDefaultOrientation());
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
				certificateChainViewer.setInput(new TreeNode[] {(TreeNode) nodes[0]});
				selectedCertificate = nodes[0];
			}
			listViewer.setSelection(selection);
		}
		listViewer.addDoubleClickListener(getDoubleClickListener());
		listViewer.addSelectionChangedListener(getParentSelectionListener());
		createButtons(composite);
		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, ProvUIMessages.TrustCertificateDialog_AcceptSelectedButtonLabel, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		super.getOkButton().setEnabled(false);
	}

	private void createButtons(Composite composite) {
		// Details button to view certificate chain
		detailsButton = new Button(composite, SWT.NONE);
		detailsButton.setText(ProvUIMessages.TrustCertificateDialog_Details);
		detailsButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				if (selectedCertificate != null) {
					X509Certificate cert = (X509Certificate) ((TreeNode) selectedCertificate).getValue();
					X509CertificateViewDialog certificateDialog = new X509CertificateViewDialog(getShell(), cert);
					certificateDialog.open();
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

		listViewer.setLabelProvider(labelProvider);
		listViewer.setContentProvider(contentProvider);

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

		Button selectButton = createButton(buttonComposite, IDialogConstants.SELECT_ALL_ID, ProvUIMessages.TrustCertificateDialog_SelectAll, false);

		SelectionListener listener = widgetSelectedAdapter(e -> {
			listViewer.setAllChecked(true);
			getOkButton().setEnabled(true);
		});
		selectButton.addSelectionListener(listener);

		Button deselectButton = createButton(buttonComposite, IDialogConstants.DESELECT_ALL_ID, ProvUIMessages.TrustCertificateDialog_DeselectAll, false);

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
				// create and open dialog for certificate chain
				X509CertificateViewDialog certificateViewDialog = new X509CertificateViewDialog(getShell(), (X509Certificate) treeNode.getValue());
				certificateViewDialog.open();
			}
		};
	}

	private ISelectionChangedListener getParentSelectionListener() {
		return event -> {
			ISelection selection = event.getSelection();
			if (selection instanceof StructuredSelection) {
				TreeNode firstElement = (TreeNode) ((StructuredSelection) selection).getFirstElement();
				getCertificateChainViewer().setInput(new TreeNode[] {firstElement});
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
}
