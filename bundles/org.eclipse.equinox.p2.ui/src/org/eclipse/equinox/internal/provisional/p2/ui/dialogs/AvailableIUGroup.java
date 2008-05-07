/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import java.net.URL;
import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.DeferredFetchFilteredTree;
import org.eclipse.equinox.internal.p2.ui.dialogs.StructuredIUGroup;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryEvent;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IRepositoryElement;
import org.eclipse.equinox.internal.provisional.p2.ui.model.MetadataRepositories;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement;
import org.eclipse.equinox.internal.provisional.p2.ui.query.QueryContext;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * An AvailableIUGroup is a reusable UI component that displays the
 * IU's available for installation.
 * 
 * @since 3.4
 */
public class AvailableIUGroup extends StructuredIUGroup {

	class CheckSelectionProvider implements ISelectionProvider, ICheckStateListener {

		CheckboxTreeViewer checkboxViewer;
		private ListenerList listeners = new ListenerList();
		List checkedNotGrayed;

		CheckSelectionProvider(CheckboxTreeViewer v) {
			this.checkboxViewer = v;
			v.addCheckStateListener(this);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
		 */
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			listeners.add(listener);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
		 */
		public ISelection getSelection() {
			return new IStructuredSelection() {
				public Object getFirstElement() {
					if (size() == 0)
						return null;
					return toList().get(0);
				}

				public Iterator iterator() {
					return toList().iterator();
				}

				public int size() {
					return toList().size();
				}

				public Object[] toArray() {
					return toList().toArray();
				}

				public List toList() {
					return getCheckedNotGrayed();
				}

				public boolean isEmpty() {
					return toList().isEmpty();
				}

			};
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
		 */
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			listeners.remove(listener);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
		 */
		public void setSelection(ISelection selection) {
			if (selection instanceof IStructuredSelection) {
				checkboxViewer.setCheckedElements(((IStructuredSelection) selection).toArray());
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ICheckStateListener#checkStateChanged(org.eclipse.jface.viewers.CheckStateChangedEvent)
		 */
		public void checkStateChanged(CheckStateChangedEvent event) {
			final Object[] listenerArray = listeners.getListeners();
			checkedNotGrayed = null;
			SelectionChangedEvent selectionEvent = new SelectionChangedEvent(this, this.getSelection());
			for (int i = 0; i < listenerArray.length; i++) {
				((ISelectionChangedListener) listenerArray[i]).selectionChanged(selectionEvent);
			}
		}

		List getCheckedNotGrayed() {
			if (checkedNotGrayed == null) {
				Object[] checked = checkboxViewer.getCheckedElements();
				checkedNotGrayed = new ArrayList(checked.length);
				for (int i = 0; i < checked.length; i++)
					if (!checkboxViewer.getGrayed(checked[i]))
						checkedNotGrayed.add(checked[i]);
			}
			return checkedNotGrayed;

		}
	}

	QueryContext queryContext;
	// We restrict the type of the filter used because PatternFilter does
	// unnecessary accesses of children that cause problems with the deferred
	// tree.
	AvailableIUPatternFilter filter;
	private IViewMenuProvider menuProvider;
	private boolean useBold = false;
	private boolean useCheckboxes = false;
	private IUDetailsLabelProvider labelProvider;
	private Display display;
	boolean ignoreEvent = false;
	DeferredFetchFilteredTree filteredTree;
	IUColumnConfig[] columnConfig;
	private int refreshRepoFlags = IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM;
	ISelectionProvider selectionProvider;
	Job lastRequestedLoadJob;

	/**
	 * Create a group that represents the available IU's but does not use any of the
	 * view menu or check box capabilities.
	 * 
	 * @param parent the parent composite for the group
	 * @param queryProvider the query provider that defines the queries used
	 * to retrieve elements in the viewer.
	 * @param font The font to use for calculating pixel sizes.  This font is
	 * not managed by the receiver.
	 * @param context the ProvisioningContext describing the context for provisioning,
	 * including information about which repositories should be used.
	 */
	public AvailableIUGroup(final Composite parent, IQueryProvider queryProvider, Font font, ProvisioningContext context) {
		this(parent, queryProvider, font, context, null, null, ProvUI.getIUColumnConfig(), null, false);
	}

	/**
	 * Create a group that represents the available IU's.
	 * 
	 * @param parent the parent composite for the group
	 * @param queryProvider the query provider that defines the queries used
	 * to retrieve elements in the viewer.
	 * @param font The font to use for calculating pixel sizes.  This font is
	 * not managed by the receiver.
	 * @param context the ProvisioningContext describing the context for provisioning,
	 * including information about which repositories should be used.
	 * @param queryContext the QueryContext describing additional information about how
	 * the model should be traversed in this view.
	 * @param filter the AvailableIUPatternFilter to use to filter the tree contents.  If <code>null</code>,
	 * then a default will be used.
	 * @param columnConfig the description of the columns that should be shown.  If <code>null</code>, a default
	 * will be used.
	 * @param menuProvider the IMenuProvider that fills the view menu.  If <code>null</code>,
	 * then there is no view menu shown.
	 * @param useCheckboxes a boolean indicating whether a checkbox selection model should be
	 * used.  If <code>true</code>, a check box selection model will be used and the group's 
	 * implementation of ISelectionProvider will use the checks as the selection.
	 */
	public AvailableIUGroup(final Composite parent, IQueryProvider queryProvider, Font font, ProvisioningContext context, QueryContext queryContext, AvailableIUPatternFilter filter, IUColumnConfig[] columnConfig, IViewMenuProvider menuProvider, boolean useCheckboxes) {
		super(parent, queryProvider, font, context);
		this.display = parent.getDisplay();
		this.queryContext = queryContext;
		this.filter = filter;
		this.menuProvider = menuProvider;
		this.useCheckboxes = useCheckboxes;
		if (columnConfig == null)
			this.columnConfig = ProvUI.getIUColumnConfig();
		else
			this.columnConfig = columnConfig;
		if (filter == null)
			this.filter = new AvailableIUPatternFilter(this.columnConfig);
		else
			this.filter = filter;
		createGroupComposite(parent);
	}

	protected StructuredViewer createViewer(Composite parent) {
		// Table of available IU's
		filteredTree = new DeferredFetchFilteredTree(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, filter, menuProvider, parent.getDisplay(), useCheckboxes);
		final TreeViewer availableIUViewer = filteredTree.getViewer();
		if (availableIUViewer instanceof CheckboxTreeViewer)
			selectionProvider = new CheckSelectionProvider((CheckboxTreeViewer) availableIUViewer);
		else
			selectionProvider = availableIUViewer;

		// If the user expanded or collapsed anything while we were loading a repo
		// in the background, we would not want to disrupt their work by making
		// a newly loaded visible and expanding it.  Setting the load job to null 
		// will take care of this.
		availableIUViewer.getTree().addTreeListener(new TreeListener() {
			public void treeCollapsed(TreeEvent e) {
				lastRequestedLoadJob = null;
			}

			public void treeExpanded(TreeEvent e) {
				lastRequestedLoadJob = null;
			}
		});

		labelProvider = new IUDetailsLabelProvider(filteredTree, columnConfig, getShell());
		labelProvider.setUseBoldFontForFilteredItems(useBold);
		labelProvider.setToolTipProperty(IInstallableUnit.PROP_DESCRIPTION);

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		availableIUViewer.setComparator(new IUComparator(IUComparator.IU_NAME));
		availableIUViewer.setComparer(new ProvElementComparer());

		// Now the content provider.
		DeferredQueryContentProvider contentProvider = new DeferredQueryContentProvider(getQueryProvider());
		availableIUViewer.setContentProvider(contentProvider);

		// Now the presentation, columns before label provider.
		setTreeColumns(availableIUViewer.getTree());
		availableIUViewer.setLabelProvider(labelProvider);

		// Notify the filtered tree so that it can hook listeners on the
		// content provider.  This is needed so that filtering is only allowed
		// after content has been retrieved.
		filteredTree.contentProviderSet(contentProvider);

		// Input last.
		availableIUViewer.setInput(getNewInput());

		final StructuredViewerProvisioningListener listener = new StructuredViewerProvisioningListener(availableIUViewer, StructuredViewerProvisioningListener.PROV_EVENT_METADATA_REPOSITORY, getQueryProvider()) {
			protected void repositoryAdded(final RepositoryEvent event) {
				if (ignoreEvent) {
					ignoreEvent = false;
					return;
				}
				makeRepositoryVisible(event.getRepositoryLocation());
			}

			protected void repositoryDiscovered(RepositoryEvent event) {
				ignoreEvent = true;
			}
		};
		ProvUIActivator.getDefault().addProvisioningListener(listener);

		availableIUViewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				ProvUIActivator.getDefault().removeProvisioningListener(listener);
			}
		});
		return availableIUViewer;
	}

	private void setTreeColumns(Tree tree) {
		tree.setHeaderVisible(true);

		for (int i = 0; i < columnConfig.length; i++) {
			TreeColumn tc = new TreeColumn(tree, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(columnConfig[i].columnTitle);
			tc.setWidth(convertHorizontalDLUsToPixels(columnConfig[i].defaultColumnWidth));
		}
	}

	Object getNewInput() {
		MetadataRepositories input = new MetadataRepositories(getProvisioningContext().getMetadataRepositories());
		input.setQueryContext(queryContext);
		input.setQueryProvider(getQueryProvider());
		return input;
	}

	/**
	 * Set the query context to be used to traverse the model in this view.
	 * If the viewer has been created and the input element honors the query
	 * context, refresh the viewer.
	 */
	public void setQueryContext(QueryContext context) {
		this.queryContext = context;
		if (viewer == null)
			return;

		Object input = viewer.getInput();
		if (input instanceof QueriedElement) {
			((QueriedElement) input).setQueryContext(context);
			viewer.refresh();
		}
	}

	public void setRepositoryRefreshFlags(int flags) {
		refreshRepoFlags = flags;
	}

	/**
	 * Set a boolean indicating whether a bold font should be used when
	 * showing filtered items.  This method does not refresh the tree or 
	 * labels, so that must be done explicitly by the caller.
	 * @param useBoldFont
	 */
	public void setUseBoldFontForFilteredItems(boolean useBoldFont) {
		if (labelProvider != null)
			labelProvider.setUseBoldFontForFilteredItems(useBoldFont);
	}

	/**
	 * Return the composite that contains the controls in this group.
	 * @return the composite
	 */
	public Composite getComposite() {
		return super.getComposite();
	}

	/**
	 * Get the viewer used to represent the available IU's
	 * @return the viewer
	 */
	public StructuredViewer getStructuredViewer() {
		return super.getStructuredViewer();
	}

	/**
	 * Get the selected IU's
	 * @return the array of selected IU's
	 */
	public IInstallableUnit[] getSelectedIUs() {
		return super.getSelectedIUs();
	}

	public Tree getTree() {
		if (viewer == null)
			return null;
		return ((TreeViewer) viewer).getTree();
	}

	/**
	 * Refresh the available view completely.
	 */
	public void refresh() {
		URL[] urls = getProvisioningContext().getMetadataRepositories();
		ProvisioningOperation op;
		if (urls == null)
			op = new RefreshMetadataRepositoriesOperation(ProvUIMessages.AvailableIUGroup_RefreshOperationLabel, refreshRepoFlags);
		else
			op = new RefreshMetadataRepositoriesOperation(ProvUIMessages.AvailableIUGroup_RefreshOperationLabel, urls);
		ProvisioningOperationRunner.schedule(op, getShell(), StatusManager.SHOW | StatusManager.LOG);
		if (viewer != null && !viewer.getControl().isDisposed())
			viewer.setInput(getNewInput());
	}

	/*
	 * Make the repository with the specified location visible in the viewer.
	 */
	void makeRepositoryVisible(final URL location) {
		// First refresh the tree so that the user sees the new repo show up...
		display.asyncExec(new Runnable() {
			public void run() {
				final TreeViewer treeViewer = filteredTree.getViewer();
				final Tree tree = treeViewer.getTree();
				IWorkbench workbench = PlatformUI.getWorkbench();
				if (workbench.isClosing())
					return;
				if (tree != null && !tree.isDisposed()) {
					treeViewer.refresh();
				}
			}
		});

		// We don't know if loading will be a fast or slow operation.
		// We do it in a job to be safe, and when it's done, we update
		// the UI.
		Job job = new Job(NLS.bind(ProvUIMessages.AvailableIUGroup_LoadingRepository, location.toExternalForm())) {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					ProvisioningUtil.loadMetadataRepository(location, null);
					return Status.OK_STATUS;
				} catch (ProvisionException e) {
					return e.getStatus();
				}
			}
		};
		job.setPriority(Job.LONG);
		job.setSystem(true);
		job.setUser(false);
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(final IJobChangeEvent event) {
				if (event.getResult().isOK())
					display.asyncExec(new Runnable() {
						public void run() {
							final TreeViewer treeViewer = filteredTree.getViewer();
							IWorkbench workbench = PlatformUI.getWorkbench();
							if (workbench.isClosing())
								return;
							// Expand only if there have been no other jobs started for other repos.
							if (event.getJob() == lastRequestedLoadJob) {
								final Tree tree = treeViewer.getTree();
								if (tree != null && !tree.isDisposed()) {
									TreeItem[] items = tree.getItems();
									for (int i = 0; i < items.length; i++) {
										if (items[i].getData() instanceof IRepositoryElement) {
											URL url = ((IRepositoryElement) items[i].getData()).getLocation();
											if (url.toExternalForm().equals(location.toExternalForm())) {
												treeViewer.expandToLevel(items[i].getData(), AbstractTreeViewer.ALL_LEVELS);
												tree.select(items[i]);
												return;
											}
										}
									}
								}
							}
						}
					});
			}
		});
		lastRequestedLoadJob = job;
		job.schedule();
	}

	public ISelectionProvider getCheckMappingSelectionProvider() {
		return selectionProvider;
	}
}
