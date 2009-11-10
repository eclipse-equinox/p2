/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.p2.ui.ProvUIProvisioningListener;
import org.eclipse.equinox.internal.p2.ui.actions.InstallAction;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddMetadataRepositoryDialog;
import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.internal.p2.ui.model.IRepositoryElement;
import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositories;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDragAdapter;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.operations.RemoveRepositoryJob;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.PluginTransfer;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * This view allows users to interact with metadata repositories
 * 
 * @since 3.4
 */
public class MetadataRepositoriesView extends RepositoriesView {

	private InstallAction installAction;

	/**
	 * The constructor.
	 */
	public MetadataRepositoriesView() {
		// constructor
	}

	protected Object getInput() {
		return new MetadataRepositories(getProvisioningUI());
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
		return new AddMetadataRepositoryDialog(shell, getProvisioningUI()).open();
	}

	protected RemoveRepositoryJob getRemoveOperation(Object[] elements) {
		ArrayList locations = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof IRepositoryElement)
				locations.add(((IRepositoryElement) elements[i]).getLocation());
		}
		return new RemoveMetadataRepositoryOperation(ProvAdminUIMessages.ArtifactRepositoriesView_RemoveRepositoryOperationLabel, getProvisioningUI().getSession(), (URI[]) locations.toArray(new URI[locations.size()]));
	}

	protected void makeActions() {
		super.makeActions();
		installAction = new InstallAction(getProvisioningUI(), viewer, null);
	}

	protected void fillContextMenu(IMenuManager manager) {
		if (installAction.isEnabled()) {
			manager.add(new Separator());
			manager.add(installAction);
		}
		super.fillContextMenu(manager);
	}

	protected void configureViewer(final TreeViewer treeViewer) {
		super.configureViewer(treeViewer);
		// Add drag support for IU's
		Transfer[] transfers = new Transfer[] {org.eclipse.jface.util.LocalSelectionTransfer.getTransfer(), PluginTransfer.getInstance(), TextTransfer.getInstance(),};
		treeViewer.addDragSupport(DND.DROP_COPY, transfers, new IUDragAdapter(treeViewer));
	}

	protected int getRepoFlags() {
		if (ProvAdminUIActivator.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.PREF_HIDE_SYSTEM_REPOS))
			return IRepositoryManager.REPOSITORIES_NON_SYSTEM;
		return IRepositoryManager.REPOSITORIES_ALL;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.admin.RepositoriesView#getListenerEventTypes()
	 */
	protected int getListenerEventTypes() {
		return ProvUIProvisioningListener.PROV_EVENT_METADATA_REPOSITORY;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.admin.ProvView#refreshUnderlyingModel()
	 */
	protected void refreshUnderlyingModel() {
		getProvisioningUI().schedule(new RefreshMetadataRepositoriesOperation(ProvAdminUIMessages.ProvView_RefreshCommandLabel, getProvisioningUI().getSession(), getRepoFlags()), StatusManager.SHOW | StatusManager.LOG);
	}

	protected List getVisualProperties() {
		List list = super.getVisualProperties();
		list.add(PreferenceConstants.PREF_USE_CATEGORIES);
		list.add(PreferenceConstants.PREF_COLLAPSE_IU_VERSIONS);
		return list;
	}
}
