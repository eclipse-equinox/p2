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
import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.p2.ui.ProvisioningUndoSupport;
import org.eclipse.equinox.p2.ui.admin.ProvAdminUIActivator;
import org.eclipse.equinox.p2.ui.viewers.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.ViewPart;

/**
 * This class supports the common characteristics for views that manipulate
 * provisioning models.
 * 
 * @since 3.4
 */
abstract class ProvView extends ViewPart {
	TreeViewer viewer;
	private UndoRedoActionGroup undoRedoGroup;
	Action refreshAction;
	private IPropertyChangeListener preferenceListener;
	protected Display display;

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
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		setTreeColumns(viewer.getTree());
		viewer.getTree().setHeaderVisible(true);
		viewer.setContentProvider(getContentProvider());
		viewer.setInput(getInput());
		viewer.setLabelProvider(getLabelProvider());
		viewer.setComparator(new ViewerComparator());
		configureViewer(viewer);
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
		undoRedoGroup.fillActionBars(bars);
	}

	protected abstract void fillLocalPullDown(IMenuManager manager);

	protected abstract void fillContextMenu(IMenuManager manager);

	protected abstract void fillLocalToolBar(IToolBarManager manager);

	protected abstract IAction getDoubleClickAction();

	protected void makeActions() {
		undoRedoGroup = new UndoRedoActionGroup(getSite(), ProvisioningUndoSupport.getProvisioningUndoContext(), true);
		refreshAction = new Action(ProvAdminUIMessages.ProvView_RefreshCommandLabel) {
			public void run() {
				viewer.refresh();
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
		if (undoRedoGroup != null) {
			undoRedoGroup.dispose();
		}
		removeListeners();
		super.dispose();
	}

	protected void addListeners() {
		IPreferenceStore store = ProvAdminUIActivator.getDefault().getPreferenceStore();
		preferenceListener = new IPropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(PreferenceConstants.PREF_SHOW_GROUPS_ONLY) || event.getProperty().equals(PreferenceConstants.PREF_HIDE_IMPLEMENTATION_REPOS)) {
					configureViewerFilter(viewer);
				}
			}

		};
		store.addPropertyChangeListener(preferenceListener);

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

	IStructuredSelection getSelection() {
		return (IStructuredSelection) viewer.getSelection();
	}

	protected void configureViewer(final TreeViewer treeViewer) {
		// Filter IU's by group if the preference calls for it
		// TODO probably want a filter menu on the view
		configureViewerFilter(treeViewer);
	}

	protected void selectionChanged(IStructuredSelection selection) {
		// subclasses may override.  Do nothing here.
	}

	protected abstract IContentProvider getContentProvider();

	protected Object getInput() {
		return null;
	}

	protected void configureViewerFilter(TreeViewer treeViewer) {
		ArrayList filters = new ArrayList();
		IPreferenceStore store = ProvAdminUIActivator.getDefault().getPreferenceStore();
		if (store.getBoolean(PreferenceConstants.PREF_SHOW_GROUPS_ONLY)) {
			filters.add(new IUGroupFilter());
		}
		if (store.getBoolean(PreferenceConstants.PREF_HIDE_IMPLEMENTATION_REPOS)) {
			filters.add(new InternalRepositoryFilter());
		}
		treeViewer.setFilters((ViewerFilter[]) filters.toArray(new ViewerFilter[filters.size()]));
	}

	protected void setTreeColumns(Tree tree) {
		// TODO generalize?
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
}
