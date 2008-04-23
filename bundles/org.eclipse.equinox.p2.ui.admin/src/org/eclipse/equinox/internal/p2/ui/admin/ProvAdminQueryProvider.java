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

import java.net.URL;
import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.IUProfilePropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IUPropertyQuery;
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
		QueryContext queryContext = null;
		// We don't currently use the query context to alter the query, but as soon as we do, we'll
		// want to make sure it gets passed through to the appropriate elements.  So
		// we do that now.
		if (element instanceof QueriedElement)
			queryContext = ((QueriedElement) element).getQueryContext();
		IPreferenceStore store = ProvAdminUIActivator.getDefault().getPreferenceStore();
		boolean showGroupsOnly = store.getBoolean(PreferenceConstants.PREF_SHOW_GROUPS_ONLY);
		boolean hideSystem = store.getBoolean(PreferenceConstants.PREF_HIDE_SYSTEM_REPOS);
		boolean showLatest = store.getBoolean(PreferenceConstants.PREF_COLLAPSE_IU_VERSIONS);
		boolean useCategories = store.getBoolean(PreferenceConstants.PREF_USE_CATEGORIES);
		boolean showRootsOnly = store.getBoolean(PreferenceConstants.PREF_SHOW_INSTALL_ROOTS_ONLY);
		Query groupQuery = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		Query categoryQuery = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_CATEGORY, Boolean.TRUE.toString());
		Query query;
		IProfile profile;
		switch (queryType) {
			case IQueryProvider.ARTIFACT_REPOS :
				int flags = hideSystem ? IArtifactRepositoryManager.REPOSITORIES_NON_SYSTEM : IArtifactRepositoryManager.REPOSITORIES_ALL;
				queryable = new QueryableArtifactRepositoryManager(flags);
				return new ElementQueryDescriptor(queryable, null, new Collector() {
					public boolean accept(Object object) {
						if (object instanceof URL)
							return super.accept(new ArtifactRepositoryElement((URL) object));
						return true;
					}
				});
			case IQueryProvider.AVAILABLE_IUS :
				// Is it a rollback repository?
				if (element instanceof RollbackRepositoryElement) {
					Query profileQuery = new InstallableUnitQuery(((RollbackRepositoryElement) element).getProfileId());
					return new ElementQueryDescriptor(((RollbackRepositoryElement) element).getQueryable(), profileQuery, new AvailableIUCollector(this, ((RollbackRepositoryElement) element).getQueryable(), queryContext, false));
				}
				// It is a regular repository.
				// What should we show as a child of a repository?
				if (element instanceof MetadataRepositoryElement) {
					if (useCategories)
						// We are using categories, group into categories first.
						return new ElementQueryDescriptor(((MetadataRepositoryElement) element).getQueryable(), categoryQuery, new CategoryElementCollector(this, ((MetadataRepositoryElement) element).getQueryable(), queryContext, true));
					if (showGroupsOnly)
						// Query all groups and use the query result to optionally select the latest version only
						return new ElementQueryDescriptor(((MetadataRepositoryElement) element).getQueryable(), groupQuery, showLatest ? new LatestIUVersionElementCollector(this, ((MetadataRepositoryElement) element).getQueryable(), queryContext, false) : new AvailableIUCollector(this, ((MetadataRepositoryElement) element).getQueryable(), queryContext, false));
					if (showLatest)
						// We are not querying groups, but we are showing the latest version only
						return new ElementQueryDescriptor(((MetadataRepositoryElement) element).getQueryable(), allQuery, new LatestIUVersionElementCollector(this, ((MetadataRepositoryElement) element).getQueryable(), queryContext, false));
					// Show 'em all!
					return new ElementQueryDescriptor(((MetadataRepositoryElement) element).getQueryable(), allQuery, new AvailableIUCollector(this, ((MetadataRepositoryElement) element).getQueryable(), queryContext, false));
				}
				if (element instanceof MetadataRepositories) {
					MetadataRepositories metaRepos = (MetadataRepositories) element;
					if (metaRepos.getMetadataRepositories() == null)
						metaRepos.setRepoFlags(IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM);
					queryable = new QueryableMetadataRepositoryManager(metaRepos);

					if (useCategories)
						// We are using categories, group into categories first.
						return new ElementQueryDescriptor(queryable, categoryQuery, new CategoryElementCollector(this, queryable, queryContext, true));
					if (showGroupsOnly)
						// Query all groups and use the query result to optionally select the latest version only
						return new ElementQueryDescriptor(queryable, groupQuery, showLatest ? new LatestIUVersionElementCollector(this, queryable, queryContext, false) : new AvailableIUCollector(this, queryable, queryContext, false));
					if (showLatest)
						// We are not querying groups, but we are showing the latest version only
						return new ElementQueryDescriptor(queryable, allQuery, new LatestIUVersionElementCollector(this, queryable, queryContext, false));
					// Show 'em all!
					return new ElementQueryDescriptor(queryable, allQuery, new AvailableIUCollector(this, queryable, queryContext, false));
				}
				// Things have been grouped by category, now what?
				// Handle uncategorized elements first
				if (element instanceof UncategorizedCategoryElement) {
					// Will have to look at all categories and other items first. 
					queryable = ((UncategorizedCategoryElement) element).getQueryable();
					Query firstPassQuery = allQuery;
					if (showGroupsOnly)
						firstPassQuery = new CompoundQuery(new Query[] {groupQuery, categoryQuery}, false);
					queryable = ((UncategorizedCategoryElement) element).getQueryable();
					Collector collector = showLatest ? new LatestIUVersionElementCollector(this, queryable, queryContext, false) : new AvailableIUCollector(this, queryable, queryContext, false);
					return new ElementQueryDescriptor(queryable, firstPassQuery, new UncategorizedElementCollector(this, queryable, queryContext, collector));

				}
				if (element instanceof CategoryElement) {
					Query membersOfCategoryQuery = new AnyRequiredCapabilityQuery(((CategoryElement) element).getRequirements());
					if (showGroupsOnly)
						// Query all groups and use the query result to optionally select the latest version only
						return new ElementQueryDescriptor(((CategoryElement) element).getQueryable(), new CompoundQuery(new Query[] {new CompoundQuery(new Query[] {groupQuery, categoryQuery}, false), membersOfCategoryQuery}, true), showLatest ? new LatestIUVersionElementCollector(this, ((CategoryElement) element).getQueryable(), queryContext, true) : new AvailableIUCollector(this, ((CategoryElement) element).getQueryable(), queryContext, true));
					if (showLatest)
						// We are not querying groups, but we are showing the latest version only
						return new ElementQueryDescriptor(((CategoryElement) element).getQueryable(), membersOfCategoryQuery, new LatestIUVersionElementCollector(this, ((CategoryElement) element).getQueryable(), queryContext, true));
					// Show 'em all!
					return new ElementQueryDescriptor(((CategoryElement) element).getQueryable(), membersOfCategoryQuery, new AvailableIUCollector(this, ((CategoryElement) element).getQueryable(), queryContext, true));
				}
				// We've already collapsed all versions, show the rest
				if (element instanceof IUVersionsElement) {
					IInstallableUnit iu = ((IUVersionsElement) element).getIU();
					return new ElementQueryDescriptor(((IUVersionsElement) element).getQueryable(), new InstallableUnitQuery(iu.getId()), new OtherIUVersionsCollector(iu, this, ((IUVersionsElement) element).getQueryable(), queryContext));
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
					collector = new LatestIUVersionCollector(this, updateQueryable, queryContext, useCategories);
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
					return new ElementQueryDescriptor(profile, new CompoundQuery(new Query[] {groupQuery, query}, true), new InstalledIUCollector(this, profile, queryContext));
				return new ElementQueryDescriptor(profile, query, new InstalledIUCollector(this, profile, queryContext));
			case IQueryProvider.METADATA_REPOS :
				if (element instanceof MetadataRepositories) {
					MetadataRepositories metaRepos = (MetadataRepositories) element;
					if (metaRepos.getMetadataRepositories() == null)
						metaRepos.setRepoFlags(hideSystem ? IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM : IMetadataRepositoryManager.REPOSITORIES_ALL);
					queryable = new QueryableMetadataRepositoryManager(metaRepos);
					return new ElementQueryDescriptor(queryable, null, new MetadataRepositoryElementCollector(this, queryContext));
				}
				return null;
			case IQueryProvider.PROFILES :
				queryable = new QueryableProfileRegistry();
				return new ElementQueryDescriptor(queryable, new Query() {
					public boolean isMatch(Object candidate) {
						return ProvUI.getAdapter(candidate, IProfile.class) != null;
					}
				}, new ProfileElementCollector(this, null, queryContext));
			default :
				return null;
		}
	}
}
