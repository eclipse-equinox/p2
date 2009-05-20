/*******************************************************************************
 *  Copyright (c) 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.security.cert.X509Certificate;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.viewers.CertificateLabelProvider;
import org.eclipse.equinox.internal.provisional.security.ui.X509CertificateViewDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ListSelectionDialog;

/**
 * A dialog that displays a certificate chain and asks the user if they
 * trust the certificate providers.
 */
public class TrustCertificateDialog extends ListSelectionDialog {

	private TreeViewer certificateChainViewer;
	private Button detailsButton;
	protected TreeNode parentElement;
	protected Object selectedCertificate;

	public TrustCertificateDialog(Shell parentShell, Object input, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
		super(parentShell, input, contentProvider, labelProvider, ProvUIMessages.TrustCertificateDialog_Title);
		setShellStyle(SWT.DIALOG_TRIM | SWT.MODELESS | SWT.RESIZE | getDefaultOrientation());
	}

	private void createButtons(Composite composite) {
		// Details button to view certificate chain
		detailsButton = new Button(composite, SWT.NONE);
		detailsButton.setText(ProvUIMessages.TrustCertificateDialog_Details);
		detailsButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				if (selectedCertificate != null) {
					X509Certificate cert = (X509Certificate) ((TreeNode) selectedCertificate).getValue();
					X509CertificateViewDialog certificateDialog = new X509CertificateViewDialog(getShell(), cert);
					certificateDialog.open();
				}
			}

			public void widgetSelected(SelectionEvent e) {
				widgetDefaultSelected(e);
			}
		});
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
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
		Object input = getViewer().getInput();
		if (input instanceof Object[]) {
			ISelection selection = null;
			Object[] nodes = (Object[]) input;
			if (nodes.length > 0) {
				selection = new StructuredSelection(nodes[0]);
				certificateChainViewer.setInput(new TreeNode[] {(TreeNode) nodes[0]});
				selectedCertificate = nodes[0];
			}
			getViewer().setSelection(selection);
		}
		getViewer().addDoubleClickListener(getDoubleClickListener());
		getViewer().addSelectionChangedListener(getParentSelectionListener());
		createButtons(composite);
		return composite;
	}

	private ISelectionChangedListener getChainSelectionListener() {
		return new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof StructuredSelection) {
					selectedCertificate = ((StructuredSelection) selection).getFirstElement();
				}
			}
		};
	}

	public TreeViewer getCertificateChainViewer() {
		return certificateChainViewer;
	}

	private IDoubleClickListener getDoubleClickListener() {
		return new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection selection = (StructuredSelection) event.getSelection();
				Object selectedElement = selection.getFirstElement();
				if (selectedElement instanceof TreeNode) {
					TreeNode treeNode = (TreeNode) selectedElement;
					// create and open dialog for certificate chain
					X509CertificateViewDialog certificateViewDialog = new X509CertificateViewDialog(getShell(), (X509Certificate) treeNode.getValue());
					certificateViewDialog.open();
				}
			}
		};
	}

	private ISelectionChangedListener getParentSelectionListener() {
		return new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof StructuredSelection) {
					getCertificateChainViewer().setInput(new TreeNode[] {(TreeNode) ((StructuredSelection) selection).getFirstElement()});
					getCertificateChainViewer().refresh();
				}
			}
		};
	}
}
