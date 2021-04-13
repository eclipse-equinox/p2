/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.engine.phases.CheckTrust;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;

/**
 * This action collects the set of bundle files on which the signature trust
 * check should be performed. The actual trust checking is done by the
 * CheckTrust phase.
 */
public class CheckTrustAction extends ProvisioningAction {

	public static final String ID = "checkTrust"; //$NON-NLS-1$

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);
		if (iu == null)
			return null;
		IProvisioningAgent agent = (IProvisioningAgent) parameters.get(ActionConstants.PARM_AGENT);
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		// if the IU is already in the profile there is nothing to do
		if (!profile.available(QueryUtil.createIUQuery(iu), null).isEmpty())
			return null;
		@SuppressWarnings("unchecked")
		Map<IArtifactDescriptor, File> bundleFiles = (Map<IArtifactDescriptor, File>) parameters
				.get(CheckTrust.PARM_ARTIFACTS);
		Collection<IArtifactKey> artifacts = iu.getArtifacts();
		if (artifacts == null) {
			return null;
		}
		IFileArtifactRepository repo = Util.getAggregatedBundleRepository(agent, profile);
		for (IArtifactKey key : artifacts) {
			for (IArtifactDescriptor descriptor : repo.getArtifactDescriptors(key)) {
				IFileArtifactRepository currentRepo = descriptor.getRepository() instanceof IFileArtifactRepository
						? (IFileArtifactRepository) descriptor.getRepository()
						: repo;
				File artifactFile = currentRepo.getArtifactFile(descriptor);
				bundleFiles.put(descriptor, artifactFile);
			}
		}
		return null;
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}
}
