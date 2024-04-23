/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitPhase;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Util;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.*;

public class CollectAction extends ProvisioningAction {

	public static final String ACTION_COLLECT = "collect"; //$NON-NLS-1$

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		IProvisioningAgent agent = (IProvisioningAgent) parameters.get(ActionConstants.PARM_AGENT);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(ActionConstants.PARM_IU);
		try {
			List<IArtifactRequest> requests = collect(agent, iu, profile);
			if (!requests.isEmpty()) {
				@SuppressWarnings("unchecked")
				Collection<IArtifactRequest[]> artifactRequests = (Collection<IArtifactRequest[]>) parameters
						.get(InstallableUnitPhase.PARM_ARTIFACT_REQUESTS);
				artifactRequests.add(requests.toArray(IArtifactRequest[]::new));
			}
		} catch (ProvisionException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		// nothing to do for now
		return Status.OK_STATUS;
	}

	private List<IArtifactRequest> collect(IProvisioningAgent agent, IInstallableUnit installableUnit, IProfile profile)
			throws ProvisionException {
		Collection<IArtifactKey> toDownload = installableUnit.getArtifacts();
		if (toDownload == null || toDownload.isEmpty()) {
			return List.of();
		}
		IArtifactRepository destination = Util.getDownloadCacheRepo(agent);
		IArtifactRepositoryManager manager = Util.getArtifactRepositoryManager(agent);
		List<IArtifactRequest> requests = new ArrayList<>(toDownload.size());
		for (IArtifactKey key : toDownload) {
			// TODO Here there are cases where the download is not necessary again because
			// what needs to be done is just a configuration step
			requests.add(manager.createMirrorRequest(key, destination, null, null,
					profile.getProperty(IProfile.PROP_STATS_PARAMETERS)));
		}

		return requests;
	}
}