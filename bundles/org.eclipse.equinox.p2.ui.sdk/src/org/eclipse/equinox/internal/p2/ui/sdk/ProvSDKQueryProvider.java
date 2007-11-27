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
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.query.CapabilityQuery;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.model.*;
import org.eclipse.equinox.p2.ui.query.*;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Provides the queries appropriate for the SDK UI.
 * 
 * @since 3.4
 */

public class ProvSDKQueryProvider implements IProvElementQueryProvider {

	public ElementQueryDescriptor getQueryDescriptor(QueriedElement element, int queryType) {
		IQueryable queryable;
		switch (queryType) {
			case IProvElementQueryProvider.ARTIFACT_REPOS :
				queryable = new QueryableArtifactRepositoryManager();
				return new ElementQueryDescriptor(queryable, new RepositoryPropertyQuery(IRepository.IMPLEMENTATION_ONLY_KEY, Boolean.toString(true), false), new QueriedElementCollector(this, queryable));
			case IProvElementQueryProvider.AVAILABLE_IUS :
				CapabilityQuery groupQuery = new CapabilityQuery(new RequiredCapability(IInstallableUnit.NAMESPACE_IU_KIND, "group", null, null, false, false)); //$NON-NLS-1$
				Query categoryQuery = new IUPropertyQuery(IInstallableUnit.PROP_CATEGORY_IU, Boolean.toString(true));
				if (element instanceof MetadataRepositoryElement) {
					return new ElementQueryDescriptor(element.getQueryable(), categoryQuery, new CategoryElementCollector(this, element.getQueryable(), false));
				}
				if (element instanceof CategoryElement) {
					Query membersOfCategoryQuery;
					if (element instanceof UncategorizedCategoryElement)
						membersOfCategoryQuery = new Query() {
							public boolean isMatch(Object candidate) {
								return true;
							}
						};
					else
						membersOfCategoryQuery = new AnyRequiredCapabilityQuery(((CategoryElement) element).getIU());
					IPreferenceStore store = ProvSDKUIActivator.getDefault().getPreferenceStore();
					Collector collector;
					if (store.getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION))
						collector = new LatestIUVersionCollector(this, element.getQueryable());
					else
						collector = new AvailableIUCollector(this, element.getQueryable());
					return new ElementQueryDescriptor(element.getQueryable(), new CompoundQuery(new Query[] {membersOfCategoryQuery, new CompoundQuery(new Query[] {groupQuery, categoryQuery}, false)}, true), collector);
				}
				// If we are showing only the latest version, we never represent other versions as children.
				if (element instanceof IUVersionsElement) {
					return null;
				}
			case IProvElementQueryProvider.AVAILABLE_UPDATES :
			case IProvElementQueryProvider.INSTALLED_IUS :
				Profile profile = (Profile) ProvUI.getAdapter(element, Profile.class);
				return new ElementQueryDescriptor(profile, new IUProfilePropertyQuery(profile, IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true)), new InstalledIUCollector(this, profile));
			case IProvElementQueryProvider.METADATA_REPOS :
				queryable = new QueryableMetadataRepositoryManager();
				return new ElementQueryDescriptor(queryable, new RepositoryPropertyQuery(IRepository.IMPLEMENTATION_ONLY_KEY, Boolean.toString(true), false), new QueriedElementCollector(this, queryable));
			case IProvElementQueryProvider.PROFILES :
				queryable = new QueryableProfileRegistry();
				return new ElementQueryDescriptor(queryable, new Query() {
					public boolean isMatch(Object candidate) {
						return ProvUI.getAdapter(candidate, Profile.class) != null;
					}
				}, new QueriedElementCollector(this, queryable));
			default :
				return null;
		}
	}
}
