/*******************************************************************************
 *  Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIProvisioningListener;
import org.eclipse.equinox.internal.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * An InstalledIUGroup is a reusable UI component that displays the
 * IU's in a given profile.
 * 
 * @since 3.4
 */
public class InstalledIUGroup extends StructuredIUGroup {

	private String profileId;

	/**
	 * Create a group that represents the installed IU's.
	 * 
	 * @param parent the parent composite for the group
	 * @param font The font to use for calculating pixel sizes.  This font is
	 * not managed by the receiver.
	 * @param profileId the id of the profile whose content is being shown.
	 * @param columnConfig the columns to be shown
	 */
	public InstalledIUGroup(ProvisioningUI ui, final Composite parent, Font font, String profileId, IUColumnConfig[] columnConfig) {
		super(ui, parent, font, columnConfig);
		if (profileId == null)
			this.profileId = ui.getProfileId();
		else
			this.profileId = profileId;
		createGroupComposite(parent);
	}

	@Override
	protected StructuredViewer createViewer(Composite parent) {
		// Table of installed IU's
		FilteredTree filteredTree = new FilteredTree(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, new PatternFilter(), true);
		filteredTree.getFilterControl().setFocus(); //Steal focus, consistent with org.eclipse.ui.internal.about.AboutPluginsPage
		TreeViewer installedIUViewer = filteredTree.getViewer();

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		IUComparator comparator = new IUComparator(IUComparator.IU_NAME);
		comparator.useColumnConfig(getColumnConfig());
		installedIUViewer.setComparator(comparator);
		installedIUViewer.setComparer(new ProvElementComparer());

		// Now the content.
		installedIUViewer.setContentProvider(new DeferredQueryContentProvider());

		// Now the visuals, columns before labels.
		setTreeColumns(installedIUViewer.getTree());
		installedIUViewer.setLabelProvider(new IUDetailsLabelProvider(null, getColumnConfig(), null));

		// Input last.
		installedIUViewer.setInput(getInput());

		final StructuredViewerProvisioningListener listener = new StructuredViewerProvisioningListener(getClass().getName(), installedIUViewer, ProvUIProvisioningListener.PROV_EVENT_IU | ProvUIProvisioningListener.PROV_EVENT_PROFILE, getProvisioningUI().getOperationRunner());
		ProvUI.getProvisioningEventBus(getProvisioningUI().getSession()).addListener(listener);
		installedIUViewer.getControl().addDisposeListener(e -> ProvUI.getProvisioningEventBus(getProvisioningUI().getSession()).removeListener(listener));
		return installedIUViewer;
	}

	private void setTreeColumns(Tree tree) {
		IUColumnConfig[] columns = getColumnConfig();
		tree.setHeaderVisible(true);

		for (int i = 0; i < columns.length; i++) {
			TreeColumn tc = new TreeColumn(tree, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(columns[i].getColumnTitle());
			tc.setWidth(columns[i].getWidthInPixels(tree));
		}
	}

	Object getInput() {
		ProfileElement element = new ProfileElement(null, profileId);
		return element;
	}

	/**
	 * Get the viewer used to represent the installed IU's
	 */
	@Override
	public StructuredViewer getStructuredViewer() {
		return super.getStructuredViewer();
	}

	@Override
	public Control getDefaultFocusControl() {
		return super.getDefaultFocusControl();
	}
}
