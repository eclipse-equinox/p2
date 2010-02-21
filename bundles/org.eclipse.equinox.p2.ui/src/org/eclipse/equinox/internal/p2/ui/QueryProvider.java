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
import java.util.Collection;
import org.eclipse.equinox.internal.p2.metadata.query.LatestIUVersionQuery;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.query.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.query.*;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

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
	public static final int AVAILABLE_ARTIFACTS = 7;

	private IQuery<IInstallableUnit> allQuery = new MatchQuery<IInstallableUnit>() {
		public boolean isMatch(IInstallableUnit candidate) {
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
		IQueryable<?> queryable = element.getQueryable();
		int queryType = element.getQueryType();
		IUViewQueryContext context = element.getQueryContext();
		if (context == null) {
			context = ProvUI.getQueryContext(policy);
		}
		switch (queryType) {
			case ARTIFACT_REPOS :
				queryable = new QueryableArtifactRepositoryManager(ui, false).locationsQueriable();
				return new ElementQueryDescriptor(queryable, new RepositoryLocationQuery(), new Collector<URI>(), new ArtifactRepositoryElementWrapper(null, element));

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
					targetProfile = ui.getSession().getProfileRegistry().getProfile(profileId);
				}

				IQuery<IInstallableUnit> topLevelQuery = policy.getVisibleAvailableIUQuery();
				IQuery<IInstallableUnit> categoryQuery = new CategoryQuery();

				// Showing child IU's of a group of repositories, or of a single repository
				if (element instanceof MetadataRepositories || element instanceof MetadataRepositoryElement) {
					if (context.getViewType() == IUViewQueryContext.AVAILABLE_VIEW_FLAT || !context.getUseCategories()) {
						AvailableIUWrapper wrapper = new AvailableIUWrapper(queryable, element, false, context.getShowAvailableChildren());
						if (showLatest)
							topLevelQuery = new LatestIUVersionQuery<IInstallableUnit>(topLevelQuery);
						if (targetProfile != null)
							wrapper.markInstalledIUs(targetProfile, hideInstalled);
						return new ElementQueryDescriptor(queryable, topLevelQuery, new Collector<Object>(), wrapper);
					}
					// Installed content not a concern for collecting categories
					return new ElementQueryDescriptor(queryable, categoryQuery, new Collector<Object>(), new CategoryElementWrapper(queryable, element));
				}

				// If it's a category or some other IUElement to drill down in, we get the requirements and show all requirements
				// that are also visible in the available list.  
				if (element instanceof CategoryElement || (element instanceof IIUElement && ((IIUElement) element).shouldShowChildren())) {
					// children of a category should drill down according to the context.  If we aren't in a category, we are already drilling down and
					// continue to do so.
					boolean drillDownTheChildren = element instanceof CategoryElement ? context.getShowAvailableChildren() : true;
					IQuery<IInstallableUnit> memberOfCategoryQuery = new CategoryMemberQuery(((IIUElement) element).getIU());
					availableIUWrapper = new AvailableIUWrapper(queryable, element, true, drillDownTheChildren);
					if (targetProfile != null)
						availableIUWrapper.markInstalledIUs(targetProfile, hideInstalled);
					// if it's a category, there is a special query.
					if (element instanceof CategoryElement) {
						if (showLatest)
							memberOfCategoryQuery = new LatestIUVersionQuery<IInstallableUnit>(memberOfCategoryQuery);
						return new ElementQueryDescriptor(queryable, memberOfCategoryQuery, new Collector<Object>(), availableIUWrapper);
					}
					// It is not a category, we want to traverse the requirements that are groups.
					@SuppressWarnings("unchecked")
					IQuery<IInstallableUnit> query = CompoundQuery.createCompoundQuery(new IQuery[] {topLevelQuery, new RequiredIUsQuery(((IIUElement) element).getIU())}, true);
					if (showLatest)
						query = new LatestIUVersionQuery<IInstallableUnit>(query);
					// If it's not a category, these are generic requirements and should be filtered by the visibility property (topLevelQuery)
					return new ElementQueryDescriptor(queryable, query, new Collector<Object>(), availableIUWrapper);
				}
				return null;

			case AVAILABLE_UPDATES :
				// This query can be used by the automatic updater in headless cases (checking for updates).  
				// We traffic in IU's rather than wrapped elements
				IProfile profile;
				IInstallableUnit[] toUpdate = null;
				if (element instanceof Updates) {
					profile = ui.getSession().getProfileRegistry().getProfile(((Updates) element).getProfileId());
					toUpdate = ((Updates) element).getIUs();
				} else {
					profile = ProvUI.getAdapter(element, IProfile.class);
				}
				if (profile == null)
					return null;
				if (toUpdate == null) {
					IQueryResult<IInstallableUnit> queryResult = profile.query(policy.getVisibleInstalledIUQuery(), null);
					toUpdate = queryResult.toArray(IInstallableUnit.class);
				}
				QueryableUpdates updateQueryable = new QueryableUpdates(ui, toUpdate);
				return new ElementQueryDescriptor(updateQueryable, context.getShowLatestVersionsOnly() ? new LatestIUVersionQuery<IInstallableUnit>() : allQuery, new Collector<Object>());

			case INSTALLED_IUS :
				// Querying of IU's.  We are drilling down into the requirements.
				if (element instanceof IIUElement && context.getShowInstallChildren()) {
					Collection<IRequirement> reqs = ((IIUElement) element).getRequirements();
					IExpression[] requirementExpressions = new IExpression[reqs.size()];
					int i = 0;
					for (IRequirement req : reqs) {
						requirementExpressions[i++] = req.getMatches();
					}
					IExpressionFactory factory = ExpressionUtil.getFactory();
					IQuery<IInstallableUnit> meetsAnyRequirementQuery = new ExpressionQuery<IInstallableUnit>(IInstallableUnit.class, factory.or(requirementExpressions));
					IQuery<IInstallableUnit> visibleAsAvailableQuery = policy.getVisibleAvailableIUQuery();
					@SuppressWarnings("unchecked")
					IQuery<IInstallableUnit> createCompoundQuery = CompoundQuery.createCompoundQuery(new IQuery[] {visibleAsAvailableQuery, meetsAnyRequirementQuery}, true);
					return new ElementQueryDescriptor(queryable, createCompoundQuery, new Collector<IInstallableUnit>(), new InstalledIUElementWrapper(queryable, element));
				}
				profile = ProvUI.getAdapter(element, IProfile.class);
				if (profile == null)
					return null;
				return new ElementQueryDescriptor(profile, policy.getVisibleInstalledIUQuery(), new Collector<IInstallableUnit>(), new InstalledIUElementWrapper(profile, element));

			case METADATA_REPOS :
				if (element instanceof MetadataRepositories) {
					if (queryable == null) {
						queryable = new QueryableMetadataRepositoryManager(ui, ((MetadataRepositories) element).getIncludeDisabledRepositories()).locationsQueriable();
						element.setQueryable(queryable);
					}
					return new ElementQueryDescriptor(element.getQueryable(), new RepositoryLocationQuery(), new Collector<URI>(), new MetadataRepositoryElementWrapper(null, element));
				}
				return null;

			case PROFILES :
				queryable = new QueryableProfileRegistry(ui);
				return new ElementQueryDescriptor(queryable, new MatchQuery<Object>() {
					public boolean isMatch(Object candidate) {
						return ProvUI.getAdapter(candidate, IProfile.class) != null;
					}
				}, new Collector<Object>(), new ProfileElementWrapper(null, element));

			case AVAILABLE_ARTIFACTS :
				if (!(queryable instanceof IArtifactRepository))
					return null;
				return new ElementQueryDescriptor(queryable, ArtifactKeyQuery.ALL_KEYS, new Collector<Object>(), new ArtifactKeyWrapper((IArtifactRepository) queryable, element));

			default :
				return null;
		}
	}
}
