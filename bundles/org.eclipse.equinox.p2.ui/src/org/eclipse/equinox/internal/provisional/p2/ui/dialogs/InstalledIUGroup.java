/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.*;

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
	 * @param queryProvider the query provider that defines the queries used
	 * to retrieve elements in the viewer.
	 * @param profileId the id of the profile whose content is being shown.
	 * @param font The font to use for calculating pixel sizes.  This font is
	 * not managed by the receiver.
	 * @param context the ProvisioningContext describing the context for provisioning,
	 * including information about which repositories should be used.

	 */
	public InstalledIUGroup(final Composite parent, IQueryProvider queryProvider, Font font, ProvisioningContext context, String profileId) {
		// This will evolve into a provisioning context
		super(parent, queryProvider, font, context);
		this.profileId = profileId;
		this.createGroupComposite(parent);
	}

	protected StructuredViewer createViewer(Composite parent) {
		// Table of installed IU's
		TableViewer installedIUViewer = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		installedIUViewer.setComparator(new IUComparator(IUComparator.IU_NAME));
		installedIUViewer.setComparer(new ProvElementComparer());

		// Now the content.
		installedIUViewer.setContentProvider(new DeferredQueryContentProvider(getQueryProvider()));

		// Now the visuals, columns before labels.
		setTableColumns(installedIUViewer.getTable());
		installedIUViewer.setLabelProvider(new IUDetailsLabelProvider());

		// Input last.
		installedIUViewer.setInput(getInput());

		final StructuredViewerProvisioningListener listener = new StructuredViewerProvisioningListener(installedIUViewer, StructuredViewerProvisioningListener.PROV_EVENT_IU | StructuredViewerProvisioningListener.PROV_EVENT_PROFILE, getQueryProvider());
		ProvUIActivator.getDefault().addProvisioningListener(listener);
		installedIUViewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				ProvUIActivator.getDefault().removeProvisioningListener(listener);
			}
		});
		return installedIUViewer;
	}

	private void setTableColumns(Table table) {
		IUColumnConfig[] columns = ProvUI.getIUColumnConfig();
		table.setHeaderVisible(true);

		for (int i = 0; i < columns.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(columns[i].columnTitle);
			tc.setWidth(convertHorizontalDLUsToPixels(columns[i].defaultColumnWidth));
		}
	}

	Object getInput() {
		ProfileElement element = new ProfileElement(profileId);
		element.setQueryProvider(getQueryProvider());
		return element;
	}
}
