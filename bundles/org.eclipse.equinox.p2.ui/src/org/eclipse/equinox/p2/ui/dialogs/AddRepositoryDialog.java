/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.equinox.p2.ui.operations.ProvisioningOperation;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Abstract dialog class for adding repositories of different types. This class
 * assumes the user view of a repository is a name and URL (and possibly other
 * info as this class develops). Individual subclasses will dictate what kind of
 * repository and how it's created.
 * 
 * @since 3.4
 * 
 */
public abstract class AddRepositoryDialog extends StatusDialog {

	private Button okButton;
	private IRepository[] knownRepositories;
	private RepositoryGroup repoGroup;

	public AddRepositoryDialog(Shell parentShell, IRepository[] knownRepositories) {

		super(parentShell);
		this.knownRepositories = knownRepositories;
		setTitle(ProvUIMessages.AddRepositoryDialog_Title);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	protected Control createDialogArea(Composite parent) {
		repoGroup = new RepositoryGroup(parent, null, new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				verifyComplete();
			}
		}, repositoryIsFile(), null, repositoryFileName());

		Dialog.applyDialogFont(repoGroup.getComposite());
		return repoGroup.getComposite();
	}

	protected void okPressed() {
		if (addRepository()) {
			setReturnCode(Window.OK);
			super.okPressed();
		} else {
			setReturnCode(Window.CANCEL);
		}
	}

	protected boolean addRepository() {
		URL newURL = makeRepositoryURL(repoGroup.getURLString());
		if (newURL == null) {
			return false;
		}

		final ProvisioningOperation op = getOperation(newURL, repoGroup.getRepositoryName());
		final IStatus[] status = new IStatus[1];
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
					status[0] = ProvisioningUndoSupport.execute(op, monitor, getShell());
					if (!status[0].isOK()) {
						StatusManager.getManager().handle(status[0], StatusManager.SHOW | StatusManager.LOG);
					}
				} catch (ExecutionException e) {
					ProvUI.handleException(e.getCause(), null);
					status[0] = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, null, null);
				}
			}
		};
		try {
			new ProgressMonitorDialog(getShell()).run(true, true, runnable);
		} catch (InterruptedException e) {
			// don't report thread interruption
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), null);
			status[0] = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, null, null);
		}
		return status[0].isOK();

	}

	protected abstract ProvisioningOperation getOperation(URL url, String name);

	protected abstract URL makeRepositoryURL(String urlString);

	protected abstract boolean repositoryIsFile();

	protected abstract String repositoryFileName();

	void verifyComplete() {
		if (okButton == null) {
			return;
		}
		IStatus status = repoGroup.verify();
		if (!status.isOK()) {
			okButton.setEnabled(false);
			updateStatus(status);
			return;
		}
		if (isDuplicate()) {
			return;
		}
		okButton.setEnabled(true);
		updateStatus(new Status(IStatus.OK, ProvUIActivator.PLUGIN_ID, IStatus.OK, "", null)); //$NON-NLS-1$

	}

	protected boolean isDuplicate() {
		String urlText = repoGroup.getURLString();
		for (int i = 0; i < knownRepositories.length; i++) {
			URL repURL = knownRepositories[i].getLocation();
			if (repURL != null && repURL.equals(urlText)) {
				setOkEnablement(false);
				this.updateStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, IStatus.OK, ProvUIMessages.AddRepositoryDialog_DuplicateURL, null));
				return true;
			}
		}
		return false;
	}

	protected void updateButtonsEnableState(IStatus status) {
		setOkEnablement(!status.matches(IStatus.ERROR));
	}

	protected void setOkEnablement(boolean enable) {
		if (okButton != null && !okButton.isDisposed())
			okButton.setEnabled(enable);
	}

}
