/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.internal.p2.ui.model.IRepositoryElement;
import org.eclipse.equinox.internal.p2.ui.viewers.RepositoryContentProvider;
import org.eclipse.equinox.internal.p2.ui.viewers.StructuredViewerProvisioningListener;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.jface.action.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.statushandlers.StatusManager;

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
			RepositoryTracker tracker = RepositoriesView.this.getRepositoryTracker();
			Object[] elements = getSelection().toArray();
			ArrayList<URI> uris = new ArrayList<URI>(elements.length);
			for (int i = 0; i < elements.length; i++) {
				if (elements[i] instanceof IRepositoryElement<?>)
					uris.add(((IRepositoryElement<?>) elements[i]).getLocation());
			}
			tracker.removeRepositories(uris.toArray(new URI[uris.size()]), RepositoriesView.this.getProvisioningUI().getSession());
		}
	}

	private class AddRepositoryAction extends Action {
		AddRepositoryAction() {
			setText(getAddCommandLabel());
			setToolTipText(getAddCommandTooltip());
			setImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.IMG_ARTIFACT_REPOSITORY));

		}

		public void run() {
			Object[] elements = ((ITreeContentProvider) viewer.getContentProvider()).getElements(getInput());
			ArrayList<URI> urls = new ArrayList<URI>();
			for (int i = 0; i < elements.length; i++)
				if (elements[i] instanceof IRepositoryElement<?>)
					urls.add(((IRepositoryElement<?>) elements[i]).getLocation());
			openAddRepositoryDialog(getShell());
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
		listener = new StructuredViewerProvisioningListener(getClass().getName(), viewer, getListenerEventTypes(), ui.getOperationRunner()) {
			protected void refreshViewer() {
				RepositoriesView.this.refreshAll(false);
			}
		};
		ProvUI.getProvisioningEventBus(ui.getSession()).addListener(listener);
	}

	protected void removeListeners() {
		super.removeListeners();
		ProvUI.getProvisioningEventBus(ui.getSession()).removeListener(listener);
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
		return new RepositoryContentProvider();

	}

	protected abstract RepositoryTracker getRepositoryTracker();

	protected abstract int openAddRepositoryDialog(Shell shell);

	protected abstract String getAddCommandLabel();

	protected abstract String getAddCommandTooltip();

	protected String getRemoveCommandLabel() {
		return ProvAdminUIMessages.RepositoriesView_RemoveCommandLabel;
	}

	protected abstract String getRemoveCommandTooltip();

	protected boolean isRepository(Object element) {
		return element instanceof IRepositoryElement<?>;
	}

	protected abstract int getListenerEventTypes();

	protected List<String> getVisualProperties() {
		List<String> list = super.getVisualProperties();
		list.add(PreferenceConstants.PREF_HIDE_SYSTEM_REPOS);
		return list;
	}

	protected void refreshUnderlyingModel() {
		IWorkbenchSiteProgressService service = getSite().getAdapter(IWorkbenchSiteProgressService.class);
		if (service != null) {
			try {
				service.run(true, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						getRepositoryTracker().refreshRepositories(getRepositoryTracker().getKnownRepositories(getProvisioningUI().getSession()), getProvisioningUI().getSession(), monitor);
					}
				});
			} catch (InvocationTargetException e) {
				ProvUI.handleException(e, null, StatusManager.SHOW);
			} catch (InterruptedException e) {
				// ignore
			}
		} else
			getRepositoryTracker().refreshRepositories(getRepositoryTracker().getKnownRepositories(getProvisioningUI().getSession()), getProvisioningUI().getSession(), new NullProgressMonitor());
	}
}
