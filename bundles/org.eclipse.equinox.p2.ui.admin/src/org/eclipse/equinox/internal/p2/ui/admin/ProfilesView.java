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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddProfileDialog;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.UpdateAndInstallDialog;
import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.equinox.p2.ui.actions.*;
import org.eclipse.equinox.p2.ui.admin.ProvAdminUIActivator;
import org.eclipse.equinox.p2.ui.model.*;
import org.eclipse.equinox.p2.ui.operations.*;
import org.eclipse.equinox.p2.ui.viewers.InstallIUDropAdapter;
import org.eclipse.equinox.p2.ui.viewers.StructuredViewerProvisioningListener;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;
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
				Object element;
				if ((element = list.get(i)) instanceof Profile) {
					profilesOnly.add(element);
				}
			}
			IUndoableOperation op = new RemoveProfilesOperation(ProvAdminUIMessages.Ops_RemoveProfileOperationLabel, (Profile[]) profilesOnly.toArray(new Profile[profilesOnly.size()]));
			try {
				// TODO hook into platform progress service
				IStatus status = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(op, null, ProvUI.getUIInfoAdapter(ProfilesView.this.getShell()));
				if (status.isOK()) {
					viewer.refresh();
				}
			} catch (ExecutionException e) {
				ProvUI.handleException(e.getCause(), null);
			}
		}
	}

	private class AddProfileAction extends Action {
		AddProfileAction() {
			setText(ProvAdminUIMessages.ProfilesView_AddProfileLabel);
			setToolTipText(ProvAdminUIMessages.ProfilesView_AddProfileTooltip);
			setImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.IMG_PROFILE));
		}

		public void run() {
			Profile[] profiles = (Profile[]) ((ITreeContentProvider) viewer.getContentProvider()).getElements(null);
			new AddProfileDialog(viewer.getControl().getShell(), profiles).open();
		}
	}

	private class InstallIntoProfileAction extends Action {
		InstallIntoProfileAction() {
			setText(ProvAdminUIMessages.InstallIUCommandLabel);
			setToolTipText(ProvAdminUIMessages.InstallIUCommandTooltip);
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
		listener = new StructuredViewerProvisioningListener(viewer, StructuredViewerProvisioningListener.PROV_EVENT_IU | StructuredViewerProvisioningListener.PROV_EVENT_PROFILE);
		ProvUIActivator.getDefault().addProvisioningListener(listener);
	}

	protected void removeListeners() {
		super.removeListeners();
		ProvUIActivator.getDefault().removeProvisioningListener(listener);
	}

	protected void configureViewer(TreeViewer treeViewer) {
		super.configureViewer(treeViewer);
		treeViewer.setInput(new AllProfiles());
		InstallIUDropAdapter adapter = new InstallIUDropAdapter(treeViewer, getOperationConfirmer());
		adapter.setFeedbackEnabled(false);
		adapter.setEntryPointStrategy(InstallAction.ENTRYPOINT_OPTIONAL);
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
		uninstallAction = new UninstallAction(ProvAdminUIMessages.UninstallIUCommandLabel, viewer, getOperationConfirmer(), null, getProfileChooser(), getShell());
		uninstallAction.setToolTipText(ProvAdminUIMessages.UninstallIUCommandTooltip);
		updateAction = new UpdateAction(ProvAdminUIMessages.UpdateIUCommandLabel, viewer, getOperationConfirmer(), null, getProfileChooser(), getShell());
		updateAction.setToolTipText(ProvAdminUIMessages.UpdateIUCommandTooltip);
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
		if (ss.size() == 1) {
			propertiesAction.setEnabled(true);
			if (ss.getFirstElement() instanceof Profile)
				installAction.setEnabled(true);
			else
				installAction.setEnabled(false);
		} else {
			propertiesAction.setEnabled(false);
		}
		Object[] selectionArray = ss.toArray();
		Object parent = null;
		if (selectionArray.length > 0) {
			uninstallAction.setEnabled(true);
			updateAction.setEnabled(true);
			removeProfileAction.setEnabled(true);

			for (int i = 0; i < selectionArray.length; i++) {
				if (selectionArray[i] instanceof InstalledIUElement) {
					InstalledIUElement element = (InstalledIUElement) selectionArray[i];
					if (parent == null) {
						parent = element.getParent(null);
					} else if (parent != element.getParent(null)) {
						uninstallAction.setEnabled(false);
						updateAction.setEnabled(false);
						break;
					}
				} else {
					uninstallAction.setEnabled(false);
					updateAction.setEnabled(false);
					break;
				}
			}
			// If the selections weren't all IU's, see if they are all
			// profiles
			if (!uninstallAction.isEnabled()) {
				for (int i = 0; i < selectionArray.length; i++) {
					if (!(selectionArray[i] instanceof Profile)) {
						removeProfileAction.setEnabled(false);
						break;
					}
				}
			} else {
				removeProfileAction.setEnabled(false);
			}
		} else {
			uninstallAction.setEnabled(false);
			updateAction.setEnabled(false);
			removeProfileAction.setEnabled(false);
		}
	}

	protected IAction getDoubleClickAction() {
		return propertiesAction;
	}

	protected IContentProvider getContentProvider() {
		return new ProfileContentProvider();
	}

	protected Object getInput() {
		return new AllProfiles();
	}

	IProfileChooser getProfileChooser() {
		return new IProfileChooser() {
			public Profile getProfile() {
				Object firstElement = getSelection().getFirstElement();
				if (firstElement instanceof InstalledIUElement) {
					return ((InstalledIUElement) firstElement).getProfile();
				}
				if (firstElement instanceof Profile) {
					return (Profile) firstElement;
				}
				return null;
			}

			public String getLabel() {
				return ProvAdminUIMessages.MetadataRepositoriesView_ChooseProfileDialogTitle;
			}
		};
	}

	private IOperationConfirmer getOperationConfirmer() {
		return new IOperationConfirmer() {

			public boolean continuePerformingOperation(ProvisioningOperation op, Shell shell) {
				String confirmMessage;
				if (op instanceof InstallOperation) {
					confirmMessage = NLS.bind(ProvAdminUIMessages.Ops_ConfirmIUInstall, ((InstallOperation) op).getProfileId());
				} else if (op instanceof UninstallOperation) {
					confirmMessage = ProvAdminUIMessages.ProfilesView_ConfirmUninstallMessage;
				} else {
					return true;
				}
				boolean proceed = true;
				IPreferenceStore store = ProvAdminUIActivator.getDefault().getPreferenceStore();
				if (store.getBoolean(PreferenceConstants.PREF_CONFIRM_SELECTION_INSTALL)) {
					MessageDialogWithToggle dlg = MessageDialogWithToggle.openYesNoCancelQuestion(shell, op.getLabel(), confirmMessage, ProvAdminUIMessages.ProfilesView_AlwaysConfirmSelectionInstallOps, true, null, null);
					int ret = dlg.getReturnCode();
					if (!(ret == Window.CANCEL || ret == -1)) { // return
						// code of -1 corresponds with ESC
						// even if no was pressed, we still want to store
						// the toggle state.
						store.setValue(PreferenceConstants.PREF_CONFIRM_SELECTION_INSTALL, dlg.getToggleState());
					}
					proceed = dlg.getReturnCode() == IDialogConstants.YES_ID;
				}
				return proceed;
			}
		};
	}

}