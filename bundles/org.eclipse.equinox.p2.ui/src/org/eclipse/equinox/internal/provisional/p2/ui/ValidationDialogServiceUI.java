/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui;

import java.security.cert.Certificate;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.TrustCertificateDialog;
import org.eclipse.equinox.internal.p2.ui.dialogs.UserValidationDialog;
import org.eclipse.equinox.internal.p2.ui.viewers.CertificateLabelProvider;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUICheckUnsigned;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * The default GUI-based implementation of {@link IServiceUI}.
 */
public class ValidationDialogServiceUI implements IServiceUI, IServiceUICheckUnsigned {
	/**
	 * Subclassed to add a cancel button to the error dialog.
	 */
	static class OkCancelErrorDialog extends ErrorDialog {

		public OkCancelErrorDialog(Shell parentShell, String dialogTitle, String message, IStatus status, int displayMask) {
			super(parentShell, dialogTitle, message, status, displayMask);
		}

		protected void createButtonsForButtonBar(Composite parent) {
			// create OK and Details buttons
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
			createDetailsButton(parent);
		}
	}

	public static final QualifiedName SUPPRESS_AUTHENTICATION_JOB_MARKER = new QualifiedName(ProvUIActivator.PLUGIN_ID, "SUPPRESS_AUTHENTICATION_REQUESTS"); //$NON-NLS-1$

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.IServiceUI#getUsernamePassword(java.lang.String)
	 */
	public AuthenticationInfo getUsernamePassword(final String location) {

		final AuthenticationInfo[] result = new AuthenticationInfo[1];
		if (!suppressAuthentication()) {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					Shell shell = ProvUI.getDefaultParentShell();
					String[] buttonLabels = new String[] {ProvUIMessages.ServiceUI_OK, ProvUIMessages.ServiceUI_Cancel};
					String message = NLS.bind(ProvUIMessages.ServiceUI_LoginDetails, location);
					UserValidationDialog dialog = new UserValidationDialog(shell, ProvUIMessages.ServiceUI_LoginRequired, null, message, buttonLabels);
					if (dialog.open() == Window.OK) {
						result[0] = dialog.getResult();
					}
				}

			});
		}
		return result[0];
	}

	private boolean suppressAuthentication() {
		Job job = Job.getJobManager().currentJob();
		if (job != null) {
			return job.getProperty(SUPPRESS_AUTHENTICATION_JOB_MARKER) != null;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.IServiceUI#showCertificates(java.lang.Object)
	 */
	public Certificate[] showCertificates(final Certificate[][] certificates) {
		final Object[] result = new Object[1];
		final TreeNode[] input = createTreeNodes(certificates);
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				Shell shell = ProvUI.getDefaultParentShell();
				ILabelProvider labelProvider = new CertificateLabelProvider();
				TreeNodeContentProvider contentProvider = new TreeNodeContentProvider();
				TrustCertificateDialog trustCertificateDialog = new TrustCertificateDialog(shell, input, labelProvider, contentProvider);
				trustCertificateDialog.open();
				Certificate[] values = new Certificate[trustCertificateDialog.getResult() == null ? 0 : trustCertificateDialog.getResult().length];
				for (int i = 0; i < values.length; i++) {
					values[i] = (Certificate) ((TreeNode) trustCertificateDialog.getResult()[i]).getValue();
				}
				result[0] = values;
			}
		});
		return (Certificate[]) result[0];
	}

	private TreeNode[] createTreeNodes(Certificate[][] certificates) {
		TreeNode[] children = new TreeNode[certificates.length];
		for (int i = 0; i < certificates.length; i++) {
			TreeNode head = new TreeNode(certificates[i][0]);
			TreeNode parent = head;
			children[i] = head;
			for (int j = 0; j < certificates[i].length; j++) {
				TreeNode node = new TreeNode(certificates[i][j]);
				node.setParent(parent);
				parent.setChildren(new TreeNode[] {node});
				parent = node;
			}
		}
		return children;
	}

	public AuthenticationInfo getUsernamePassword(final String location, final AuthenticationInfo previousInfo) {

		final AuthenticationInfo[] result = new AuthenticationInfo[1];
		if (!suppressAuthentication()) {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					Shell shell = ProvUI.getDefaultParentShell();
					String[] buttonLabels = new String[] {ProvUIMessages.ServiceUI_OK, ProvUIMessages.ServiceUI_Cancel};
					String message = null;
					if (previousInfo.saveResult())
						message = NLS.bind(ProvUIMessages.ProvUIMessages_SavedNotAccepted_EnterFor_0, location);
					else
						message = NLS.bind(ProvUIMessages.ProvUIMessages_NotAccepted_EnterFor_0, location);

					UserValidationDialog dialog = new UserValidationDialog(previousInfo, shell, ProvUIMessages.ServiceUI_LoginRequired, null, message, buttonLabels);
					if (dialog.open() == Window.OK) {
						result[0] = dialog.getResult();
					}
				}

			});
		}
		return result[0];
	}

	public boolean promptForUnsignedContent(final String[] details) {
		final boolean[] result = new boolean[] {false};
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				Shell shell = ProvUI.getDefaultParentShell();
				OkCancelErrorDialog dialog = new OkCancelErrorDialog(shell, ProvUIMessages.ServiceUI_warning_title, null, createStatus(), IStatus.WARNING);
				result[0] = dialog.open() == IDialogConstants.OK_ID;
			}

			private IStatus createStatus() {
				MultiStatus parent = new MultiStatus(ProvUIActivator.PLUGIN_ID, 0, ProvUIMessages.ServiceUI_unsigned_message, null);
				for (int i = 0; i < details.length; i++) {
					parent.add(new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, details[i]));
				}
				return parent;
			}
		});
		return result[0];
	}
}
