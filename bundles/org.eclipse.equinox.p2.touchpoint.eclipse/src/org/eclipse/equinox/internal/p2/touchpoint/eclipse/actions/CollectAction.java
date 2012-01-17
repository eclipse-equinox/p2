/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.osgi.util.NLS;

public class CollectAction extends ProvisioningAction {
	public static final String ID = "collect"; //$NON-NLS-1$
	public static final String ARTIFACT_FOLDER = "artifact.folder"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		IProvisioningAgent agent = (IProvisioningAgent) parameters.get(ActionConstants.PARM_AGENT);
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(ActionConstants.PARM_IU);
		IArtifactRequest[] requests;
		try {
			requests = CollectAction.collect(agent, profile, iu);
		} catch (ProvisionException e) {
			return e.getStatus();
		}

		@SuppressWarnings("unchecked")
		Collection<IArtifactRequest[]> artifactRequests = (Collection<IArtifactRequest[]>) parameters.get(ActionConstants.PARM_ARTIFACT_REQUESTS);
		artifactRequests.add(requests);
		return Status.OK_STATUS;
	}

	public IStatus undo(Map<String, Object> parameters) {
		// nothing to do for now
		return Status.OK_STATUS;
	}

	public static boolean isZipped(Collection<ITouchpointData> data) {
		if (data == null || data.size() == 0)
			return false;
		for (ITouchpointData td : data) {
			if (td.getInstruction("zipped") != null) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	public static Map<String, String> createArtifactDescriptorProperties(IInstallableUnit installableUnit) {
		Map<String, String> descriptorProperties = null;
		if (CollectAction.isZipped(installableUnit.getTouchpointData())) {
			descriptorProperties = new HashMap<String, String>();
			descriptorProperties.put(CollectAction.ARTIFACT_FOLDER, Boolean.TRUE.toString());
		}
		return descriptorProperties;
	}

	// TODO: Here we may want to consult multiple caches
	static IArtifactRequest[] collect(IProvisioningAgent agent, IProfile profile, IInstallableUnit installableUnit) throws ProvisionException {
		Collection<IArtifactKey> toDownload = installableUnit.getArtifacts();
		if (toDownload == null || toDownload.size() == 0)
			return IArtifactRepositoryManager.NO_ARTIFACT_REQUEST;

		IArtifactRepository aggregatedRepositoryView = Util.getAggregatedBundleRepository(agent, profile);
		IArtifactRepository bundlePool = Util.getBundlePoolRepository(agent, profile);
		if (bundlePool == null)
			throw new ProvisionException(Util.createError(NLS.bind(Messages.no_bundle_pool, profile.getProfileId())));

		List<IArtifactRequest> requests = new ArrayList<IArtifactRequest>();
		for (IArtifactKey key : toDownload) {
			if (!aggregatedRepositoryView.contains(key)) {
				Map<String, String> repositoryProperties = CollectAction.createArtifactDescriptorProperties(installableUnit);
				requests.add(Util.getArtifactRepositoryManager(agent).createMirrorRequest(key, bundlePool, null, repositoryProperties, profile.getProperty(IProfile.PROP_STATS_PARAMETERS)));
			}
		}

		if (requests.isEmpty())
			return IArtifactRepositoryManager.NO_ARTIFACT_REQUEST;

		IArtifactRequest[] result = requests.toArray(new IArtifactRequest[requests.size()]);
		return result;
	}
}