/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.actions.UninstallAction;
import org.eclipse.equinox.internal.p2.ui.actions.UpdateAction;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddProfileDialog;
import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.viewers.ProvElementContentProvider;
import org.eclipse.equinox.internal.p2.ui.viewers.StructuredViewerProvisioningListener;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
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
	protected Action addProfileAction, removeProfileAction, uninstallAction;
	UpdateAction updateAction;
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
			Object[] selections = getSelection().toArray();
			List<String> profilesOnly = new ArrayList<String>();
			for (int i = 0; i < selections.length; i++) {
				if (selections[i] instanceof ProfileElement)
					profilesOnly.add(((ProfileElement) selections[i]).getProfileId());
			}
			RemoveProfilesJob op = new RemoveProfilesJob(ProvAdminUIMessages.Ops_RemoveProfileOperationLabel, getProvisioningUI().getSession(), profilesOnly.toArray(new String[profilesOnly.size()]));
			ProfilesView.this.run(op);
		}
	}

	private class AddProfileAction extends Action {
		AddProfileAction() {
			setText(ProvAdminUIMessages.ProfilesView_AddProfileLabel);
			setToolTipText(ProvAdminUIMessages.ProfilesView_AddProfileTooltip);
			setImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.IMG_PROFILE));
		}

		public void run() {
			new AddProfileDialog(viewer.getControl().getShell(), getKnownProfileIds()).open();
		}
	}

	public ProfilesView() {
		// constructor
	}

	protected void addListeners() {
		super.addListeners();
		listener = new StructuredViewerProvisioningListener(getClass().getName(), viewer, ProvUIProvisioningListener.PROV_EVENT_IU | ProvUIProvisioningListener.PROV_EVENT_PROFILE, getProvisioningUI().getOperationRunner());
		ProvUI.getProvisioningEventBus(getProvisioningUI().getSession()).addListener(listener);
	}

	protected void removeListeners() {
		super.removeListeners();
		ProvUI.getProvisioningEventBus(getProvisioningUI().getSession()).removeListener(listener);
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
		uninstallAction = new UninstallAction(getProvisioningUI(), viewer, null);
		propertiesAction = new PropertyDialogAction(this.getSite(), viewer);
		updateAction = new UpdateAction(getProvisioningUI(), viewer, null, true);
		updateAction.setSkipSelectionPage(true);

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
		propertiesAction.setEnabled(false);
		removeProfileAction.setEnabled(false);
		if (ss.size() == 1) {
			propertiesAction.setEnabled(true);
		}
		Object[] selectionArray = ss.toArray();
		if (selectionArray.length > 0) {
			removeProfileAction.setEnabled(true);
			for (int i = 0; i < selectionArray.length; i++) {
				IProfile profile = ProvUI.getAdapter(selectionArray[i], IProfile.class);
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
		return new ProvElementContentProvider();
	}

	protected Object getInput() {
		return new Profiles(getProvisioningUI());
	}

	protected String getProfileId() {
		Object firstElement = getSelection().getFirstElement();
		if (firstElement instanceof InstalledIUElement) {
			return ((InstalledIUElement) firstElement).getProfileId();
		}
		IProfile profile = ProvUI.getAdapter(firstElement, IProfile.class);
		if (profile != null)
			return profile.getProfileId();
		return null;
	}

	protected List<String> getVisualProperties() {
		List<String> list = super.getVisualProperties();
		list.add(PreferenceConstants.PREF_SHOW_INSTALL_ROOTS_ONLY);
		return list;
	}

	String[] getKnownProfileIds() {
		IProfile[] allProfiles = ProvAdminUIActivator.getDefault().getProfileRegistry().getProfiles();
		String[] ids = new String[allProfiles.length];
		for (int i = 0; i < allProfiles.length; i++)
			ids[i] = allProfiles[i].getProfileId();
		return ids;
	}

	protected ProvisioningUI getProvisioningUI() {
		ProvisioningUI ui = ProvAdminUIActivator.getDefault().getProvisioningUI(getProfileId());
		if (ui != null)
			return ui;
		return super.getProvisioningUI();
	}
}
