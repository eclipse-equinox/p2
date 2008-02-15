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
package org.eclipse.equinox.internal.p2.ui.admin;

import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.CapabilityQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.*;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.model.*;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.query.*;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.UpdateEvent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Provides the queries appropriate for the SDK UI.
 * 
 * @since 3.4
 */

public class ProvAdminQueryProvider implements IQueryProvider {

	private Query allQuery = new Query() {
		public boolean isMatch(Object candidate) {
			return true;
		}

	};

	public ElementQueryDescriptor getQueryDescriptor(Object element, int queryType) {
		IQueryable queryable;
		IPreferenceStore store = ProvAdminUIActivator.getDefault().getPreferenceStore();
		boolean showGroupsOnly = store.getBoolean(PreferenceConstants.PREF_SHOW_GROUPS_ONLY);
		boolean hideSystem = store.getBoolean(PreferenceConstants.PREF_HIDE_SYSTEM_REPOS);
		boolean showLatest = store.getBoolean(PreferenceConstants.PREF_COLLAPSE_IU_VERSIONS);
		boolean useCategories = store.getBoolean(PreferenceConstants.PREF_USE_CATEGORIES);
		boolean showRootsOnly = store.getBoolean(PreferenceConstants.PREF_SHOW_INSTALL_ROOTS_ONLY);
		Query groupQuery = new CapabilityQuery(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_KIND, "group", null, null, false, false)); //$NON-NLS-1$
		Query categoryQuery = new IUPropertyQuery(IInstallableUnit.PROP_CATEGORY_IU, Boolean.toString(true));
		Query query;
		IProfile profile;
		switch (queryType) {
			case IQueryProvider.ARTIFACT_REPOS :
				queryable = new QueryableArtifactRepositoryManager();
				query = hideSystem ? new FilteredRepositoryQuery(IArtifactRepositoryManager.REPOSITORIES_NON_SYSTEM) : allQuery;
				return new ElementQueryDescriptor(queryable, query, new QueriedElementCollector(this, queryable));
			case IQueryProvider.AVAILABLE_IUS :
				// Is it a rollback repository?
				if (element instanceof RollbackRepositoryElement) {
					Query profileQuery = new InstallableUnitQuery(((RollbackRepositoryElement) element).getProfileId());
					return new ElementQueryDescriptor(((RollbackRepositoryElement) element).getQueryable(), profileQuery, new AvailableIUCollector(this, ((RollbackRepositoryElement) element).getQueryable(), false));
				}
				// It is a regular repository.
				// What should we show as a child of a repository?
				if (element instanceof MetadataRepositoryElement) {
					if (useCategories)
						// We are using categories, group into categories first.
						return new ElementQueryDescriptor(((MetadataRepositoryElement) element).getQueryable(), categoryQuery, new CategoryElementCollector(this, ((MetadataRepositoryElement) element).getQueryable(), false));
					if (showGroupsOnly)
						// Query all groups and use the query result to optionally select the latest version only
						return new ElementQueryDescriptor(((MetadataRepositoryElement) element).getQueryable(), groupQuery, showLatest ? new LatestIUVersionElementCollector(this, ((MetadataRepositoryElement) element).getQueryable(), false) : new AvailableIUCollector(this, ((MetadataRepositoryElement) element).getQueryable(), false));
					if (showLatest)
						// We are not querying groups, but we are showing the latest version only
						return new ElementQueryDescriptor(((MetadataRepositoryElement) element).getQueryable(), allQuery, new LatestIUVersionElementCollector(this, ((MetadataRepositoryElement) element).getQueryable(), false));
					// Show 'em all!
					return new ElementQueryDescriptor(((MetadataRepositoryElement) element).getQueryable(), allQuery, new AvailableIUCollector(this, ((MetadataRepositoryElement) element).getQueryable(), false));
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
						return new ElementQueryDescriptor(((CategoryElement) element).getQueryable(), new CompoundQuery(new Query[] {new CompoundQuery(new Query[] {groupQuery, categoryQuery}, false), membersOfCategoryQuery}, true), showLatest ? new LatestIUVersionElementCollector(this, ((CategoryElement) element).getQueryable(), true) : new AvailableIUCollector(this, ((CategoryElement) element).getQueryable(), true));
					if (showLatest)
						// We are not querying groups, but we are showing the latest version only
						return new ElementQueryDescriptor(((CategoryElement) element).getQueryable(), membersOfCategoryQuery, new LatestIUVersionElementCollector(this, ((CategoryElement) element).getQueryable(), true));
					// Show 'em all!
					return new ElementQueryDescriptor(((CategoryElement) element).getQueryable(), membersOfCategoryQuery, new AvailableIUCollector(this, ((CategoryElement) element).getQueryable(), true));
				}
				// We've already collapsed all versions, show the rest
				if (element instanceof IUVersionsElement) {
					IInstallableUnit iu = ((IUVersionsElement) element).getIU();
					return new ElementQueryDescriptor(((IUVersionsElement) element).getQueryable(), new InstallableUnitQuery(iu.getId()), new OtherIUVersionsCollector(iu, this, ((IUVersionsElement) element).getQueryable()));
				}
			case IQueryProvider.AVAILABLE_UPDATES :
				profile = (IProfile) ProvUI.getAdapter(element, IProfile.class);
				IInstallableUnit[] toUpdate;
				Collector collector;
				if (profile != null) {
					collector = profile.query(new IUProfilePropertyQuery(profile, IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true)), new Collector(), null);
					toUpdate = (IInstallableUnit[]) collector.toArray(IInstallableUnit.class);
				} else if (element instanceof UpdateEvent) {
					try {
						profile = ProvisioningUtil.getProfile(((UpdateEvent) element).getProfileId());
					} catch (ProvisionException e) {
						ProvUI.handleException(e, ProvAdminUIMessages.ProvAdminQueryProvider_UpdateQueryError, StatusManager.LOG);
						return null;
					}
					toUpdate = ((UpdateEvent) element).getIUs();
				} else
					return null;
				QueryableUpdates updateQueryable = new QueryableUpdates(toUpdate);
				if (showLatest)
					collector = new LatestIUVersionCollector(this, updateQueryable, useCategories);
				else
					collector = new Collector();
				return new ElementQueryDescriptor(updateQueryable, allQuery, collector);
			case IQueryProvider.INSTALLED_IUS :
				profile = (IProfile) ProvUI.getAdapter(element, IProfile.class);
				if (showRootsOnly)
					query = new IUProfilePropertyQuery(profile, IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
				else
					query = allQuery;
				if (showGroupsOnly)
					return new ElementQueryDescriptor(profile, new CompoundQuery(new Query[] {groupQuery, query}, true), new InstalledIUCollector(this, profile));
				return new ElementQueryDescriptor(profile, query, new InstalledIUCollector(this, profile));
			case IQueryProvider.METADATA_REPOS :
				if (element instanceof MetadataRepositories)
					queryable = new QueryableMetadataRepositoryManager(((MetadataRepositories) element).getMetadataRepositories());
				else
					queryable = new QueryableMetadataRepositoryManager();
				query = hideSystem ? new FilteredRepositoryQuery(IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM) : allQuery;
				return new ElementQueryDescriptor(queryable, query, new QueriedElementCollector(this, queryable));
			case IQueryProvider.PROFILES :
				queryable = new QueryableProfileRegistry();
				return new ElementQueryDescriptor(queryable, new Query() {
					public boolean isMatch(Object candidate) {
						return ProvUI.getAdapter(candidate, IProfile.class) != null;
					}
				}, new ProfileElementCollector(this, null));
			default :
				return null;
		}
	}
}
