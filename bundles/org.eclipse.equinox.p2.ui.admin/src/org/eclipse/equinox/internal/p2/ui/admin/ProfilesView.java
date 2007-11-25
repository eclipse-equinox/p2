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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddProfileDialog;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.UpdateAndInstallDialog;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.equinox.p2.ui.actions.UninstallAction;
import org.eclipse.equinox.p2.ui.actions.UpdateAction;
import org.eclipse.equinox.p2.ui.admin.ProvAdminUIActivator;
import org.eclipse.equinox.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.p2.ui.model.Profiles;
import org.eclipse.equinox.p2.ui.operations.ProfileOperation;
import org.eclipse.equinox.p2.ui.operations.RemoveProfilesOperation;
import org.eclipse.equinox.p2.ui.viewers.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;

/**
 * This view allows users to interact with installed profiles.
 * 
 * @since 3.4
 */
public class ProfilesView extends ProvView {
	private Action addProfileAction, removeProfileAction, uninstallAction, updateAction, installAction;
	private PropertyDialogAction propertiesAction;
	private StructuredViewerProvisioningListener listener;

	private class RemoveProfileAction extends Action {
		RemoveProfileAction() {
			setText(ProvAdminUIMessages.ProfilesView_RemoveProfileLabel);
			setToolTipText(ProvAdminUIMessages.ProfilesView_RemoveProfileTooltip);
			setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
			setDisabledImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE_DISABLED));

		}

		public void run() {
			List list = getSelection().toList();
			List profilesOnly = new ArrayList();
			for (int i = 0; i < list.size(); i++) {
				Profile profile = (Profile) ProvUI.getAdapter(list.get(i), Profile.class);
				if (profile != null) {
					profilesOnly.add(profile);
				}
			}
			ProfileOperation op = new RemoveProfilesOperation(ProvAdminUIMessages.Ops_RemoveProfileOperationLabel, (Profile[]) profilesOnly.toArray(new Profile[profilesOnly.size()]));
			ProvisioningOperationRunner.schedule(op, ProfilesView.this.getShell());
		}
	}

	private class AddProfileAction extends Action {
		AddProfileAction() {
			setText(ProvAdminUIMessages.ProfilesView_AddProfileLabel);
			setToolTipText(ProvAdminUIMessages.ProfilesView_AddProfileTooltip);
			setImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.IMG_PROFILE));
		}

		public void run() {
			new AddProfileDialog(viewer.getControl().getShell(), ((ITreeContentProvider) viewer.getContentProvider()).getElements(getInput())).open();
		}
	}

	private class InstallIntoProfileAction extends Action {
		InstallIntoProfileAction() {
			setText(ProvUI.INSTALL_COMMAND_LABEL);
			setToolTipText(ProvUI.INSTALL_COMMAND_TOOLTIP);
		}

		public void run() {
			Profile profile = getProfileChooser().getProfile();
			if (profile != null) {
				UpdateAndInstallDialog dialog = new UpdateAndInstallDialog(getShell(), profile);
				dialog.open();
			}
		}
	}

	public ProfilesView() {
		// constructor
	}

	protected void addListeners() {
		super.addListeners();
		listener = new StructuredViewerProvisioningListener(viewer, StructuredViewerProvisioningListener.PROV_EVENT_IU | StructuredViewerProvisioningListener.PROV_EVENT_PROFILE, ProvAdminUIActivator.getDefault().getQueryProvider());
		ProvUIActivator.getDefault().addProvisioningListener(listener);
	}

	protected void removeListeners() {
		super.removeListeners();
		ProvUIActivator.getDefault().removeProvisioningListener(listener);
	}

	protected void configureViewer(TreeViewer treeViewer) {
		super.configureViewer(treeViewer);
		InstallIUDropAdapter adapter = new InstallIUDropAdapter(treeViewer);
		adapter.setFeedbackEnabled(false);
		Transfer[] transfers = new Transfer[] {org.eclipse.jface.util.LocalSelectionTransfer.getTransfer()};
		treeViewer.addDropSupport(DND.DROP_COPY, transfers, adapter);
	}

	protected void fillLocalPullDown(IMenuManager manager) {
		manager.add(addProfileAction);
		manager.add(removeProfileAction);
		manager.add(propertiesAction);
		manager.add(new Separator());
		manager.add(installAction);
		manager.add(updateAction);
		manager.add(uninstallAction);
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(addProfileAction);
		if (removeProfileAction.isEnabled()) {
			manager.add(removeProfileAction);
		}
		if (propertiesAction.isEnabled()) {
			manager.add(propertiesAction);
		}
		if (updateAction.isEnabled()) {
			manager.add(new Separator());
			manager.add(updateAction);
			manager.add(uninstallAction);
		}
		if (installAction.isEnabled()) {
			manager.add(new Separator());
			manager.add(installAction);
		}
	}

	protected void fillLocalToolBar(IToolBarManager manager) {
		manager.add(new Separator());
		manager.add(addProfileAction);
		manager.add(removeProfileAction);
	}

	protected void makeActions() {
		super.makeActions();
		addProfileAction = new AddProfileAction();
		removeProfileAction = new RemoveProfileAction();
		uninstallAction = new UninstallAction(viewer, null, getProfileChooser(), getShell());
		updateAction = new UpdateAction(viewer, null, getProfileChooser(), getShell());
		propertiesAction = new PropertyDialogAction(this.getSite(), viewer);
		installAction = new InstallIntoProfileAction();

		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), propertiesAction);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ss = (IStructuredSelection) event.getSelection();
				ProfilesView.this.selectionChanged(ss);
			}
		});

		// prime the action validation
		selectionChanged((IStructuredSelection) viewer.getSelection());
	}

	protected void selectionChanged(IStructuredSelection ss) {
		super.selectionChanged(ss);
		installAction.setEnabled(false);
		propertiesAction.setEnabled(false);
		removeProfileAction.setEnabled(false);
		if (ss.size() == 1) {
			propertiesAction.setEnabled(true);
			Profile profile = (Profile) ProvUI.getAdapter(ss.getFirstElement(), Profile.class);
			if (profile != null)
				installAction.setEnabled(true);
		}
		Object[] selectionArray = ss.toArray();
		if (selectionArray.length > 0) {
			removeProfileAction.setEnabled(true);
			for (int i = 0; i < selectionArray.length; i++) {
				Profile profile = (Profile) ProvUI.getAdapter(selectionArray[i], Profile.class);
				if (profile == null) {
					removeProfileAction.setEnabled(false);
					break;
				}
			}
		}
	}

	protected IAction getDoubleClickAction() {
		return propertiesAction;
	}

	protected IContentProvider getContentProvider() {
		return new DeferredQueryContentProvider(ProvAdminUIActivator.getDefault().getQueryProvider());
	}

	protected Object getInput() {
		return new Profiles();
	}

	IProfileChooser getProfileChooser() {
		return new IProfileChooser() {
			public Profile getProfile() {
				Object firstElement = getSelection().getFirstElement();
				if (firstElement instanceof InstalledIUElement) {
					return ((InstalledIUElement) firstElement).getProfile();
				}
				return (Profile) ProvUI.getAdapter(firstElement, Profile.class);
			}

			public String getLabel() {
				return ProvAdminUIMessages.MetadataRepositoriesView_ChooseProfileDialogTitle;
			}
		};
	}
}