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

import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.dialogs.StructuredIUGroup;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.model.MetadataRepositories;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
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
	 */
	public AvailableIUGroup(final Composite parent, IQueryProvider queryProvider, Font font, ProvisioningContext context) {
		super(parent, queryProvider, font, context);
		this.createGroupComposite(parent);
	}

	protected StructuredViewer createViewer(Composite parent) {
		// Table of available IU's
		final TreeViewer availableIUViewer = new TreeViewer(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);

		final IUDetailsLabelProvider labelProvider = new IUDetailsLabelProvider();
		labelProvider.setToolTipProperty(IInstallableUnit.PROP_DESCRIPTION);

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		availableIUViewer.setComparator(new IUComparator(IUComparator.IU_ID));
		availableIUViewer.setComparer(new ProvElementComparer());

		// Now the content.
		availableIUViewer.setContentProvider(new DeferredQueryContentProvider(getQueryProvider()));
		availableIUViewer.setInput(getInput());

		// Now the presentation, columns before label provider.
		setTreeColumns(availableIUViewer.getTree());
		availableIUViewer.setLabelProvider(labelProvider);

		final StructuredViewerProvisioningListener listener = new StructuredViewerProvisioningListener(availableIUViewer, StructuredViewerProvisioningListener.PROV_EVENT_REPOSITORY, getQueryProvider()) {
			protected void refreshAll() {
				AvailableIUGroup.this.refreshAll();
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
		IUColumnConfig[] columns = ProvUI.getIUColumnConfig();
		tree.setHeaderVisible(true);

		for (int i = 0; i < columns.length; i++) {
			TreeColumn tc = new TreeColumn(tree, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(columns[i].columnTitle);
			tc.setWidth(convertHorizontalDLUsToPixels(columns[i].defaultColumnWidth));
		}
	}

	Object getInput() {
		MetadataRepositories input = new MetadataRepositories(getProvisioningContext().getMetadataRepositories());
		input.setQueryType(IQueryProvider.AVAILABLE_IUS);
		return input;
	}

	public void refreshAll() {
		// The content provider caches the children unless input changes,
		// so a viewer.refresh() is not enough.
		getStructuredViewer().setInput(getInput());
	}
}
