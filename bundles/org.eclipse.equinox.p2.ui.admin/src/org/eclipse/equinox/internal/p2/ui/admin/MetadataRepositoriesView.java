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

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddMetadataRepositoryDialog;
import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.InstallAction;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IRepositoryElement;
import org.eclipse.equinox.internal.provisional.p2.ui.model.MetadataRepositories;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.IUDragAdapter;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.StructuredViewerProvisioningListener;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
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

	private class RevertAction extends Action {

		RevertAction() {
			setText(ProvUI.REVERT_COMMAND_LABEL);
			setToolTipText(ProvUI.REVERT_COMMAND_TOOLTIP);
			setImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.IMG_PROFILE));
		}

		public void run() {
			IInstallableUnit[] ius = getSelectedIUs();
			String targetProfileId = ProvAdminUIActivator.getDefault().getPolicy().getProfileChooser().getProfileId(getShell());
			ProvisioningPlan plan = getProvisioningPlan(ius);
			if (ProvAdminUIActivator.getDefault().getPolicy().getPlanValidator().continueWorkingWithPlan(plan, getShell())) {
				ProvisioningOperation op = new ProfileModificationOperation(ProvAdminUIMessages.MetadataRepositoriesView_RevertLabel, targetProfileId, plan);
				ProvisioningOperationRunner.schedule(op, getShell(), StatusManager.SHOW | StatusManager.LOG);
			}
		}

		protected IInstallableUnit[] getSelectedIUs() {
			ISelection selection = viewer.getSelection();
			if (!(selection instanceof IStructuredSelection))
				return new IInstallableUnit[0];
			List elements = ((IStructuredSelection) selection).toList();
			List iusList = new ArrayList(elements.size());

			for (int i = 0; i < elements.size(); i++) {
				IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(elements.get(i), IInstallableUnit.class);
				if (iu != null && !ProvisioningUtil.isCategory(iu))
					iusList.add(iu);
			}
			return (IInstallableUnit[]) iusList.toArray(new IInstallableUnit[iusList.size()]);
		}

		private ProvisioningPlan getProvisioningPlan(final IInstallableUnit[] ius) {
			final ProvisioningPlan[] plan = new ProvisioningPlan[1];
			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					try {
						if (ius.length == 1)
							plan[0] = ProvisioningUtil.getRevertPlan(ius[0], monitor);
					} catch (ProvisionException e) {
						ProvUI.handleException(e, ProvAdminUIMessages.MetadataRepositoriesView_UnexpectedRevertError, StatusManager.BLOCK | StatusManager.LOG);
					}
				}
			};
			try {
				new ProgressMonitorDialog(getShell()).run(true, true, runnable);
			} catch (InterruptedException e) {
				// don't report thread interruption
			} catch (InvocationTargetException e) {
				ProvUI.handleException(e.getCause(), ProvAdminUIMessages.MetadataRepositoriesView_UnexpectedRevertError, StatusManager.BLOCK | StatusManager.LOG);
			}
			return plan[0];
		}

	}

	private InstallAction installAction;
	private RevertAction revertAction;

	/**
	 * The constructor.
	 */
	public MetadataRepositoriesView() {
		// constructor
	}

	protected Object getInput() {
		return new MetadataRepositories(ProvAdminUIActivator.getDefault().getPolicy());
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
		ArrayList locations = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof IRepositoryElement)
				locations.add(((IRepositoryElement) elements[i]).getLocation());
		}
		return new RemoveMetadataRepositoryOperation(ProvAdminUIMessages.ArtifactRepositoriesView_RemoveRepositoryOperationLabel, (URI[]) locations.toArray(new URI[locations.size()]));
	}

	protected void makeActions() {
		super.makeActions();
		installAction = new InstallAction(ProvAdminUIActivator.getDefault().getPolicy(), viewer, null);
		revertAction = new RevertAction();
	}

	protected void fillContextMenu(IMenuManager manager) {
		if (installAction.isEnabled()) {
			manager.add(new Separator());
			manager.add(installAction);
			manager.add(revertAction);
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
		return StructuredViewerProvisioningListener.PROV_EVENT_METADATA_REPOSITORY;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.admin.ProvView#refreshUnderlyingModel()
	 */
	protected void refreshUnderlyingModel() {
		ProvisioningOperationRunner.schedule(new RefreshMetadataRepositoriesOperation(ProvAdminUIMessages.ProvView_RefreshCommandLabel, getRepoFlags()), getShell(), StatusManager.SHOW | StatusManager.LOG);
	}
}
