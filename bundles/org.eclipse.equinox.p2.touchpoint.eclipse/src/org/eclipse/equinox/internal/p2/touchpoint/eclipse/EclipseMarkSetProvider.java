/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.garbagecollector.MarkSet;
import org.eclipse.equinox.internal.p2.garbagecollector.MarkSetProvider;
import org.eclipse.equinox.internal.p2.update.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

/**
 * MarkSetProvider implementation for the Eclipse touchpoint.
 */
public class EclipseMarkSetProvider extends MarkSetProvider {
	private static final String ARTIFACT_CLASSIFIER_OSGI_BUNDLE = "osgi.bundle"; //$NON-NLS-1$
	private static final String ARTIFACT_CLASSIFIER_FEATURE = "org.eclipse.update.feature"; //$NON-NLS-1$

	private Collection<IArtifactKey> artifactKeyList = null;

	public MarkSet[] getMarkSets(IProvisioningAgent agent, IProfile inProfile) {
		artifactKeyList = new HashSet<IArtifactKey>();
		IArtifactRepository repositoryToGC = Util.getBundlePoolRepository(agent, inProfile);
		if (repositoryToGC == null)
			return new MarkSet[0];
		addArtifactKeys(inProfile);
		IProfile currentProfile = getCurrentProfile(agent);
		if (currentProfile != null && inProfile.getProfileId().equals(currentProfile.getProfileId())) {
			addRunningBundles(repositoryToGC);
			addRunningFeatures(inProfile, repositoryToGC);
		}
		return new MarkSet[] {new MarkSet(artifactKeyList.toArray(new IArtifactKey[artifactKeyList.size()]), repositoryToGC)};
	}

	private void addRunningFeatures(IProfile profile, IArtifactRepository repositoryToGC) {
		try {
			List<Feature> allFeatures = getAllFeatures(Configuration.load(new File(Util.getConfigurationFolder(profile), "org.eclipse.update/platform.xml"), null)); //$NON-NLS-1$
			for (Feature f : allFeatures) {
				IArtifactKey match = searchArtifact(f.getId(), Version.create(f.getVersion()), ARTIFACT_CLASSIFIER_FEATURE, repositoryToGC);
				if (match != null)
					artifactKeyList.add(match);
			}
		} catch (ProvisionException e) {
			//Ignore the exception
		}
	}

	private List<Feature> getAllFeatures(Configuration cfg) {
		if (cfg == null)
			return Collections.<Feature> emptyList();
		List<Site> sites = cfg.getSites();
		ArrayList<Feature> result = new ArrayList<Feature>();
		for (Site object : sites) {
			Feature[] features = object.getFeatures();
			for (int i = 0; i < features.length; i++) {
				result.add(features[i]);
			}
		}
		return result;
	}

	private IProfile getCurrentProfile(IProvisioningAgent agent) {
		IProfileRegistry pr = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (pr == null)
			return null;
		return pr.getProfile(IProfileRegistry.SELF);
	}

	private void addArtifactKeys(IProfile aProfile) {
		Iterator<IInstallableUnit> installableUnits = aProfile.query(QueryUtil.createIUAnyQuery(), null).iterator();
		while (installableUnits.hasNext()) {
			Collection<IArtifactKey> keys = installableUnits.next().getArtifacts();
			if (keys == null)
				continue;
			artifactKeyList.addAll(keys);
		}
	}

	public IArtifactRepository getRepository(IProvisioningAgent agent, IProfile aProfile) {
		return Util.getBundlePoolRepository(agent, aProfile);
	}

	private void addRunningBundles(IArtifactRepository repo) {
		artifactKeyList.addAll(findCorrespondinArtifacts(new WhatIsRunning().getBundlesBeingRun(), repo));
	}

	private IArtifactKey searchArtifact(String searchedId, Version searchedVersion, String classifier, IArtifactRepository repo) {
		//This is somewhat cheating since normally we should get the artifact key from the IUs that were representing the running system (e.g. we could get that info from the rollback repo)
		VersionRange range = searchedVersion != null ? new VersionRange(searchedVersion, true, searchedVersion, true) : null;
		ArtifactKeyQuery query = new ArtifactKeyQuery(classifier, searchedId, range);
		//TODO short-circuit the query when we find one?
		IQueryResult<IArtifactKey> keys = repo.query(query, null);
		if (!keys.isEmpty())
			return keys.iterator().next();
		return null;
	}

	//Find for each bundle info a corresponding artifact in repo 
	private List<IArtifactKey> findCorrespondinArtifacts(BundleInfo[] bis, IArtifactRepository repo) {
		ArrayList<IArtifactKey> toRetain = new ArrayList<IArtifactKey>();
		for (int i = 0; i < bis.length; i++) {
			// if version is "0.0.0", we will use null to find all versions, see bug 305710
			Version version = BundleInfo.EMPTY_VERSION.equals(bis[i].getVersion()) ? null : Version.create(bis[i].getVersion());
			IArtifactKey match = searchArtifact(bis[i].getSymbolicName(), version, ARTIFACT_CLASSIFIER_OSGI_BUNDLE, repo);
			if (match != null)
				toRetain.add(match);
		}
		return toRetain;
	}
}
