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
package org.eclipse.equinox.internal.provisional.p2.ui.query;

import java.util.Collection;
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.model.CategoryElement;
import org.eclipse.equinox.internal.provisional.p2.ui.model.QueriedElementCollector;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;

/**
 * Collector that examines available IU's and wraps them in an
 * element representing either a category an IU.
 *  
 * @since 3.4
 */
public class AvailableIUCollector extends QueriedElementCollector {

	private boolean makeCategories;
	private ElementQueryDescriptor installedQueryDescriptor;
	private boolean hideInstalledIUs = false;
	Collection installedElements = null;

	public AvailableIUCollector(IQueryProvider queryProvider, IQueryable queryable, QueryContext queryContext, boolean makeCategories) {
		super(queryProvider, queryable, queryContext);
		this.makeCategories = makeCategories;
	}

	public void hideInstalledIUs(ElementQueryDescriptor installedQuery) {
		installedQueryDescriptor = installedQuery;
		hideInstalledIUs = true;
	}

	/**
	 * Accepts a result that matches the query criteria.
	 * 
	 * @param match an object matching the query
	 * @return <code>true</code> if the query should continue,
	 * or <code>false</code> to indicate the query should stop.
	 */
	public boolean accept(Object match) {
		if (hideInstalledIUs && installedElements == null)
			initializeInstalledElements();
		if (hideInstalledIUs) {
			IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(match, IInstallableUnit.class);
			if (installedElements.contains(iu))
				return true;
		}
		// If it's not an IU, it's an element and work is done
		if (!(match instanceof IInstallableUnit))
			return super.accept(match);

		// We need to make an element
		IInstallableUnit iu = (IInstallableUnit) match;
		if (makeCategories && isCategory(iu))
			return super.accept(new CategoryElement(iu));
		return super.accept(makeDefaultElement(iu));
	}

	private void initializeInstalledElements() {
		installedElements = installedQueryDescriptor.queryable.query(installedQueryDescriptor.query, installedQueryDescriptor.collector, null).toCollection();
	}

	protected Object makeDefaultElement(IInstallableUnit iu) {
		return new AvailableIUElement(iu, null);
	}

	protected boolean isCategory(IInstallableUnit iu) {
		String isCategory = iu.getProperty(IInstallableUnit.PROP_TYPE_CATEGORY);
		return isCategory != null && Boolean.valueOf(isCategory).booleanValue();
	}

	protected boolean makeCategory() {
		return makeCategories;
	}
}
