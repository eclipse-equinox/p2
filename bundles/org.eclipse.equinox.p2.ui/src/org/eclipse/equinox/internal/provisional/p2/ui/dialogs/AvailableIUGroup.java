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
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.DeferredFetchFilteredTree;
import org.eclipse.equinox.internal.p2.ui.dialogs.StructuredIUGroup;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.model.MetadataRepositories;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.RefreshMetadataRepositoriesOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.query.QueryContext;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.*;

/**
 * An AvailableIUGroup is a reusable UI component that displays the
 * IU's available for installation.
 * 
 * @since 3.4
 */
public class AvailableIUGroup extends StructuredIUGroup {

	QueryContext queryContext;
	// We restrict the type of the filter used because PatternFilter does
	// unnecessary accesses of children that cause problems with the deferred
	// tree.
	AvailableIUPatternFilter filter;
	private IViewMenuProvider menuProvider;
	private boolean useBold = false;
	private IUDetailsLabelProvider labelProvider;
	DeferredFetchFilteredTree filteredTree;
	IUColumnConfig[] columnConfig;
	private int refreshRepoFlags = IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM;

	/**
	 * Create a group that represents the available IU's but does not use any of the
	 * view menu or filtering capabilities.
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
		this(parent, queryProvider, font, context, null, null, ProvUI.getIUColumnConfig(), null);
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
	 */
	public AvailableIUGroup(final Composite parent, IQueryProvider queryProvider, Font font, ProvisioningContext context, QueryContext queryContext, AvailableIUPatternFilter filter, IUColumnConfig[] columnConfig, IViewMenuProvider menuProvider) {
		super(parent, queryProvider, font, context);
		this.queryContext = queryContext;
		this.filter = filter;
		this.menuProvider = menuProvider;
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
		filteredTree = new DeferredFetchFilteredTree(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, filter, menuProvider, parent.getDisplay());
		final TreeViewer availableIUViewer = filteredTree.getViewer();

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
		availableIUViewer.setInput(getInput());

		final StructuredViewerProvisioningListener listener = new StructuredViewerProvisioningListener(availableIUViewer, StructuredViewerProvisioningListener.PROV_EVENT_METADATA_REPOSITORY, getQueryProvider());
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

	Object getInput() {
		MetadataRepositories input = new MetadataRepositories(getProvisioningContext().getMetadataRepositories());
		input.setQueryContext(queryContext);
		input.setQueryProvider(getQueryProvider());
		return input;
	}

	/**
	 * Set the query context to be used to traverse the model in this view.  If the viewer has
	 * already been created, reset the input with the new information.
	 * @param context the query context
	 */
	public void setQueryContext(QueryContext context) {
		this.queryContext = context;
		if (getStructuredViewer() != null)
			getStructuredViewer().setInput(getInput());
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
		if (getStructuredViewer() == null)
			return null;
		return ((TreeViewer) getStructuredViewer()).getTree();
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
		ProvisioningOperationRunner.schedule(op, getShell());
		// Calling this will create a new input and refresh the viewer
		setQueryContext(queryContext);
	}
}
