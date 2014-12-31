/*******************************************************************************
 *  Copyright (c) 2008, 2014 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rapicorp, Inc. - add support for information dialog
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.net.URL;
import java.security.cert.Certificate;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.dialogs.TrustCertificateDialog;
import org.eclipse.equinox.internal.p2.ui.dialogs.UserValidationDialog;
import org.eclipse.equinox.internal.p2.ui.viewers.CertificateLabelProvider;
import org.eclipse.equinox.p2.core.UIServices;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * The default GUI-based implementation of {@link UIServices}.
 * The service declaration is made in the serviceui_component.xml file.

 */
public class ValidationDialogServiceUI extends UIServices {

	static final class MessageDialogWithLink extends MessageDialog {
		private final String linkText;

		MessageDialogWithLink(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels, int defaultIndex, String linkText) {
			super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
			this.linkText = linkText;
		}

		protected Control createCustomArea(Composite parent) {
			if (linkText == null)
				return super.createCustomArea(parent);

			Link link = new Link(parent, SWT.NONE);
			link.setText(linkText);
			link.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					try {
						URL url = new URL(e.text);
						PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(url);
					} catch (Exception x) {
						ProvUIActivator.getDefault().getLog().log(//
								new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, x.getMessage(), x));
					}
				}
			});
			return link;
		}
	}

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

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.IServiceUI#getUsernamePassword(java.lang.String)
	 */
	public AuthenticationInfo getUsernamePassword(final String location) {

		final AuthenticationInfo[] result = new AuthenticationInfo[1];
		if (!suppressAuthentication() && !isHeadless()) {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					Shell shell = ProvUI.getDefaultParentShell();
					String message = NLS.bind(ProvUIMessages.ServiceUI_LoginDetails, location);
					UserValidationDialog dialog = new UserValidationDialog(shell, ProvUIMessages.ServiceUI_LoginRequired, null, message);
					int dialogCode = dialog.open();
					if (dialogCode == Window.OK) {
						result[0] = dialog.getResult();
					} else if (dialogCode == Window.CANCEL) {
						result[0] = AUTHENTICATION_PROMPT_CANCELED;
					}
				}

			});
		}
		return result[0];
	}

	private boolean suppressAuthentication() {
		Job job = Job.getJobManager().currentJob();
		if (job != null) {
			return job.getProperty(LoadMetadataRepositoryJob.SUPPRESS_AUTHENTICATION_JOB_MARKER) != null;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.IServiceUI#showCertificates(java.lang.Object)
	 */
	public TrustInfo getTrustInfo(Certificate[][] untrustedChains, final String[] unsignedDetail) {
		boolean trustUnsigned = true;
		boolean persistTrust = false;
		Certificate[] trusted = new Certificate[0];
		// Some day we may summarize all of this in one UI, or perhaps we'll have a preference to honor regarding
		// unsigned content.  For now we prompt separately first as to whether unsigned detail should be trusted
		if (!isHeadless() && unsignedDetail != null && unsignedDetail.length > 0) {
			final boolean[] result = new boolean[] {false};
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					Shell shell = ProvUI.getDefaultParentShell();
					OkCancelErrorDialog dialog = new OkCancelErrorDialog(shell, ProvUIMessages.ServiceUI_warning_title, null, createStatus(), IStatus.WARNING);
					result[0] = dialog.open() == IDialogConstants.OK_ID;
				}

				private IStatus createStatus() {
					MultiStatus parent = new MultiStatus(ProvUIActivator.PLUGIN_ID, 0, ProvUIMessages.ServiceUI_unsigned_message, null);
					for (int i = 0; i < unsignedDetail.length; i++) {
						parent.add(new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, unsignedDetail[i]));
					}
					return parent;
				}
			});
			trustUnsigned = result[0];
		}
		// For now, there is no need to show certificates if there was unsigned content and we don't trust it.
		if (!trustUnsigned)
			return new TrustInfo(trusted, persistTrust, trustUnsigned);

		// We've established trust for unsigned content, now examine the untrusted chains
		if (!isHeadless() && untrustedChains != null && untrustedChains.length > 0) {

			final Object[] result = new Object[1];
			final TreeNode[] input = createTreeNodes(untrustedChains);

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
			persistTrust = true;
			trusted = (Certificate[]) result[0];
		}
		return new TrustInfo(trusted, persistTrust, trustUnsigned);
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
		if (!suppressAuthentication() && !isHeadless()) {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					Shell shell = ProvUI.getDefaultParentShell();
					String message = null;
					if (previousInfo.saveResult())
						message = NLS.bind(ProvUIMessages.ProvUIMessages_SavedNotAccepted_EnterFor_0, location);
					else
						message = NLS.bind(ProvUIMessages.ProvUIMessages_NotAccepted_EnterFor_0, location);

					UserValidationDialog dialog = new UserValidationDialog(previousInfo, shell, ProvUIMessages.ServiceUI_LoginRequired, null, message);
					int dialogCode = dialog.open();
					if (dialogCode == Window.OK) {
						result[0] = dialog.getResult();
					} else if (dialogCode == Window.CANCEL) {
						result[0] = AUTHENTICATION_PROMPT_CANCELED;
					}
				}

			});
		}
		return result[0];
	}

	public void showInformationMessage(final String title, final String text, final String linkText) {
		if (isHeadless()) {
			super.showInformationMessage(title, text, linkText);
			return;
		}
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				MessageDialog dialog = new MessageDialogWithLink(ProvUI.getDefaultParentShell(), title, null, text, MessageDialog.INFORMATION, new String[] {IDialogConstants.OK_LABEL}, 0, linkText);
				dialog.open();
			}
		});
	}

	private boolean isHeadless() {
		// If there is no UI available and we are still the IServiceUI, 
		// assume that the operation should proceed.  See
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=291049
		return !PlatformUI.isWorkbenchRunning();
	}

}
