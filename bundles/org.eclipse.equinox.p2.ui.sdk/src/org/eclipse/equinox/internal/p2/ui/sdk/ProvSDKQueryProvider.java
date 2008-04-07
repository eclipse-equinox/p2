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

				// Showing children of a rollback element
				if (element instanceof RollbackRepositoryElement) {
					Query profileIdQuery = new InstallableUnitQuery(((RollbackRepositoryElement) element).getProfileId());
					Query rollbackIUQuery = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_PROFILE, Boolean.toString(true));
					return new ElementQueryDescriptor(((RollbackRepositoryElement) element).getQueryable(), new CompoundQuery(new Query[] {profileIdQuery, rollbackIUQuery}, true), new RollbackIUCollector(this, ((RollbackRepositoryElement) element).getQueryable()));
				}

				Query groupQuery = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_GROUP, Boolean.TRUE.toString());
				Query categoryQuery = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_CATEGORY, Boolean.toString(true));

				// Showing child IU's of some repositories
				if (element instanceof MetadataRepositories) {
					MetadataRepositories metaRepos = (MetadataRepositories) element;
					if (metaRepos.getMetadataRepositories() != null)
						queryable = new QueryableMetadataRepositoryManager(((MetadataRepositories) element).getMetadataRepositories());
					else
						queryable = new QueryableMetadataRepositoryManager(IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM);

					if (metaRepos.getQueryContext() != null && metaRepos.getQueryContext() instanceof AvailableIUViewQueryContext) {
						AvailableIUViewQueryContext context = (AvailableIUViewQueryContext) metaRepos.getQueryContext();
						if (context.getViewType() == AvailableIUViewQueryContext.VIEW_FLAT) {
							Collector collector;
							if (showLatest)
								collector = new LatestIUVersionElementCollector(this, queryable, true);
							else
								collector = new AvailableIUCollector(this, queryable, true);
							return new ElementQueryDescriptor(queryable, groupQuery, collector);
						}
					}
					// If there is no query context, assume by category
					return new ElementQueryDescriptor(queryable, categoryQuery, new CategoryElementCollector(this, queryable, true));
				}

				// Showing children of a repository
				if (element instanceof MetadataRepositoryElement) {
					return new ElementQueryDescriptor(((MetadataRepositoryElement) element).getQueryable(), categoryQuery, new CategoryElementCollector(this, ((MetadataRepositoryElement) element).getQueryable(), true));
				}

				// Showing children of categories
				// Must do this one before CategoryElement since it's a subclass
				if (element instanceof UncategorizedCategoryElement) {
					// Will have to look at all categories and groups and from there, figure out what's left
					Query firstPassQuery = new CompoundQuery(new Query[] {groupQuery, categoryQuery}, false);
					queryable = ((UncategorizedCategoryElement) element).getQueryable();
					Collector collector = showLatest ? new LatestIUVersionElementCollector(this, queryable, false) : new AvailableIUCollector(this, queryable, false);
					return new ElementQueryDescriptor(queryable, firstPassQuery, new UncategorizedElementCollector(this, queryable, collector));

				}
				if (element instanceof CategoryElement) {
					Query membersOfCategoryQuery = new AnyRequiredCapabilityQuery(((CategoryElement) element).getRequirements());
					Collector collector;
					if (showLatest)
						collector = new LatestIUVersionElementCollector(this, ((CategoryElement) element).getQueryable(), true);
					else
						collector = new AvailableIUCollector(this, ((CategoryElement) element).getQueryable(), true);
					return new ElementQueryDescriptor(((CategoryElement) element).getQueryable(), new CompoundQuery(new Query[] {new CompoundQuery(new Query[] {groupQuery, categoryQuery}, false), membersOfCategoryQuery}, true), collector);
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
					collector = profile.query(new IUProfilePropertyQuery(profile, IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true)), new Collector(), null);
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
					collector = new LatestIUVersionCollector(this, updateQueryable, true);
				else
					collector = new Collector();
				return new ElementQueryDescriptor(updateQueryable, allQuery, collector);
			case IQueryProvider.INSTALLED_IUS :
				profile = (IProfile) ProvUI.getAdapter(element, IProfile.class);
				return new ElementQueryDescriptor(profile, new IUProfilePropertyQuery(profile, IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true)), new InstalledIUCollector(this, profile));
			case IQueryProvider.METADATA_REPOS :
				if (element instanceof MetadataRepositories) {
					MetadataRepositories metaRepos = (MetadataRepositories) element;
					if (metaRepos.getMetadataRepositories() != null)
						queryable = new QueryableMetadataRepositoryManager(((MetadataRepositories) element).getMetadataRepositories());
					else
						queryable = new QueryableMetadataRepositoryManager(IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM);
				} else
					queryable = new QueryableMetadataRepositoryManager(IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM);
				return new ElementQueryDescriptor(queryable, null, new MetadataRepositoryElementCollector(this));
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
