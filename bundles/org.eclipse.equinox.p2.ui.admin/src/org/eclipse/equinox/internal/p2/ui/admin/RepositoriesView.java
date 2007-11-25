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

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.equinox.p2.ui.admin.ProvAdminUIActivator;
import org.eclipse.equinox.p2.ui.operations.ProvisioningOperation;
import org.eclipse.equinox.p2.ui.viewers.RepositoryContentProvider;
import org.eclipse.equinox.p2.ui.viewers.StructuredViewerProvisioningListener;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;

/**
 * This class supports the common characteristics for views that manipulate
 * provisioning repositories.
 * 
 * @since 3.4
 */
abstract class RepositoriesView extends ProvView {

	private class RemoveRepositoryAction extends Action {
		RemoveRepositoryAction() {
			setText(getRemoveCommandLabel());
			setToolTipText(getRemoveCommandTooltip());
			setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
			setDisabledImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE_DISABLED));

		}

		public void run() {
			ProvisioningOperationRunner.schedule(getRemoveOperation(getSelection().toArray()), getShell());
		}
	}

	private class AddRepositoryAction extends Action {
		AddRepositoryAction() {
			setText(getAddCommandLabel());
			setToolTipText(getAddCommandTooltip());
			setImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.IMG_ARTIFACT_REPOSITORY));

		}

		public void run() {
			openAddRepositoryDialog(getShell(), ((ITreeContentProvider) viewer.getContentProvider()).getElements(getInput()));
		}
	}

	private Action addRepositoryAction, removeRepositoryAction;
	private PropertyDialogAction propertiesAction;

	private StructuredViewerProvisioningListener listener;

	/**
	 * The constructor.
	 */
	public RepositoriesView() {
		// nothing to do
	}

	protected void addListeners() {
		super.addListeners();
		listener = new StructuredViewerProvisioningListener(viewer, StructuredViewerProvisioningListener.PROV_EVENT_REPOSITORY, ProvAdminUIActivator.getDefault().getQueryProvider());
		ProvUIActivator.getDefault().addProvisioningListener(listener);
	}

	protected void removeListeners() {
		super.removeListeners();
		ProvUIActivator.getDefault().removeProvisioningListener(listener);
	}

	protected void fillLocalPullDown(IMenuManager manager) {
		manager.add(addRepositoryAction);
		manager.add(removeRepositoryAction);
		manager.add(propertiesAction);
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(addRepositoryAction);
		if (removeRepositoryAction.isEnabled()) {
			manager.add(removeRepositoryAction);
		}
		if (propertiesAction.isEnabled()) {
			manager.add(new Separator());
			manager.add(propertiesAction);
		}
	}

	protected void fillLocalToolBar(IToolBarManager manager) {
		manager.add(addRepositoryAction);
		manager.add(removeRepositoryAction);
	}

	protected void makeActions() {
		super.makeActions();
		addRepositoryAction = new AddRepositoryAction();
		removeRepositoryAction = new RemoveRepositoryAction();
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.DELETE.getId(), removeRepositoryAction);

		propertiesAction = new PropertyDialogAction(this.getSite(), viewer);
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), propertiesAction);
		IStructuredSelection selection = getSelection();
		if (selection.size() == 1 && isRepository(selection.getFirstElement())) {
			propertiesAction.setEnabled(true);
			removeRepositoryAction.setEnabled(true);
		} else {
			propertiesAction.setEnabled(false);
			removeRepositoryAction.setEnabled(false);
		}
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ss = (IStructuredSelection) event.getSelection();
				RepositoriesView.this.selectionChanged(ss);
			}
		});
	}

	protected IAction getDoubleClickAction() {
		return propertiesAction;
	}

	protected void selectionChanged(IStructuredSelection selection) {
		propertiesAction.setEnabled(selection.size() == 1 && ((ProvUI.getAdapter(selection.getFirstElement(), IInstallableUnit.class) != null) || (isRepository(selection.getFirstElement()))));
		boolean enabled = false;
		Object[] selectionArray = selection.toArray();
		for (int i = 0; i < selectionArray.length; i++) {
			if (!isRepository(selectionArray[i])) {
				enabled = false;
				break;
			}
			enabled = true;
		}
		removeRepositoryAction.setEnabled(enabled);
	}

	protected IContentProvider getContentProvider() {
		return new RepositoryContentProvider(ProvAdminUIActivator.getDefault().getQueryProvider());

	}

	protected abstract int openAddRepositoryDialog(Shell shell, Object[] elements);

	protected abstract ProvisioningOperation getRemoveOperation(Object[] elements);

	protected abstract String getAddCommandLabel();

	protected abstract String getAddCommandTooltip();

	protected String getRemoveCommandLabel() {
		return ProvAdminUIMessages.RepositoriesView_RemoveCommandLabel;
	}

	protected abstract String getRemoveCommandTooltip();

	protected abstract boolean isRepository(Object element);
}