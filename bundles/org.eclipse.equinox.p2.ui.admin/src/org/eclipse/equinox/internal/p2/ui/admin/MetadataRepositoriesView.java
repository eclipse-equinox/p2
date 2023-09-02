/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin;

import java.util.List;
import org.eclipse.equinox.internal.p2.ui.ProvUIProvisioningListener;
import org.eclipse.equinox.internal.p2.ui.QueryableMetadataRepositoryManager;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddMetadataRepositoryDialog;
import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositories;
import org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDragAdapter;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.PluginTransfer;

/**
 * This view allows users to interact with metadata repositories
 * 
 * @since 3.4
 */
public class MetadataRepositoriesView extends RepositoriesView {

	private InstallAction installAction;
	private RepositoryTracker tracker;
	MetadataRepositories input;

	/**
	 * The constructor.
	 */
	public MetadataRepositoriesView() {
		// constructor
	}

	@Override
	protected Object getInput() {
		if (input == null) {
			// view by repo
			IUViewQueryContext context = new IUViewQueryContext(IUViewQueryContext.AVAILABLE_VIEW_BY_REPO);
			Policy policy = ProvAdminUIActivator.getDefault().getPolicy();
			context.setShowLatestVersionsOnly(policy.getShowLatestVersionsOnly());
			context.setShowInstallChildren(policy.getShowDrilldownRequirements());
			context.setShowProvisioningPlanChildren(policy.getShowDrilldownRequirements());
			context.setUseCategories(policy.getGroupByCategory());

			input = new MetadataRepositories(context, getProvisioningUI(), new QueryableMetadataRepositoryManager(getProvisioningUI(), false));
		}
		return input;
	}

	@Override
	protected String getAddCommandLabel() {
		return ProvAdminUIMessages.MetadataRepositoriesView_AddRepositoryLabel;
	}

	@Override
	protected String getAddCommandTooltip() {
		return ProvAdminUIMessages.MetadataRepositoriesView_AddRepositoryTooltip;
	}

	@Override
	protected String getRemoveCommandTooltip() {
		return ProvAdminUIMessages.MetadataRepositoriesView_RemoveRepositoryTooltip;
	}

	@Override
	protected int openAddRepositoryDialog(Shell shell) {
		return new AddMetadataRepositoryDialog(shell, getProvisioningUI()).open();
	}

	@Override
	protected void makeActions() {
		super.makeActions();
		installAction = new InstallAction(getProvisioningUI(), viewer);
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		if (installAction.isEnabled()) {
			manager.add(new Separator());
			manager.add(installAction);
		}
		super.fillContextMenu(manager);
	}

	@Override
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

	@Override
	protected int getListenerEventTypes() {
		return ProvUIProvisioningListener.PROV_EVENT_METADATA_REPOSITORY;
	}

	@Override
	protected List<String> getVisualProperties() {
		List<String> list = super.getVisualProperties();
		list.add(PreferenceConstants.PREF_USE_CATEGORIES);
		list.add(PreferenceConstants.PREF_COLLAPSE_IU_VERSIONS);
		return list;
	}

	@Override
	protected void updateCachesForPreferences() {
		// We want to reconstruct the input's query context based on the latest preferences
		input = null;
	}

	@Override
	protected RepositoryTracker getRepositoryTracker() {
		if (tracker == null) {
			tracker = SingleRepositoryTracker.createMetadataRepositoryTracker(getProvisioningUI());
		}
		return tracker;
	}
}
