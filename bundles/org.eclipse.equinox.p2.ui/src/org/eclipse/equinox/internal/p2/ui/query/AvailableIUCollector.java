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
package org.eclipse.equinox.internal.p2.ui.query;

import java.util.Iterator;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;

/**
 * Collector that examines available IU's and wraps them in an
 * element representing either a category an IU.
 *  
 * @since 3.4
 */
public class AvailableIUCollector extends QueriedElementCollector {

	private boolean makeCategories;
	private IProfile profile;
	private boolean hideInstalledIUs = false;
	private boolean drillDownChild = false;

	public AvailableIUCollector(IQueryable queryable, Object parent, boolean makeCategories, boolean makeDrillDownChild) {
		super(queryable, parent);
		this.makeCategories = makeCategories;
		this.drillDownChild = makeDrillDownChild;
	}

	public void markInstalledIUs(IProfile targetProfile, boolean hideInstalled) {
		this.profile = targetProfile;
		hideInstalledIUs = hideInstalled;
	}

	/**
	 * Accepts a result that matches the query criteria.
	 * 
	 * @param match an object matching the query
	 * @return <code>true</code> if the query should continue,
	 * or <code>false</code> to indicate the query should stop.
	 */
	public boolean accept(Object match) {
		IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(match, IInstallableUnit.class);
		boolean isUpdate = false;
		boolean isInstalled = false;
		if (profile != null && iu != null) {
			Collector collector = profile.query(new InstallableUnitQuery(iu.getId()), new Collector(), null);
			Iterator iter = collector.iterator();
			// We are typically iterating over only one IU unless it's a non-singleton.
			while (iter.hasNext()) {
				IInstallableUnit installed = (IInstallableUnit) iter.next();
				if (installed.getVersion().compareTo(iu.getVersion()) < 0)
					isUpdate = true;
				else {
					isUpdate = false;
					isInstalled = true;
				}
			}
		}
		// if we are hiding, hide anything that is the same iu or older
		if (hideInstalledIUs && isInstalled && !isUpdate) {
			return true;
		}

		// subclass already made this an element, just set the install flag
		if (match instanceof AvailableIUElement) {
			AvailableIUElement element = (AvailableIUElement) match;
			element.setIsInstalled(isInstalled);
			element.setIsUpdate(isUpdate);
			return super.accept(match);
		}
		// If it's not an IU or element, we have nothing to do here
		if (!(match instanceof IInstallableUnit))
			return super.accept(match);

		// We need to make an element
		if (makeCategories && isCategory(iu))
			return super.accept(new CategoryElement(parent, iu));

		IIUElement element = makeDefaultElement(iu);
		if (element instanceof AvailableIUElement) {
			AvailableIUElement availableElement = (AvailableIUElement) element;
			availableElement.setIsInstalled(isInstalled);
			availableElement.setIsUpdate(isUpdate);
		}
		return super.accept(element);
	}

	protected IIUElement makeDefaultElement(IInstallableUnit iu) {
		if (parent instanceof AvailableIUElement)
			drillDownChild = ((AvailableIUElement) parent).shouldShowChildren();
		return new AvailableIUElement(parent, iu, null, drillDownChild);
	}

	protected boolean isCategory(IInstallableUnit iu) {
		String isCategory = iu.getProperty(IInstallableUnit.PROP_TYPE_CATEGORY);
		return isCategory != null && Boolean.valueOf(isCategory).booleanValue();
	}

	protected boolean makeCategory() {
		return makeCategories;
	}
}
