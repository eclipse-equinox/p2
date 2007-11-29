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
package org.eclipse.equinox.internal.p2.ui.admin;

import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.query.CapabilityQuery;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.admin.ProvAdminUIActivator;
import org.eclipse.equinox.p2.ui.model.*;
import org.eclipse.equinox.p2.ui.query.*;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Provides the queries appropriate for the SDK UI.
 * 
 * @since 3.4
 */

public class ProvAdminQueryProvider implements IProvElementQueryProvider {

	private Query allQuery = new Query() {
		public boolean isMatch(Object candidate) {
			return true;
		}

	};

	public ElementQueryDescriptor getQueryDescriptor(QueriedElement element, int queryType) {
		IQueryable queryable;
		IPreferenceStore store = ProvAdminUIActivator.getDefault().getPreferenceStore();
		boolean showGroupsOnly = store.getBoolean(PreferenceConstants.PREF_SHOW_GROUPS_ONLY);
		boolean hideImpl = store.getBoolean(PreferenceConstants.PREF_HIDE_IMPLEMENTATION_REPOS);
		boolean showLatest = store.getBoolean(PreferenceConstants.PREF_COLLAPSE_IU_VERSIONS);
		boolean useCategories = store.getBoolean(PreferenceConstants.PREF_USE_CATEGORIES);
		boolean showRootsOnly = store.getBoolean(PreferenceConstants.PREF_SHOW_INSTALL_ROOTS_ONLY);
		Query groupQuery = new CapabilityQuery(new RequiredCapability(IInstallableUnit.NAMESPACE_IU_KIND, "group", null, null, false, false)); //$NON-NLS-1$
		Query categoryQuery = new IUPropertyQuery(IInstallableUnit.PROP_CATEGORY_IU, Boolean.toString(true));
		Query query;
		Profile profile;
		switch (queryType) {
			case IProvElementQueryProvider.ARTIFACT_REPOS :
				queryable = new QueryableArtifactRepositoryManager();
				query = hideImpl ? new RepositoryPropertyQuery(IRepository.IMPLEMENTATION_ONLY_KEY, Boolean.toString(true), false) : allQuery;
				return new ElementQueryDescriptor(queryable, query, new QueriedElementCollector(this, queryable));
			case IProvElementQueryProvider.AVAILABLE_IUS :
				// What should we show as a child of a repository?
				if (element instanceof MetadataRepositoryElement) {
					if (useCategories)
						// We are using categories, group into categories first.
						return new ElementQueryDescriptor(element.getQueryable(), categoryQuery, new CategoryElementCollector(this, element.getQueryable(), false));
					if (showGroupsOnly)
						// Query all groups and use the query result to optionally select the latest version only
						return new ElementQueryDescriptor(element.getQueryable(), groupQuery, showLatest ? new LatestIUVersionCollector(this, element.getQueryable(), false) : new AvailableIUCollector(this, element.getQueryable(), false));
					if (showLatest)
						// We are not querying groups, but we are showing the latest version only
						return new ElementQueryDescriptor(element.getQueryable(), allQuery, new LatestIUVersionCollector(this, element.getQueryable(), false));
					// Show 'em all!
					return new ElementQueryDescriptor(element.getQueryable(), allQuery, new AvailableIUCollector(this, element.getQueryable(), false));
				}
				// Things have been grouped by category, now what?
				if (element instanceof CategoryElement) {
					Query membersOfCategoryQuery;
					if (element instanceof UncategorizedCategoryElement)
						membersOfCategoryQuery = allQuery;
					else
						membersOfCategoryQuery = new AnyRequiredCapabilityQuery(((CategoryElement) element).getIU());

					if (showGroupsOnly)
						// Query all groups and use the query result to optionally select the latest version only
						return new ElementQueryDescriptor(element.getQueryable(), new CompoundQuery(new Query[] {new CompoundQuery(new Query[] {groupQuery, categoryQuery}, false), membersOfCategoryQuery}, true), showLatest ? new LatestIUVersionCollector(this, element.getQueryable(), true) : new AvailableIUCollector(this, element.getQueryable(), true));
					if (showLatest)
						// We are not querying groups, but we are showing the latest version only
						return new ElementQueryDescriptor(element.getQueryable(), membersOfCategoryQuery, new LatestIUVersionCollector(this, element.getQueryable(), true));
					// Show 'em all!
					return new ElementQueryDescriptor(element.getQueryable(), membersOfCategoryQuery, new AvailableIUCollector(this, element.getQueryable(), true));
				}
				// We've already collapsed all versions, show the rest
				if (element instanceof IUVersionsElement) {
					IInstallableUnit iu = ((IUVersionsElement) element).getIU();
					return new ElementQueryDescriptor(element.getQueryable(), new InstallableUnitQuery(iu.getId()), new OtherIUVersionsCollector(iu, this, element.getQueryable()));
				}
			case IProvElementQueryProvider.AVAILABLE_UPDATES :
				profile = (Profile) ProvUI.getAdapter(element, Profile.class);
				return new ElementQueryDescriptor(profile, new IUProfilePropertyQuery(profile, IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true)), new InstalledIUCollector(this, profile));

			case IProvElementQueryProvider.INSTALLED_IUS :
				profile = (Profile) ProvUI.getAdapter(element, Profile.class);
				if (showRootsOnly)
					query = new IUProfilePropertyQuery(profile, IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
				else
					query = allQuery;
				if (showGroupsOnly)
					new ElementQueryDescriptor(profile, new CompoundQuery(new Query[] {query, groupQuery}, true), new InstalledIUCollector(this, profile));
				return new ElementQueryDescriptor(profile, query, new InstalledIUCollector(this, profile));
			case IProvElementQueryProvider.METADATA_REPOS :
				queryable = new QueryableMetadataRepositoryManager();
				query = hideImpl ? new RepositoryPropertyQuery(IRepository.IMPLEMENTATION_ONLY_KEY, Boolean.toString(true), false) : allQuery;
				return new ElementQueryDescriptor(queryable, query, new QueriedElementCollector(this, queryable));
			case IProvElementQueryProvider.PROFILES :
				queryable = new QueryableProfileRegistry();
				return new ElementQueryDescriptor(queryable, new Query() {
					public boolean isMatch(Object candidate) {
						return ProvUI.getAdapter(candidate, Profile.class) != null;
					}
				}, new ProfileElementCollector(this, null));
			default :
				return null;
		}
	}
}
