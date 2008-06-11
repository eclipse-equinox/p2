/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
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
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.osgi.util.NLS;

public class CollectAction extends ProvisioningAction {
	public static final String ID = "collect"; //$NON-NLS-1$
	public static final String ARTIFACT_FOLDER = "artifact.folder"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		InstallableUnitOperand operand = (InstallableUnitOperand) parameters.get(ActionConstants.PARM_OPERAND);
		IArtifactRequest[] requests;
		try {
			requests = CollectAction.collect(operand.second(), profile);
		} catch (ProvisionException e) {
			return e.getStatus();
		}

		Collection artifactRequests = (Collection) parameters.get(ActionConstants.PARM_ARTIFACT_REQUESTS);
		artifactRequests.add(requests);
		return Status.OK_STATUS;
	}

	public IStatus undo(Map parameters) {
		// nothing to do for now
		return Status.OK_STATUS;
	}

	public static boolean isZipped(TouchpointData[] data) {
		if (data == null || data.length == 0)
			return false;
		for (int i = 0; i < data.length; i++) {
			if (data[i].getInstructions("zipped") != null) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	public static Properties createArtifactDescriptorProperties(IInstallableUnit installableUnit) {
		Properties descriptorProperties = null;
		if (CollectAction.isZipped(installableUnit.getTouchpointData())) {
			descriptorProperties = new Properties();
			descriptorProperties.setProperty(CollectAction.ARTIFACT_FOLDER, Boolean.TRUE.toString());
		}
		return descriptorProperties;
	}

	// TODO: Here we may want to consult multiple caches
	public static IArtifactRequest[] collect(IInstallableUnit installableUnit, IProfile profile) throws ProvisionException {
		IArtifactKey[] toDownload = installableUnit.getArtifacts();
		if (toDownload == null || toDownload.length == 0)
			return IArtifactRepositoryManager.NO_ARTIFACT_REQUEST;

		IArtifactRepository aggregatedRepositoryView = Util.getAggregatedBundleRepository(profile);
		IArtifactRepository bundlePool = Util.getBundlePoolRepository(profile);
		if (bundlePool == null)
			throw new ProvisionException(Util.createError(NLS.bind(Messages.no_bundle_pool, profile.getProfileId())));

		List requests = new ArrayList();
		for (int i = 0; i < toDownload.length; i++) {
			IArtifactKey key = toDownload[i];
			if (!aggregatedRepositoryView.contains(key)) {
				Properties repositoryProperties = CollectAction.createArtifactDescriptorProperties(installableUnit);
				requests.add(Util.getArtifactRepositoryManager().createMirrorRequest(key, bundlePool, null, repositoryProperties));
			}
		}

		if (requests.isEmpty())
			return IArtifactRepositoryManager.NO_ARTIFACT_REQUEST;

		IArtifactRequest[] result = (IArtifactRequest[]) requests.toArray(new IArtifactRequest[requests.size()]);
		return result;
	}
}