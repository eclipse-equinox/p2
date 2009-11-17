/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.net.URI;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.query.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.p2.metadata.query.CategoryQuery;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.ui.*;

/**
 * Provides a default set of queries to drive the provisioning UI.
 * 
 * @since 3.5
 */

public class QueryProvider {

	private ProvisioningUI ui;

	public static final int METADATA_REPOS = 1;
	public static final int ARTIFACT_REPOS = 2;
	public static final int PROFILES = 3;
	public static final int AVAILABLE_IUS = 4;
	public static final int AVAILABLE_UPDATES = 5;
	public static final int INSTALLED_IUS = 6;

	private IQuery allQuery = new MatchQuery() {
		public boolean isMatch(Object candidate) {
			return true;
		}
	};

	public QueryProvider(ProvisioningUI ui) {
		this.ui = ui;
	}

	public ElementQueryDescriptor getQueryDescriptor(final QueriedElement element) {
		// Initialize queryable, queryContext, and queryType from the element.
		// In some cases we override this.
		Policy policy = ui.getPolicy();
		IQueryable queryable = element.getQueryable();
		int queryType = element.getQueryType();
		IUViewQueryContext context = element.getQueryContext();
		if (context == null) {
			context = policy.getQueryContext();
		}
		switch (queryType) {
			case ARTIFACT_REPOS :
				queryable = new QueryableArtifactRepositoryManager(ui.getSession(), policy.getRepositoryManipulator(), false);
				return new ElementQueryDescriptor(queryable, new RepositoryLocationQuery(), new Collector() {
					public boolean accept(Object object) {
						if (object instanceof URI)
							return super.accept(new ArtifactRepositoryElement(element, (URI) object));
						return true;
					}
				});

			case AVAILABLE_IUS :
				// Things get more complicated if the user wants to filter out installed items. 
				// This involves setting up a secondary query for installed content that the various
				// collectors will use to reject content.  We can't use a compound query because the
				// queryables are different (profile for installed content, repo for available content)
				AvailableIUWrapper availableIUWrapper;
				boolean showLatest = context.getShowLatestVersionsOnly();
				boolean hideInstalled = context.getHideAlreadyInstalled();
				IProfile targetProfile = null;
				String profileId = context.getInstalledProfileId();
				if (profileId != null) {
					targetProfile = ui.getSession().getProfile(profileId);
				}

				IQuery topLevelQuery = context.getVisibleAvailableIUProperty();
				IQuery categoryQuery = new CategoryQuery();

				// Showing child IU's of a group of repositories, or of a single repository
				if (element instanceof MetadataRepositories || element instanceof MetadataRepositoryElement) {
					if (context.getViewType() == IUViewQueryContext.AVAILABLE_VIEW_FLAT || !context.getUseCategories()) {
						AvailableIUWrapper wrapper = new AvailableIUWrapper(queryable, element, false, context.getShowAvailableChildren());
						if (showLatest)
							topLevelQuery = new PipedQuery(new IQuery[] {topLevelQuery, new LatestIUVersionQuery()});
						if (targetProfile != null)
							wrapper.markInstalledIUs(targetProfile, hideInstalled);
						return new ElementQueryDescriptor(queryable, topLevelQuery, new Collector(), wrapper);
					}
					// Installed content not a concern for collecting categories
					return new ElementQueryDescriptor(queryable, categoryQuery, new Collector(), new CategoryElementWrapper(queryable, element));
				}

				// If it's a category or some other IUElement to drill down in, we get the requirements and show all requirements
				// that are also visible in the available list.  
				if (element instanceof CategoryElement || (element instanceof IIUElement && ((IIUElement) element).shouldShowChildren())) {
					// children of a category should drill down according to the context.  If we aren't in a category, we are already drilling down and
					// continue to do so.
					boolean drillDown = element instanceof CategoryElement ? context.getShowAvailableChildren() : true;
					IQuery meetsAnyRequirementQuery = new CapabilityQuery(((IIUElement) element).getRequirements());
					availableIUWrapper = new AvailableIUWrapper(queryable, element, true, drillDown);
					if (targetProfile != null)
						availableIUWrapper.markInstalledIUs(targetProfile, hideInstalled);
					// if it's a category, the metadata was specifically set up so that the requirements are the IU's that should
					// be visible in the category.
					if (element instanceof CategoryElement) {
						if (showLatest)
							meetsAnyRequirementQuery = new PipedQuery(new IQuery[] {meetsAnyRequirementQuery, new LatestIUVersionQuery()});
						return new ElementQueryDescriptor(queryable, meetsAnyRequirementQuery, new Collector(), availableIUWrapper);
					}
					IQuery query = CompoundQuery.createCompoundQuery(new IQuery[] {topLevelQuery, meetsAnyRequirementQuery}, true);
					if (showLatest)
						query = new PipedQuery(new IQuery[] {query, new LatestIUVersionQuery()});
					// If it's not a category, these are generic requirements and should be filtered by the visibility property (topLevelQuery)
					return new ElementQueryDescriptor(queryable, query, new Collector(), availableIUWrapper);
				}
				return null;

			case AVAILABLE_UPDATES :
				// This query can be used by the automatic updater in headless cases (checking for updates).  
				// We traffic in IU's rather than wrapped elements
				IProfile profile;
				IInstallableUnit[] toUpdate = null;
				if (element instanceof Updates) {
					profile = ui.getSession().getProfile(((Updates) element).getProfileId());
					toUpdate = ((Updates) element).getIUs();
				} else {
					profile = (IProfile) ProvUI.getAdapter(element, IProfile.class);
				}
				if (profile == null)
					return null;
				if (toUpdate == null) {
					Collector collector = profile.query(context.getVisibleInstalledIUProperty(), new Collector(), null);
					toUpdate = (IInstallableUnit[]) collector.toArray(IInstallableUnit.class);
				}
				QueryableUpdates updateQueryable = new QueryableUpdates(ui, toUpdate);
				return new ElementQueryDescriptor(updateQueryable, context.getShowLatestVersionsOnly() ? new LatestIUVersionQuery() : allQuery, new Collector());

			case INSTALLED_IUS :
				// Querying of IU's.  We are drilling down into the requirements.
				if (element instanceof IIUElement && context.getShowInstallChildren()) {
					IQuery meetsAnyRequirementQuery = new CapabilityQuery(((IIUElement) element).getRequirements());
					IQuery visibleAsAvailableQuery = context.getVisibleAvailableIUProperty();
					return new ElementQueryDescriptor(queryable, CompoundQuery.createCompoundQuery(new IQuery[] {visibleAsAvailableQuery, meetsAnyRequirementQuery}, true), new Collector(), new InstalledIUElementWrapper(queryable, element));
				}
				profile = (IProfile) ProvUI.getAdapter(element, IProfile.class);
				if (profile == null)
					return null;
				return new ElementQueryDescriptor(profile, context.getVisibleInstalledIUProperty(), new Collector(), new InstalledIUElementWrapper(profile, element));

			case METADATA_REPOS :
				if (element instanceof MetadataRepositories) {
					if (queryable == null) {
						queryable = new QueryableMetadataRepositoryManager(ui.getSession(), policy.getRepositoryManipulator(), ((MetadataRepositories) element).getIncludeDisabledRepositories());
						element.setQueryable(queryable);
					}
					return new ElementQueryDescriptor(element.getQueryable(), new RepositoryLocationQuery(), new Collector(), new MetadataRepositoryElementWrapper(element.getQueryable(), element));
				}
				return null;

			case PROFILES :
				queryable = new QueryableProfileRegistry(ui);
				return new ElementQueryDescriptor(queryable, new MatchQuery() {
					public boolean isMatch(Object candidate) {
						return ProvUI.getAdapter(candidate, IProfile.class) != null;
					}
				}, new Collector(), new ProfileElementWrapper(null, element));

			default :
				return null;
		}
	}
}
