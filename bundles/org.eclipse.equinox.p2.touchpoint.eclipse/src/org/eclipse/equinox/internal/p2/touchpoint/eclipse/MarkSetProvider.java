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
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.p2.garbagecollector.IMarkSetProvider;
import org.eclipse.equinox.internal.p2.garbagecollector.MarkSet;
import org.eclipse.equinox.internal.p2.update.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * IMarkSetProvider implementation for the EclipseTouchPoint.
 */
public class MarkSetProvider implements IMarkSetProvider {
	private static final String ARTIFACT_CLASSIFIER_OSGI_BUNDLE = "osgi.bundle"; //$NON-NLS-1$
	private static final String ARTIFACT_CLASSIFIER_FEATURE = "org.eclipse.update.feature"; //$NON-NLS-1$

	private Collection artifactKeyList = null;

	public MarkSet[] getMarkSets(IProfile inProfile) {
		artifactKeyList = new HashSet();
		IArtifactRepository repositoryToGC = Util.getBundlePoolRepository(inProfile);
		addArtifactKeys(inProfile);
		IProfile currentProfile = getCurrentProfile();
		if (currentProfile != null && inProfile.getProfileId().equals(currentProfile.getProfileId())) {
			addRunningBundles(repositoryToGC);
			addRunningFeatures(inProfile, repositoryToGC);
		}
		return new MarkSet[] {new MarkSet((IArtifactKey[]) artifactKeyList.toArray(new IArtifactKey[0]), repositoryToGC)};
	}

	private void addRunningFeatures(IProfile profile, IArtifactRepository repositoryToGC) {
		try {
			List allFeatures = getAllFeatures(Configuration.load(new File(Util.getConfigurationFolder(profile), "org.eclipse.update/platform.xml"), null)); //$NON-NLS-1$
			for (Iterator iterator = allFeatures.iterator(); iterator.hasNext();) {
				Feature f = (Feature) iterator.next();
				IArtifactKey match = searchArtifact(f.getId(), new Version(f.getVersion()), ARTIFACT_CLASSIFIER_FEATURE, repositoryToGC);
				if (match != null)
					artifactKeyList.add(match);
			}
		} catch (ProvisionException e) {
			//Ignore the exception
		}
	}

	private List getAllFeatures(Configuration cfg) {
		if (cfg == null)
			return Collections.EMPTY_LIST;
		List sites = cfg.getSites();
		ArrayList result = new ArrayList();
		for (Iterator iterator = sites.iterator(); iterator.hasNext();) {
			Site object = (Site) iterator.next();
			Feature[] features = object.getFeatures();
			for (int i = 0; i < features.length; i++) {
				result.add(features[i]);
			}
		}
		return result;
	}

	private IProfile getCurrentProfile() {
		ServiceReference sr = Activator.getContext().getServiceReference(IProfileRegistry.class.getName());
		if (sr == null)
			return null;
		IProfileRegistry pr = (IProfileRegistry) Activator.getContext().getService(sr);
		if (pr == null)
			return null;
		Activator.getContext().ungetService(sr);
		return pr.getProfile(IProfileRegistry.SELF);
	}

	private void addArtifactKeys(IProfile aProfile) {
		Iterator installableUnits = aProfile.query(InstallableUnitQuery.ANY, new Collector(), null).iterator();
		while (installableUnits.hasNext()) {
			IArtifactKey[] keys = ((IInstallableUnit) installableUnits.next()).getArtifacts();
			if (keys == null)
				return;
			for (int i = 0; i < keys.length; i++) {
				artifactKeyList.add(keys[i]);
			}
		}
	}

	public IArtifactRepository getRepository(IProfile aProfile) {
		return Util.getBundlePoolRepository(aProfile);
	}

	private void addRunningBundles(IArtifactRepository repo) {
		artifactKeyList.addAll(findCorrespondinArtifacts(new WhatIsRunning().getBundlesBeingRun(), repo));
	}

	private IArtifactKey searchArtifact(String searchedId, Version searchedVersion, String classifier, IArtifactRepository repo) {
		//This is somewhat cheating since normally we should get the artifact key from the IUs that were representing the running system (e.g. we could get that info from the rollback repo)
		IArtifactKey[] keys = repo.getArtifactKeys();
		for (int i = 0; i < keys.length; i++) {
			if (keys[i].getClassifier().equals(classifier)) {
				String id = keys[i].getId();
				Version v = keys[i].getVersion();
				if (id != null && id.equals(searchedId) && v != null && v.equals(searchedVersion))
					return keys[i];
			}
		}
		return null;
	}

	//Find for each bundle info a corresponding artifact in repo 
	private ArrayList findCorrespondinArtifacts(BundleInfo[] bis, IArtifactRepository repo) {
		ArrayList toRetain = new ArrayList();
		for (int i = 0; i < bis.length; i++) {
			IArtifactKey match = searchArtifact(bis[i].getSymbolicName(), new Version(bis[i].getVersion()), ARTIFACT_CLASSIFIER_OSGI_BUNDLE, repo);
			if (match != null)
				toRetain.add(match);
		}
		return toRetain;
	}
}
