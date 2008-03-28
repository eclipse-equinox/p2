/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin;

import java.net.URL;
import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddMetadataRepositoryDialog;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddProfileDialog;
import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.IProfileChooser;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.InstallAction;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.RevertAction;
import org.eclipse.equinox.internal.provisional.p2.ui.model.*;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.RemoveMetadataRepositoryOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
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
		MetadataRepositories input = new MetadataRepositories();
		input.setQueryProvider(ProvAdminUIActivator.getDefault().getQueryProvider());
		input.setQueryType(IQueryProvider.METADATA_REPOS);
		return input;
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

	protected int openAddRepositoryDialog(Shell shell) {
		return new AddMetadataRepositoryDialog(shell, getRepoFlags()).open();
	}

	protected ProvisioningOperation getRemoveOperation(Object[] elements) {
		ArrayList urls = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof RepositoryElement)
				urls.add(((RepositoryElement) elements[i]).getLocation());
		}
		return new RemoveMetadataRepositoryOperation(ProvAdminUIMessages.ArtifactRepositoriesView_RemoveRepositoryOperationLabel, (URL[]) urls.toArray(new URL[urls.size()]));
	}

	protected void makeActions() {
		super.makeActions();
		installAction = new InstallAction(viewer, null, getProfileChooser(), ProvAdminUIActivator.getDefault().getPolicies(), getShell());
		revertAction = new RevertAction(viewer, null, getProfileChooser(), ProvAdminUIActivator.getDefault().getPolicies(), getShell());
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
					IProfile profile = (IProfile) ProvUI.getAdapter(result[0], IProfile.class);
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

	protected int getRepoFlags() {
		if (ProvAdminUIActivator.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.PREF_HIDE_SYSTEM_REPOS))
			return IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM;
		return IMetadataRepositoryManager.REPOSITORIES_ALL;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.admin.RepositoriesView#getListenerEventTypes()
	 */
	protected int getListenerEventTypes() {
		return StructuredViewerProvisioningListener.PROV_EVENT_METADATA_REPOSITORY;
	}

}
