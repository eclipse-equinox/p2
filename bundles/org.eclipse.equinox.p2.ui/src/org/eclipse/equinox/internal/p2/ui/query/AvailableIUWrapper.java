/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *     Rapicorp, Inc (Pascal Rapicault) - Bug 394156 - Add support for updates from one namespace to another
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.query;

import java.util.Iterator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;

/**
 * A wrapper that examines available IU's and wraps them in an
 * element representing either a category or a regular IU.
 *  
 * @since 3.4
 */
public class AvailableIUWrapper extends QueriedElementWrapper {

	private boolean makeCategories;
	private IProfile profile;
	private boolean hideInstalledIUs = false;
	private boolean drillDownChild = false;

	public AvailableIUWrapper(IQueryable<?> queryable, Object parent, boolean makeCategories, boolean makeDrillDownChild) {
		super(queryable, parent);
		this.makeCategories = makeCategories;
		this.drillDownChild = makeDrillDownChild;
	}

	public void markInstalledIUs(IProfile targetProfile, boolean hideInstalled) {
		this.profile = targetProfile;
		hideInstalledIUs = hideInstalled;
	}

	class InformationCache {
		Object item = null;
		boolean isUpdate = false;
		boolean isInstalled = false;
		boolean isPatch = false;

		public InformationCache(Object item, boolean isUpdate, boolean isInstalled, boolean isPatch) {
			this.item = item;
			this.isUpdate = isUpdate;
			this.isInstalled = isInstalled;
			this.isPatch = isPatch;
		}
	}

	InformationCache cache = null;

	protected boolean shouldWrap(Object match) {
		IInstallableUnit iu = ProvUI.getAdapter(match, IInstallableUnit.class);
		cache = computeIUInformation(iu); // Cache the result

		// if we are hiding, hide anything that is the same iu or older
		if (hideInstalledIUs && cache.isInstalled && !cache.isUpdate) {
			emptyExplanationString = ProvUIMessages.AvailableIUWrapper_AllAreInstalled;
			emptyExplanationSeverity = IStatus.INFO;
			emptyExplanationDescription = ProvUIMessages.IUViewQueryContext_AllAreInstalledDescription;
			return false;
		}
		return true;
	}

	/**
	 * Compute information about this IU. This computes whether or
	 * not this IU is installed and / or updated.
	 */
	private InformationCache computeIUInformation(IInstallableUnit iu) {
		boolean isUpdate = false;
		boolean isInstalled = false;
		boolean isPatch = iu == null ? false : QueryUtil.isPatch(iu);
		if (profile != null && iu != null) {
			isInstalled = !profile.query(QueryUtil.createIUQuery(iu), null).isEmpty();
			Iterator<IInstallableUnit> iter = profile.query(new UserVisibleRootQuery(), null).iterator();
			while (iter.hasNext()) {
				IInstallableUnit installed = iter.next();
				if (iu.getUpdateDescriptor() != null && iu.getUpdateDescriptor().isUpdateOf(installed) && (!iu.getId().equals(installed.getId()) || installed.getVersion().compareTo(iu.getVersion()) < 0)) {
					isUpdate = true;
					break;
				}
			}
		}
		return new InformationCache(iu, isUpdate, isInstalled, isPatch);
	}

	protected Object wrap(Object item) {
		IInstallableUnit iu = ProvUI.getAdapter(item, IInstallableUnit.class);
		boolean isUpdate = false;
		boolean isInstalled = false;
		boolean isPatch = false;
		if (cache != null && cache.item == item) {
			// This cache should always be valide, since accept is called before transformItem
			isUpdate = cache.isUpdate;
			isInstalled = cache.isInstalled;
			isPatch = cache.isPatch;
		} else {
			InformationCache iuInformation = computeIUInformation(iu);
			isUpdate = iuInformation.isUpdate;
			isInstalled = iuInformation.isInstalled;
			isPatch = iuInformation.isPatch;
		}
		// subclass already made this an element, just set the install flag
		if (item instanceof AvailableIUElement) {
			AvailableIUElement element = (AvailableIUElement) item;
			element.setIsInstalled(isInstalled);
			element.setIsUpdate(isUpdate);
			element.setIsPatch(isPatch);
			return super.wrap(item);
		}
		// If it's not an IU or element, we have nothing to do here
		if (!(item instanceof IInstallableUnit))
			return super.wrap(item);

		// We need to make an element
		if (makeCategories && isCategory(iu))
			return super.wrap(new CategoryElement(parent, iu));

		IIUElement element = makeDefaultElement(iu);
		if (element instanceof AvailableIUElement) {
			AvailableIUElement availableElement = (AvailableIUElement) element;
			availableElement.setIsInstalled(isInstalled);
			availableElement.setIsUpdate(isUpdate);
			availableElement.setIsPatch(isPatch);
		}
		return super.wrap(element);
	}

	protected IIUElement makeDefaultElement(IInstallableUnit iu) {
		if (parent instanceof AvailableIUElement)
			drillDownChild = ((AvailableIUElement) parent).shouldShowChildren();
		return new AvailableIUElement(parent, iu, null, drillDownChild);
	}

	protected boolean isCategory(IInstallableUnit iu) {
		return QueryUtil.isCategory(iu);
	}

	protected boolean makeCategory() {
		return makeCategories;
	}
}
