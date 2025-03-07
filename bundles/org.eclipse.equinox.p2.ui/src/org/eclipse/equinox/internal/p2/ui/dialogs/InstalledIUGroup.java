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

import java.util.*;
import org.eclipse.e4.ui.dialogs.filteredtree.FilteredTree;
import org.eclipse.e4.ui.dialogs.filteredtree.PatternFilter;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIProvisioningListener;
import org.eclipse.equinox.internal.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.internal.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.*;

/**
 * An InstalledIUGroup is a reusable UI component that displays the IU's in a
 * given profile.
 *
 * @since 3.4
 */
public class InstalledIUGroup extends StructuredIUGroup {

	private String profileId;

	/**
	 * Create a group that represents the installed IU's.
	 *
	 * @param parent       the parent composite for the group
	 * @param font         The font to use for calculating pixel sizes. This font is
	 *                     not managed by the receiver.
	 * @param profileId    the id of the profile whose content is being shown.
	 * @param columnConfig the columns to be shown
	 */
	public InstalledIUGroup(ProvisioningUI ui, final Composite parent, Font font, String profileId,
			IUColumnConfig[] columnConfig) {
		super(ui, parent, font, columnConfig);
		if (profileId == null) {
			this.profileId = ui.getProfileId();
		} else {
			this.profileId = profileId;
		}
		createGroupComposite(parent);
	}

	@Override
	protected StructuredViewer createViewer(Composite parent) {
		// Table of installed IU's
		FilteredTree filteredTree = new FilteredTree(parent,
				SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, new PatternFilter());
		filteredTree.getFilterControl().setFocus(); // Steal focus, consistent with
													// org.eclipse.ui.internal.about.AboutPluginsPage
		TreeViewer installedIUViewer = filteredTree.getViewer();

		// Filters and sorters before establishing content, so we don't refresh
		// unnecessarily.
		IUComparator comparator = new IUComparator(IUComparator.IU_NAME);
		comparator.useColumnConfig(getColumnConfig());
		installedIUViewer.setComparator(comparator);
		installedIUViewer.setComparer(new ProvElementComparer());

		// Now the content.
		// Specialize the content provider for performance optimization.
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=550265
		installedIUViewer.setContentProvider(new DeferredQueryContentProvider() {
			/**
			 * A cache for mapping each element to its children.
			 */
			private final Map<Object, Object[]> elementChildren = new HashMap<>();

			/**
			 * A cache for mapping each IU to that IU's children.
			 */
			private final Map<IInstallableUnit, Set<IInstallableUnit>> iuChildren = new HashMap<>();

			@Override
			public boolean hasChildren(Object element) {
				// Delegate to getChildren so that all children are always cached.
				return getChildren(element).length != 0;
			}

			@Override
			public Object[] getChildren(Object element) {
				Object[] objects = elementChildren.get(element);
				if (objects == null) {
					if (element instanceof InstalledIUElement) {
						// Special case handling.
						InstalledIUElement installedIUElement = (InstalledIUElement) element;
						if (!installedIUElement.shouldShowChildren()) {
							// If the element shouldn't show children because that would create a cycle,
							// don't show any.
							objects = new Object[0];
							elementChildren.put(element, objects);
							return objects;
						}

						// If the children for this IU have already been computed.
						String localProfileId = installedIUElement.getProfileId();
						IInstallableUnit iu = installedIUElement.getIU();
						Set<IInstallableUnit> children = iuChildren.get(iu);
						if (children != null) {
							// Bypass the expensive query computation because we already know the IU
							// children from a previous expensive computation.
							Set<InstalledIUElement> elementIUChildren = new LinkedHashSet<>();
							for (IInstallableUnit childIU : children) {
								elementIUChildren.add(new InstalledIUElement(element, localProfileId, childIU));
							}

							objects = elementIUChildren.toArray();
							elementChildren.put(element, objects);
							return objects;
						}
					}

					// Get the children the normal ways and cache them.
					objects = super.getChildren(element);
					elementChildren.put(element, objects);

					// If there are children and this is an InstalledIUElement, cache the mapping
					// from the IU to the child IUs for reuse above.
					if (objects.length != 0 && element instanceof InstalledIUElement) {
						InstalledIUElement installedIUElement = (InstalledIUElement) element;
						IInstallableUnit iu = installedIUElement.getIU();
						Set<IInstallableUnit> children = new LinkedHashSet<>();
						for (Object object : objects) {
							if (object instanceof InstalledIUElement) {
								InstalledIUElement childInstalledIUElement = (InstalledIUElement) object;
								IInstallableUnit childIU = childInstalledIUElement.getIU();
								children.add(childIU);
							} else {
								// In case all the children are not also InstalledIUElement, which shouldn't
								// happen.
								return objects;
							}
						}
						iuChildren.put(iu, children);
					}
				}
				return objects;
			}
		});

		// Now the visuals, columns before labels.
		setTreeColumns(installedIUViewer.getTree());
		installedIUViewer.setLabelProvider(new IUDetailsLabelProvider(null, getColumnConfig(), null));

		// Input last.
		installedIUViewer.setInput(getInput());

		final StructuredViewerProvisioningListener listener = new StructuredViewerProvisioningListener(
				getClass().getName(), installedIUViewer,
				ProvUIProvisioningListener.PROV_EVENT_IU | ProvUIProvisioningListener.PROV_EVENT_PROFILE,
				getProvisioningUI().getOperationRunner());
		ProvUI.getProvisioningEventBus(getProvisioningUI().getSession()).addListener(listener);
		installedIUViewer.getControl().addDisposeListener(
				e -> ProvUI.getProvisioningEventBus(getProvisioningUI().getSession()).removeListener(listener));
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
