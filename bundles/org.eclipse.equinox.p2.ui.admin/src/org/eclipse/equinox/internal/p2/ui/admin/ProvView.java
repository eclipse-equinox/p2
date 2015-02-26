/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.actions.RefreshAction;
import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/**
 * This class supports the common characteristics for views that manipulate
 * provisioning models.
 * 
 * @since 3.4
 */
abstract class ProvView extends ViewPart {
	protected TreeViewer viewer;
	RefreshAction refreshAction;
	private IPropertyChangeListener preferenceListener;
	protected Display display;
	protected ProvisioningUI ui;

	/**
	 * The constructor.
	 */
	public ProvView() {
		// constructor
	}

	/**
	 * Create and initialize the viewer
	 */
	public void createPartControl(Composite parent) {
		// Store the display so we can make async calls from listeners
		display = parent.getDisplay();
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		viewer.getTree().setHeaderVisible(true);
		configureViewer(viewer);
		// Do this after setting up sorters, filters, etc.
		// Otherwise it will retrieve content on each change.
		viewer.setContentProvider(getContentProvider());

		// Now set up the visuals, columns before labels.
		setTreeColumns(viewer.getTree());
		viewer.setLabelProvider(getLabelProvider());

		// Input is last
		viewer.setInput(getInput());

		addListeners();
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ProvView.this.fillContextMenu(manager);
				manager.add(new Separator());
				manager.add(refreshAction);
				manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IAction action = getDoubleClickAction();
				if (action != null && action.isEnabled()) {
					action.run();
				}
			}
		});
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		IMenuManager manager = bars.getMenuManager();
		fillLocalPullDown(manager);
		manager.add(new Separator());
		manager.add(refreshAction);

		fillLocalToolBar(bars.getToolBarManager());
	}

	protected abstract void fillLocalPullDown(IMenuManager manager);

	protected abstract void fillContextMenu(IMenuManager manager);

	protected abstract void fillLocalToolBar(IToolBarManager manager);

	protected abstract IAction getDoubleClickAction();

	protected void makeActions() {
		refreshAction = new RefreshAction(ProvisioningUI.getDefaultUI(), viewer, viewer.getControl()) {
			protected void refresh() {
				refreshAll(true);
			}
		};
		refreshAction.setToolTipText(ProvAdminUIMessages.ProvView_RefreshCommandTooltip);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	public void dispose() {
		removeListeners();
		super.dispose();
	}

	protected void addListeners() {
		IPreferenceStore store = ProvAdminUIActivator.getDefault().getPreferenceStore();
		preferenceListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (getVisualProperties().contains(event.getProperty())) {
					updateCachesForPreferences();
					ProvView.this.refreshAll(false);
				}
			}

		};
		store.addPropertyChangeListener(preferenceListener);
	}

	protected void updateCachesForPreferences() {
		// default is to do nothing
	}

	protected void removeListeners() {
		if (preferenceListener != null) {
			IPreferenceStore store = ProvAdminUIActivator.getDefault().getPreferenceStore();
			store.removePropertyChangeListener(preferenceListener);
		}
	}

	Shell getShell() {
		return viewer.getControl().getShell();
	}

	Control getViewerControl() {
		return viewer.getControl();
	}

	protected IStructuredSelection getSelection() {
		return (IStructuredSelection) viewer.getSelection();
	}

	protected void run(ProvisioningJob job) {
		IWorkbenchSiteProgressService service = getSite().getService(IWorkbenchSiteProgressService.class);
		if (service != null)
			service.schedule(job);
		else
			job.runModal(new NullProgressMonitor());
	}

	protected void configureViewer(final TreeViewer treeViewer) {
		viewer.setComparator(new IUComparator(IUComparator.IU_ID));
		viewer.setComparer(new ProvElementComparer());
	}

	protected void selectionChanged(IStructuredSelection selection) {
		// subclasses may override.  Do nothing here.
	}

	protected abstract IContentProvider getContentProvider();

	protected Object getInput() {
		return null;
	}

	protected void setTreeColumns(Tree tree) {
		// For now we set two columns and the content depends on the elements
		TreeColumn tc = new TreeColumn(tree, SWT.LEFT, 0);
		tc.setResizable(true);
		tc.setWidth(400);
		tc = new TreeColumn(tree, SWT.LEFT, 1);
		tc.setWidth(600);
		tc.setResizable(true);
	}

	protected ILabelProvider getLabelProvider() {
		return new ProvElementLabelProvider();
	}

	protected void refreshUnderlyingModel() {
		// do nothing by default
	}

	protected List<String> getVisualProperties() {
		ArrayList<String> list = new ArrayList<String>(1);
		list.add(PreferenceConstants.PREF_SHOW_GROUPS_ONLY);
		return list;
	}

	final void refreshAll(boolean refreshModel) {
		// Refresh the underlying elements
		if (refreshModel)
			refreshUnderlyingModel();
		// We then reset the input to ensure that anything the content providers 
		// are caching gets reset also.  The net effect is that everything 
		// will get queried again.
		viewer.setInput(getInput());
	}

	protected String getProfileId() {
		return null;
	}

	protected ProvisioningUI getProvisioningUI() {
		if (ui == null) {
			ui = ProvAdminUIActivator.getDefault().getProvisioningUI(getProfileId());
		}
		return ui;
	}
}
