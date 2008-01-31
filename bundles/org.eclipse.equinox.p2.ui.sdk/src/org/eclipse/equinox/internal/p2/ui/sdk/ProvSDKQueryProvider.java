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

import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.query.CapabilityQuery;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.model.*;
import org.eclipse.equinox.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.p2.ui.query.*;
import org.eclipse.equinox.p2.updatechecker.UpdateEvent;

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
				queryable = new QueryableArtifactRepositoryManager();
				return new ElementQueryDescriptor(queryable, new FilteredRepositoryQuery(IArtifactRepositoryManager.REPOSITORIES_NON_SYSTEM), new QueriedElementCollector(this, queryable));
			case IQueryProvider.AVAILABLE_IUS :
				if (element instanceof RollbackRepositoryElement) {
					Query profileIdQuery = new InstallableUnitQuery(((RollbackRepositoryElement) element).getProfileId());
					Query rollbackIUQuery = new IUPropertyQuery(IInstallableUnit.PROP_PROFILE_IU_KEY, Boolean.toString(true));
					return new ElementQueryDescriptor(((RollbackRepositoryElement) element).getQueryable(), new CompoundQuery(new Query[] {profileIdQuery, rollbackIUQuery}, true), new RollbackIUCollector(this, ((RollbackRepositoryElement) element).getQueryable()));
				}
				CapabilityQuery groupQuery = new CapabilityQuery(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_KIND, "group", null, null, false, false)); //$NON-NLS-1$
				Query categoryQuery = new IUPropertyQuery(IInstallableUnit.PROP_CATEGORY_IU, Boolean.toString(true));
				if (element instanceof MetadataRepositoryElement) {
					return new ElementQueryDescriptor(((MetadataRepositoryElement) element).getQueryable(), categoryQuery, new CategoryElementCollector(this, ((MetadataRepositoryElement) element).getQueryable(), false));
				}
				if (element instanceof CategoryElement) {
					Query membersOfCategoryQuery;
					if (element instanceof UncategorizedCategoryElement)
						membersOfCategoryQuery = allQuery;
					else
						membersOfCategoryQuery = new AnyRequiredCapabilityQuery(((CategoryElement) element).getIU());
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
						ProvUI.handleException(e, null);
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
				if (element instanceof MetadataRepositories)
					queryable = new QueryableMetadataRepositoryManager(((MetadataRepositories) element).getMetadataRepositories());
				else
					queryable = new QueryableMetadataRepositoryManager();
				return new ElementQueryDescriptor(queryable, new FilteredRepositoryQuery(IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM), new QueriedElementCollector(this, queryable));
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
