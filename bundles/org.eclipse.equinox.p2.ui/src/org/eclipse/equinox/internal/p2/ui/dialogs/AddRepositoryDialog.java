/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.internal.p2.ui.IProvHelpContextIds;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * Abstract dialog class for adding repositories of different types. This class
 * assumes the user view of a repository is a name and URI. Individual subclasses 
 * will dictate what kind of repository and how it's created.
 * 
 * @since 3.4
 * 
 */
public abstract class AddRepositoryDialog extends RepositoryNameAndLocationDialog {

	URI addedLocation;
	static final String[] ARCHIVE_EXTENSIONS = new String[] {"*.jar;*.zip"}; //$NON-NLS-1$ 
	static String lastLocalLocation = null;
	static String lastArchiveLocation = null;
	Policy policy;

	public AddRepositoryDialog(Shell parentShell, ProvisioningUI ui) {
		super(parentShell, ui);
		setTitle(ProvUIMessages.AddRepositoryDialog_Title);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parentShell, IProvHelpContextIds.ADD_REPOSITORY_DIALOG);
	}

	protected Control createDialogArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		initializeDialogUnits(comp);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginTop = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);

		comp.setLayout(layout);
		GridData data = new GridData();
		comp.setLayoutData(data);

		// Name: []
		nickname = createNameField(comp);

		Button localButton = new Button(comp, SWT.PUSH);
		localButton.setText(ProvUIMessages.RepositoryGroup_LocalRepoBrowseButton);
		localButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.APPLICATION_MODAL);
				dialog.setMessage(ProvUIMessages.RepositoryGroup_SelectRepositoryDirectory);
				dialog.setFilterPath(lastLocalLocation);
				String path = dialog.open();
				if (path != null) {
					lastLocalLocation = path;
					url.setText(makeLocalURIString(path));
					validateRepositoryURL(false);
				}
			}
		});
		setButtonLayoutData(localButton);

		// Location: []
		url = createLocationField(comp);

		Button archiveButton = new Button(comp, SWT.PUSH);
		archiveButton.setText(ProvUIMessages.RepositoryGroup_ArchivedRepoBrowseButton);
		archiveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				FileDialog dialog = new FileDialog(getShell(), SWT.APPLICATION_MODAL);
				dialog.setText(ProvUIMessages.RepositoryGroup_RepositoryFile);
				dialog.setFilterExtensions(ARCHIVE_EXTENSIONS);
				dialog.setFileName(lastArchiveLocation);
				String path = dialog.open();
				if (path != null) {
					lastArchiveLocation = path;
					url.setText(makeLocalURIString(path));
					validateRepositoryURL(false);
				}
			}
		});
		setButtonLayoutData(archiveButton);
		comp.setTabList(new Control[] {nickname, url, localButton, archiveButton});
		Dialog.applyDialogFont(comp);
		return comp;
	}

	String makeLocalURIString(String path) {
		try {
			URI localURI = URIUtil.fromString(path);
			return URIUtil.toUnencodedString(RepositoryHelper.localRepoURIHelper(localURI));
		} catch (URISyntaxException e) {
			return path;
		}
	}

	protected boolean handleOk() {
		IStatus status = addRepository();
		return status.isOK();
	}

	/**
	 * Get the location of the repository that was added by this dialog.  Return <code>null</code>
	 * if the dialog has not yet added a repository location.
	 * 
	 * @return the location of the repository that has been added by this dialog, or <code>null</code>
	 * if no repository has been added.
	 */
	public URI getAddedLocation() {
		return addedLocation;
	}

	protected IStatus addRepository() {
		IStatus status = validateRepositoryURL(false);
		if (status.isOK()) {
			addedLocation = getUserLocation();
			String nick = nickname.getText().trim();
			if (nick.length() == 0)
				nick = null;
			getRepositoryTracker().addRepository(addedLocation, nick, getProvisioningUI().getSession());
		}
		return status;
	}
}
