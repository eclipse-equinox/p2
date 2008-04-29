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
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.net.URL;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Provides the queries appropriate for the SDK UI.
 * 
 * @since 3.4
 */

public class ProvSDKQueryProvider implements IQueryProvider {

	private Query allQuery = new Query() {
		public boolean isMatch(Object candidate) {
			return true;
		}

	};

	public ElementQueryDescriptor getQueryDescriptor(Object element, int queryType) {
		IQueryable queryable;
		QueryContext queryContext = null;
		if (element instanceof QueriedElement)
			queryContext = ((QueriedElement) element).getQueryContext();
		boolean showLatest = ProvSDKUIActivator.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION);
		switch (queryType) {
			case IQueryProvider.ARTIFACT_REPOS :
				queryable = new QueryableArtifactRepositoryManager(IArtifactRepositoryManager.REPOSITORIES_NON_SYSTEM);
				return new ElementQueryDescriptor(queryable, null, new Collector() {
					public boolean accept(Object object) {
						if (object instanceof URL)
							return super.accept(new ArtifactRepositoryElement((URL) object));
						return true;
					}
				});
			case IQueryProvider.AVAILABLE_IUS :
				// Things get more complicated if the user wants to filter out installed items. 
				// This involves setting up a secondary query for installed content that the various
				// collectors will use to reject content.  We can't use a compound query because the
				// queryables are different (profile for installed content, repo for available content)
				AvailableIUViewQueryContext availableViewQueryContext = null;
				AvailableIUCollector availableIUCollector;
				boolean hideInstalled = false;
				ElementQueryDescriptor installedQueryDescriptor = null;
				if (queryContext instanceof AvailableIUViewQueryContext) {
					availableViewQueryContext = (AvailableIUViewQueryContext) ((QueriedElement) element).getQueryContext();
					showLatest = availableViewQueryContext.getShowLatestVersionsOnly();
					hideInstalled = availableViewQueryContext.getHideAlreadyInstalled();
					String profileId = availableViewQueryContext.getInstalledProfileId();
					if (hideInstalled && profileId != null) {
						try {
							IProfile profile = ProvisioningUtil.getProfile(profileId);
							installedQueryDescriptor = new ElementQueryDescriptor(profile, new IUProfilePropertyByIdQuery(profile.getProfileId(), IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true)), new Collector());
						} catch (ProvisionException e) {
							// just bail out, we won't try to query the installed
							installedQueryDescriptor = null;
						}
					}
				}

				// Showing children of a rollback element
				if (element instanceof RollbackRepositoryElement) {
					Query profileIdQuery = new InstallableUnitQuery(((RollbackRepositoryElement) element).getProfileId());
					Query rollbackIUQuery = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_PROFILE, Boolean.toString(true));
					availableIUCollector = new RollbackIUCollector(this, ((RollbackRepositoryElement) element).getQueryable(), queryContext);
					if (hideInstalled && installedQueryDescriptor != null)
						availableIUCollector.hideInstalledIUs(installedQueryDescriptor);
					return new ElementQueryDescriptor(((RollbackRepositoryElement) element).getQueryable(), new CompoundQuery(new Query[] {profileIdQuery, rollbackIUQuery}, true), availableIUCollector);
				}

				Query groupQuery = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_GROUP, Boolean.TRUE.toString());
				Query categoryQuery = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_CATEGORY, Boolean.toString(true));

				// Showing child IU's of some repositories
				if (element instanceof MetadataRepositories) {
					MetadataRepositories metaRepos = (MetadataRepositories) element;
					if (metaRepos.getMetadataRepositories() == null)
						metaRepos.setRepoFlags(IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM);
					queryable = new QueryableMetadataRepositoryManager(metaRepos);

					if (availableViewQueryContext != null) {
						if (availableViewQueryContext.getViewType() == AvailableIUViewQueryContext.VIEW_FLAT) {
							AvailableIUCollector collector;
							if (showLatest)
								collector = new LatestIUVersionElementCollector(this, queryable, queryContext, true);
							else
								collector = new AvailableIUCollector(this, queryable, queryContext, true);
							if (hideInstalled && installedQueryDescriptor != null)
								collector.hideInstalledIUs(installedQueryDescriptor);
							return new ElementQueryDescriptor(queryable, groupQuery, collector);
						}
					}
					// Assume category view if it wasn't flat or if there was no query context at all.
					// Installed content not a concern for collecting categories
					return new ElementQueryDescriptor(queryable, categoryQuery, new CategoryElementCollector(this, queryable, queryContext, true));
				}

				// Showing children of a repository.  We always show categories, no concern for filtering installed content
				if (element instanceof MetadataRepositoryElement) {
					return new ElementQueryDescriptor(((MetadataRepositoryElement) element).getQueryable(), categoryQuery, new CategoryElementCollector(this, ((MetadataRepositoryElement) element).getQueryable(), queryContext, true));
				}

				// Showing children of categories
				// Must do this one before CategoryElement since it's a subclass
				if (element instanceof UncategorizedCategoryElement) {
					// Will have to look at all categories and groups and from there, figure out what's left
					Query firstPassQuery = new CompoundQuery(new Query[] {groupQuery, categoryQuery}, false);
					queryable = ((UncategorizedCategoryElement) element).getQueryable();
					availableIUCollector = showLatest ? new LatestIUVersionElementCollector(this, queryable, queryContext, false) : new AvailableIUCollector(this, queryable, queryContext, false);
					if (hideInstalled && installedQueryDescriptor != null)
						availableIUCollector.hideInstalledIUs(installedQueryDescriptor);
					return new ElementQueryDescriptor(queryable, firstPassQuery, new UncategorizedElementCollector(this, queryable, queryContext, availableIUCollector));

				}
				if (element instanceof CategoryElement) {
					Query membersOfCategoryQuery = new AnyRequiredCapabilityQuery(((CategoryElement) element).getRequirements());
					if (showLatest)
						availableIUCollector = new LatestIUVersionElementCollector(this, ((CategoryElement) element).getQueryable(), queryContext, true);
					else
						availableIUCollector = new AvailableIUCollector(this, ((CategoryElement) element).getQueryable(), queryContext, true);
					if (hideInstalled && installedQueryDescriptor != null)
						availableIUCollector.hideInstalledIUs(installedQueryDescriptor);
					return new ElementQueryDescriptor(((CategoryElement) element).getQueryable(), new CompoundQuery(new Query[] {new CompoundQuery(new Query[] {groupQuery, categoryQuery}, false), membersOfCategoryQuery}, true), availableIUCollector);
				}
				// If we are showing only the latest version, we never represent other versions as children.
				if (element instanceof IUVersionsElement) {
					return null;
				}
			case IQueryProvider.AVAILABLE_UPDATES :
				IProfile profile = (IProfile) ProvUI.getAdapter(element, IProfile.class);
				IInstallableUnit[] toUpdate;
				Collector collector;
				if (profile != null) {
					collector = profile.query(new IUProfilePropertyByIdQuery(profile.getProfileId(), IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true)), new Collector(), null);
					toUpdate = (IInstallableUnit[]) collector.toArray(IInstallableUnit.class);
				} else if (element instanceof UpdateEvent) {
					try {
						profile = ProvisioningUtil.getProfile(((UpdateEvent) element).getProfileId());
					} catch (ProvisionException e) {
						ProvUI.handleException(e, NLS.bind(ProvSDKMessages.ProvSDKQueryProvider_ErrorRetrievingProfile, ((UpdateEvent) element).getProfileId()), StatusManager.LOG);
						return null;
					}
					toUpdate = ((UpdateEvent) element).getIUs();
				} else
					return null;
				QueryableUpdates updateQueryable = new QueryableUpdates(toUpdate);
				if (showLatest)
					collector = new LatestIUVersionCollector(this, updateQueryable, queryContext, true);
				else
					collector = new Collector();
				return new ElementQueryDescriptor(updateQueryable, allQuery, collector);
			case IQueryProvider.INSTALLED_IUS :
				profile = (IProfile) ProvUI.getAdapter(element, IProfile.class);
				if (profile == null)
					return null;
				return new ElementQueryDescriptor(profile, new IUProfilePropertyByIdQuery(profile.getProfileId(), IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true)), new InstalledIUCollector(this, profile, queryContext));
			case IQueryProvider.METADATA_REPOS :
				if (element instanceof MetadataRepositories) {
					MetadataRepositories metaRepos = (MetadataRepositories) element;
					if (metaRepos.getMetadataRepositories() == null)
						metaRepos.setRepoFlags(IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM);
					queryable = new QueryableMetadataRepositoryManager(metaRepos);
					metaRepos.setQueryable(queryable);
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
