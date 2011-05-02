/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.net.MalformedURLException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.statushandlers.StatusManager;

public class UpdateSingleIUPage extends ProvisioningWizardPage {

	UpdateOperation operation;

	protected UpdateSingleIUPage(UpdateOperation operation, ProvisioningUI ui) {
		super("UpdateSingleIUPage", ui, null); //$NON-NLS-1$
		setTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
		IProduct product = Platform.getProduct();
		String productName = product != null && product.getName() != null ? product.getName() : ProvUIMessages.ApplicationInRestartDialog;
		setDescription(NLS.bind(ProvUIMessages.UpdateSingleIUPage_SingleUpdateDescription, productName));
		Assert.isNotNull(operation);
		Assert.isTrue(operation.hasResolved());
		Assert.isTrue(operation.getSelectedUpdates().length == 1);
		Assert.isTrue(operation.getResolutionResult().isOK());
		this.operation = operation;
	}

	public void createControl(Composite parent) {
		IInstallableUnit updateIU = getUpdate().replacement;
		String url = null;
		if (updateIU.getUpdateDescriptor().getLocation() != null)
			try {
				url = URIUtil.toURL(updateIU.getUpdateDescriptor().getLocation()).toExternalForm();
			} catch (MalformedURLException e) {
				// ignore and null URL will be ignored below
			}
		if (url != null) {
			Browser browser = null;
			try {
				browser = new Browser(parent, SWT.NONE);
				browser.setUrl(url);
				browser.setBackground(parent.getBackground());
				Point size = getProvisioningUI().getPolicy().getUpdateDetailsPreferredSize();
				if (size != null) {
					browser.setSize(size);
				}
				setControl(browser);
				return;
			} catch (SWTError e) {
				// Fall through to backup plan.
			}
		}
		// Create a text description of the update.
		Text text = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY);
		text.setBackground(parent.getBackground());
		text.setText(getUpdateText(updateIU));
		setControl(text);
	}

	private String getUpdateText(IInstallableUnit iu) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(new IUDetailsLabelProvider().getClipboardText(getUpdate().replacement, CopyUtils.DELIMITER));
		buffer.append(CopyUtils.NEWLINE);
		buffer.append(CopyUtils.NEWLINE);
		String text = iu.getUpdateDescriptor().getDescription();
		if (text != null)
			buffer.append(text);
		else {
			text = iu.getProperty(IInstallableUnit.PROP_DESCRIPTION);
			if (text != null)
				buffer.append(text);
		}
		return buffer.toString();

	}

	public boolean performFinish() {
		if (operation.getResolutionResult().getSeverity() != IStatus.ERROR) {
			getProvisioningUI().schedule(operation.getProvisioningJob(null), StatusManager.SHOW | StatusManager.LOG);
			return true;
		}
		return false;
	}

	@Override
	protected String getClipboardText(Control control) {
		return getUpdate().toString();
	}

	private Update getUpdate() {
		return operation.getSelectedUpdates()[0];
	}

}
