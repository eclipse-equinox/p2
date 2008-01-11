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
package org.eclipse.equinox.p2.engine.phases;

import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.p2.engine.Messages;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;

public class Sizing extends Phase {
	private static final String PHASE_ID = "collect"; //$NON-NLS-1$

	private long sizeOnDisk;
	private long dlSize;

	public Sizing(int weight, String phaseName) {
		super(PHASE_ID, weight);
	}

	protected boolean isApplicable(Operand op) {
		return (op.second() != null);
	}

	public long getDiskSize() {
		return sizeOnDisk;
	}

	public long getDlSize() {
		return dlSize;
	}

	protected ProvisioningAction[] getActions(Operand currentOperand) {
		ProvisioningAction action = getTouchpoint(currentOperand).getAction("collect"); //$NON-NLS-1$
		return new ProvisioningAction[] {action};
	}

	protected String getProblemMessage() {
		return Messages.Phase_Sizing_Error;
	}

	protected IStatus completePhase(IProgressMonitor monitor, Profile profile, Map parameters) {
		List artifactRequests = (List) parameters.get("artifactRequests"); //$NON-NLS-1$
		Set artifactsToObtain = new HashSet(artifactRequests.size());

		for (Iterator it = artifactRequests.iterator(); it.hasNext();) {
			IArtifactRequest[] requests = (IArtifactRequest[]) it.next();
			if (requests == null)
				continue;
			for (int i = 0; i < requests.length; i++) {
				artifactsToObtain.add(requests[i]);
			}
		}

		IArtifactRepositoryManager repoMgr = (IArtifactRepositoryManager) ServiceHelper.getService(EngineActivator.getContext(), IArtifactRepositoryManager.class.getName());
		URL[] repositories = repoMgr.getKnownRepositories(IArtifactRepositoryManager.REPOSITORIES_ALL);

		for (Iterator iterator = artifactsToObtain.iterator(); iterator.hasNext();) {
			IArtifactRequest artifactRequest = (IArtifactRequest) iterator.next();
			for (int i = 0; i < repositories.length; i++) {
				IArtifactRepository repo;
				try {
					repo = repoMgr.loadRepository(repositories[i], null);
				} catch (ProvisionException e) {
					continue;//skip unresponsive repositories
				}
				IArtifactDescriptor[] descriptors = repo.getArtifactDescriptors(artifactRequest.getArtifactKey());
				if (descriptors.length > 0) {
					if (descriptors[0].getProperty(IArtifactDescriptor.ARTIFACT_SIZE) != null)
						sizeOnDisk += Long.parseLong(descriptors[0].getProperty(IArtifactDescriptor.ARTIFACT_SIZE));
					if (descriptors[0].getProperty(IArtifactDescriptor.DOWNLOAD_SIZE) != null)
						dlSize += Long.parseLong(descriptors[0].getProperty(IArtifactDescriptor.DOWNLOAD_SIZE));
					break;
				}
			}
		}
		return null;
	}

	protected IStatus initializePhase(IProgressMonitor monitor, Profile profile, Map parameters) {
		parameters.put(PARM_ARTIFACT_REQUESTS, new ArrayList());
		return null;
	}
}
