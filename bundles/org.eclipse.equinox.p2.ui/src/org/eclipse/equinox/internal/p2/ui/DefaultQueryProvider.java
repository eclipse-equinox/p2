/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.net.URI;
import java.net.URL;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.query.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.IUProfilePropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IUPropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.*;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.model.MetadataRepositories;
import org.eclipse.equinox.internal.provisional.p2.ui.model.Updates;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Provides a default set of queries to drive the provisioning UI.
 * 
 * @since 3.5
 */

public class DefaultQueryProvider extends QueryProvider {

	private Policy policy;

	private Query allQuery = new Query() {
		public boolean isMatch(Object candidate) {
			return true;
		}
	};

	public DefaultQueryProvider(Policy policy) {
		this.policy = policy;
	}

	public ElementQueryDescriptor getQueryDescriptor(final QueriedElement element) {
		IQueryable queryable;
		int queryType = element.getQueryType();
		IUViewQueryContext context = element.getQueryContext();
		if (context == null) {
			context = policy.getQueryContext();
		}
		switch (queryType) {
			case QueryProvider.ARTIFACT_REPOS :
				queryable = new QueryableArtifactRepositoryManager(context.getArtifactRepositoryFlags());
				return new ElementQueryDescriptor(queryable, null, new Collector() {
					public boolean accept(Object object) {
						if (object instanceof URL)
							return super.accept(new ArtifactRepositoryElement(element, (URI) object));
						return true;
					}
				});
			case QueryProvider.AVAILABLE_IUS :
				// Things get more complicated if the user wants to filter out installed items. 
				// This involves setting up a secondary query for installed content that the various
				// collectors will use to reject content.  We can't use a compound query because the
				// queryables are different (profile for installed content, repo for available content)
				AvailableIUCollector availableIUCollector;
				ElementQueryDescriptor installedQueryDescriptor = null;
				boolean showLatest = context.getShowLatestVersionsOnly();
				boolean hideInstalled = context.getHideAlreadyInstalled();
				String profileId = context.getInstalledProfileId();
				if (hideInstalled && profileId != null) {
					try {
						IProfile profile = ProvisioningUtil.getProfile(profileId);
						installedQueryDescriptor = new ElementQueryDescriptor(profile, new IUProfilePropertyByIdQuery(profile.getProfileId(), context.getVisibleInstalledIUProperty(), Boolean.toString(true)), new Collector());
					} catch (ProvisionException e) {
						// just bail out, we won't try to query the installed
						installedQueryDescriptor = null;
					}
				}

				// Showing children of a rollback element
				if (element instanceof RollbackRepositoryElement) {
					Query profileIdQuery = new InstallableUnitQuery(((RollbackRepositoryElement) element).getProfileId());
					Query rollbackIUQuery = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_PROFILE, Boolean.toString(true));
					availableIUCollector = new RollbackIUCollector(((RollbackRepositoryElement) element).getQueryable(), element.getParent(element));
					return new ElementQueryDescriptor(((RollbackRepositoryElement) element).getQueryable(), new CompoundQuery(new Query[] {profileIdQuery, rollbackIUQuery}, true), availableIUCollector);
				}

				Query topLevelQuery = new IUPropertyQuery(context.getVisibleAvailableIUProperty(), Boolean.TRUE.toString());
				Query categoryQuery = new IUPropertyQuery(IInstallableUnit.PROP_TYPE_CATEGORY, Boolean.toString(true));

				// Showing child IU's of a group of repositories, or of a single repository
				if (element instanceof MetadataRepositories || element instanceof MetadataRepositoryElement) {
					queryable = element.getQueryable();

					if (context.getViewType() == IUViewQueryContext.AVAILABLE_VIEW_FLAT) {
						AvailableIUCollector collector;
						if (showLatest)
							collector = new LatestIUVersionElementCollector(queryable, element, true);
						else
							collector = new AvailableIUCollector(queryable, element, true);
						if (hideInstalled && installedQueryDescriptor != null)
							collector.hideInstalledIUs(installedQueryDescriptor);
						return new ElementQueryDescriptor(queryable, topLevelQuery, collector);
					}

					// Assume category view if it wasn't flat.
					// Installed content not a concern for collecting categories
					return new ElementQueryDescriptor(queryable, categoryQuery, new CategoryElementCollector(queryable, element, true));
				}

				// Showing children of categories that we've already collected
				// Must do this one before CategoryElement since it's a subclass
				if (element instanceof UncategorizedCategoryElement) {
					// Will have to look at all categories and groups and from there, figure out what's left
					Query firstPassQuery = new CompoundQuery(new Query[] {topLevelQuery, categoryQuery}, false);
					queryable = ((UncategorizedCategoryElement) element).getQueryable();
					availableIUCollector = showLatest ? new LatestIUVersionElementCollector(queryable, element, false) : new AvailableIUCollector(queryable, element, false);
					if (hideInstalled && installedQueryDescriptor != null)
						availableIUCollector.hideInstalledIUs(installedQueryDescriptor);
					return new ElementQueryDescriptor(queryable, firstPassQuery, new UncategorizedElementCollector(queryable, element, availableIUCollector));

				}
				if (element instanceof CategoryElement) {
					Query membersOfCategoryQuery = new AnyRequiredCapabilityQuery(((CategoryElement) element).getRequirements());
					if (showLatest)
						availableIUCollector = new LatestIUVersionElementCollector(((CategoryElement) element).getQueryable(), element, true);
					else
						availableIUCollector = new AvailableIUCollector(((CategoryElement) element).getQueryable(), element, true);
					if (hideInstalled && installedQueryDescriptor != null)
						availableIUCollector.hideInstalledIUs(installedQueryDescriptor);
					return new ElementQueryDescriptor(((CategoryElement) element).getQueryable(), new CompoundQuery(new Query[] {new CompoundQuery(new Query[] {topLevelQuery, categoryQuery}, false), membersOfCategoryQuery}, true), availableIUCollector);
				}
				// If we are showing only the latest version, we never represent other versions as children.
				if (element instanceof IUVersionsElement) {
					return null;
				}
			case QueryProvider.AVAILABLE_UPDATES :
				IProfile profile;
				IInstallableUnit[] toUpdate = null;
				if (element instanceof Updates) {
					try {
						profile = ProvisioningUtil.getProfile(((Updates) element).getProfileId());
					} catch (ProvisionException e) {
						ProvUI.handleException(e, NLS.bind(ProvUIMessages.DefaultQueryProvider_ErrorRetrievingProfile, ((Updates) element).getProfileId()), StatusManager.LOG);
						return null;
					}
					toUpdate = ((Updates) element).getIUs();
				} else {
					profile = (IProfile) ProvUI.getAdapter(element, IProfile.class);
				}
				if (profile == null)
					return null;
				Collector collector;
				if (toUpdate == null) {
					collector = profile.query(new IUProfilePropertyByIdQuery(profile.getProfileId(), context.getVisibleInstalledIUProperty(), Boolean.toString(true)), new Collector(), null);
					toUpdate = (IInstallableUnit[]) collector.toArray(IInstallableUnit.class);
				}
				QueryableUpdates updateQueryable = new QueryableUpdates(toUpdate);
				if (context.getShowLatestVersionsOnly())
					collector = new LatestIUVersionCollector(updateQueryable, element, true);
				else
					collector = new Collector();
				return new ElementQueryDescriptor(updateQueryable, allQuery, collector);
			case QueryProvider.INSTALLED_IUS :
				profile = (IProfile) ProvUI.getAdapter(element, IProfile.class);
				if (profile == null)
					return null;
				// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=229352
				// Rollback profiles are specialized/temporary instances so we must use a query that uses the profile instance, not the id.
				if (element instanceof RollbackProfileElement)
					return new ElementQueryDescriptor(profile, new IUProfilePropertyQuery(profile, context.getVisibleInstalledIUProperty(), Boolean.toString(true)), new InstalledIUCollector(profile, element));
				return new ElementQueryDescriptor(profile, new IUProfilePropertyByIdQuery(profile.getProfileId(), context.getVisibleInstalledIUProperty(), Boolean.toString(true)), new InstalledIUCollector(profile, element));
			case QueryProvider.METADATA_REPOS :
				if (element instanceof MetadataRepositories) {
					if (element.getQueryable() == null) {
						queryable = new QueryableMetadataRepositoryManager(policy, ((MetadataRepositories) element).getIncludeDisabledRepositories());
						element.setQueryable(queryable);
					}
					return new ElementQueryDescriptor(element.getQueryable(), null, new MetadataRepositoryElementCollector(element.getQueryable(), element));
				}
				return null;
			case QueryProvider.PROFILES :
				queryable = new QueryableProfileRegistry();
				return new ElementQueryDescriptor(queryable, new Query() {
					public boolean isMatch(Object candidate) {
						return ProvUI.getAdapter(candidate, IProfile.class) != null;
					}
				}, new ProfileElementCollector(null, element));
			default :
				return null;
		}
	}
}
