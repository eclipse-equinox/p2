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
package org.eclipse.equinox.internal.p2.ui.admin;

import java.net.URL;
import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddMetadataRepositoryDialog;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddProfileDialog;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.ui.IProfileChooser;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.actions.InstallAction;
import org.eclipse.equinox.p2.ui.actions.RevertAction;
import org.eclipse.equinox.p2.ui.admin.ProvAdminUIActivator;
import org.eclipse.equinox.p2.ui.model.*;
import org.eclipse.equinox.p2.ui.operations.ProvisioningOperation;
import org.eclipse.equinox.p2.ui.operations.RemoveMetadataRepositoryOperation;
import org.eclipse.equinox.p2.ui.viewers.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.part.PluginTransfer;

/**
 * This view allows users to interact with metadata repositories
 * 
 * @since 3.4
 */
public class MetadataRepositoriesView extends RepositoriesView {

	private InstallAction installAction;
	private RevertAction revertAction;

	/**
	 * The constructor.
	 */
	public MetadataRepositoriesView() {
		// constructor
	}

	protected Object getInput() {
		return new MetadataRepositories();
	}

	protected String getAddCommandLabel() {
		return ProvAdminUIMessages.MetadataRepositoriesView_AddRepositoryLabel;
	}

	protected String getAddCommandTooltip() {
		return ProvAdminUIMessages.MetadataRepositoriesView_AddRepositoryTooltip;
	}

	protected String getRemoveCommandTooltip() {
		return ProvAdminUIMessages.MetadataRepositoriesView_RemoveRepositoryTooltip;
	}

	protected int openAddRepositoryDialog(Shell shell, URL[] knownRepos) {
		return new AddMetadataRepositoryDialog(shell, knownRepos).open();
	}

	protected ProvisioningOperation getRemoveOperation(Object[] elements) {
		ArrayList urls = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof RepositoryElement)
				urls.add(((RepositoryElement) elements[i]).getURL());
		}
		return new RemoveMetadataRepositoryOperation(ProvAdminUIMessages.ArtifactRepositoriesView_RemoveRepositoryOperationLabel, (URL[]) urls.toArray(new URL[urls.size()]));
	}

	protected void makeActions() {
		super.makeActions();
		installAction = new InstallAction(viewer, null, getProfileChooser(), null, getShell());
		revertAction = new RevertAction(viewer, null, getProfileChooser(), getShell());
	}

	private IProfileChooser getProfileChooser() {
		return new IProfileChooser() {

			public String getProfileId(Shell shell) {
				// TODO would be nice if the profile chooser dialog let you
				// create a new profile
				DeferredQueryContentProvider provider = new DeferredQueryContentProvider(ProvAdminUIActivator.getDefault().getQueryProvider());
				if (provider.getElements(new Profiles()).length == 0) {
					AddProfileDialog dialog = new AddProfileDialog(shell, new String[0]);
					if (dialog.open() == Window.OK) {
						return dialog.getAddedProfileId();
					}
					return null;
				}

				ListDialog dialog = new ListDialog(getShell());
				dialog.setTitle(ProvAdminUIMessages.MetadataRepositoriesView_ChooseProfileDialogTitle);
				dialog.setLabelProvider(new ProvElementLabelProvider());
				dialog.setInput(new Profiles());
				dialog.setContentProvider(provider);
				dialog.open();
				Object[] result = dialog.getResult();
				if (result != null && result.length > 0) {
					Profile profile = (Profile) ProvUI.getAdapter(result[0], Profile.class);
					if (profile != null)
						return profile.getProfileId();
				}
				return null;
			}

			public String getLabel() {
				return ProvAdminUIMessages.MetadataRepositoriesView_ChooseProfileDialogTitle;
			}
		};
	}

	protected void fillContextMenu(IMenuManager manager) {
		if (installAction.isEnabled()) {
			manager.add(new Separator());
			manager.add(installAction);
			manager.add(revertAction);
		}
		super.fillContextMenu(manager);
	}

	protected boolean isRepository(Object element) {
		return element instanceof MetadataRepositoryElement;
	}

	protected void configureViewer(final TreeViewer treeViewer) {
		super.configureViewer(treeViewer);
		// Add drag support for IU's
		Transfer[] transfers = new Transfer[] {org.eclipse.jface.util.LocalSelectionTransfer.getTransfer(), PluginTransfer.getInstance(), TextTransfer.getInstance(),};
		treeViewer.addDragSupport(DND.DROP_COPY, transfers, new IUDragAdapter(treeViewer));
	}

}