/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.util.Collection;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Util;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;

public class CollectAction extends ProvisioningAction {

	public static final String ACTION_COLLECT = "collect"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		IProvisioningAgent agent = (IProvisioningAgent) parameters.get(ActionConstants.PARM_AGENT);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(ActionConstants.PARM_IU);
		try {
			IArtifactRequest[] requests = collect(agent, iu, profile);
			@SuppressWarnings("unchecked")
			Collection<IArtifactRequest[]> artifactRequests = (Collection<IArtifactRequest[]>) parameters.get(ActionConstants.PARM_ARTIFACT_REQUESTS);
			artifactRequests.add(requests);
		} catch (ProvisionException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	public IStatus undo(Map<String, Object> parameters) {
		// nothing to do for now
		return Status.OK_STATUS;
	}

	IArtifactRequest[] collect(IProvisioningAgent agent, IInstallableUnit installableUnit, IProfile profile) throws ProvisionException {
		Collection<IArtifactKey> toDownload = installableUnit.getArtifacts();
		if (toDownload == null)
			return new IArtifactRequest[0];
		IArtifactRepository destination = Util.getDownloadCacheRepo(agent);
		IArtifactRequest[] requests = new IArtifactRequest[toDownload.size()];
		int count = 0;
		for (IArtifactKey key : toDownload) {
			//TODO Here there are cases where the download is not necessary again because what needs to be done is just a configuration step
			requests[count++] = Util.getArtifactRepositoryManager(agent).createMirrorRequest(key, destination, null, null, profile.getProperty(IProfile.PROP_STATS_PARAMETERS));
		}

		if (requests.length == count)
			return requests;
		IArtifactRequest[] result = new IArtifactRequest[count];
		System.arraycopy(requests, 0, result, 0, count);
		return result;
	}
}