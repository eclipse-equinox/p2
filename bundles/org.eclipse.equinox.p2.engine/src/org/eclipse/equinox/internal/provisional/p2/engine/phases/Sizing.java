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
package org.eclipse.equinox.internal.provisional.p2.engine.phases;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.ITouchpointType;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;

public class Sizing extends InstallableUnitPhase {
	private static final String PHASE_ID = "sizing"; //$NON-NLS-1$
	private static final String COLLECT_PHASE_ID = "collect"; //$NON-NLS-1$

	private long sizeOnDisk;
	private long dlSize;

	public Sizing(int weight, String phaseName) {
		super(PHASE_ID, weight);
	}

	protected boolean isApplicable(InstallableUnitOperand op) {
		return (op.second() != null && !op.second().equals(op.first()));
	}

	public long getDiskSize() {
		return sizeOnDisk;
	}

	public long getDlSize() {
		return dlSize;
	}

	protected ProvisioningAction[] getActions(InstallableUnitOperand operand) {
		IInstallableUnit unit = operand.second();
		ProvisioningAction[] parsedActions = getActions(unit, COLLECT_PHASE_ID);
		if (parsedActions != null)
			return parsedActions;

		ITouchpointType type = unit.getTouchpointType();
		if (type == null || type == ITouchpointType.NONE)
			return null;

		String actionId = getActionManager().getTouchpointQualifiedActionId(COLLECT_PHASE_ID, type);
		ProvisioningAction action = getActionManager().getAction(actionId, null);
		if (action == null) {
			return null;
		}
		return new ProvisioningAction[] {action};
	}

	protected String getProblemMessage() {
		return Messages.Phase_Sizing_Error;
	}

	protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		List artifactRequests = (List) parameters.get(Collect.PARM_ARTIFACT_REQUESTS);
		ProvisioningContext context = (ProvisioningContext) parameters.get(PARM_CONTEXT);
		int statusCode = 0;

		Set artifactsToObtain = new HashSet(artifactRequests.size());

		for (Iterator it = artifactRequests.iterator(); it.hasNext();) {
			IArtifactRequest[] requests = (IArtifactRequest[]) it.next();
			if (requests == null)
				continue;
			for (int i = 0; i < requests.length; i++) {
				artifactsToObtain.add(requests[i]);
			}
		}

		if (monitor.isCanceled())
			return Status.CANCEL_STATUS;

		IArtifactRepositoryManager repoMgr = (IArtifactRepositoryManager) ServiceHelper.getService(EngineActivator.getContext(), IArtifactRepositoryManager.class.getName());
		URI[] repositories = null;
		if (context == null || context.getArtifactRepositories() == null)
			repositories = repoMgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		else
			repositories = context.getArtifactRepositories();

		for (Iterator iterator = artifactsToObtain.iterator(); iterator.hasNext() && !monitor.isCanceled();) {
			IArtifactRequest artifactRequest = (IArtifactRequest) iterator.next();
			boolean found = false;
			for (int i = 0; i < repositories.length; i++) {
				IArtifactRepository repo;
				try {
					repo = repoMgr.loadRepository(repositories[i], monitor);
				} catch (ProvisionException e) {
					continue;//skip unresponsive repositories
				}
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				IArtifactDescriptor[] descriptors = repo.getArtifactDescriptors(artifactRequest.getArtifactKey());
				if (descriptors.length > 0) {
					if (descriptors[0].getProperty(IArtifactDescriptor.ARTIFACT_SIZE) != null)
						sizeOnDisk += Long.parseLong(descriptors[0].getProperty(IArtifactDescriptor.ARTIFACT_SIZE));
					else
						statusCode = ProvisionException.ARTIFACT_INCOMPLETE_SIZING;
					if (descriptors[0].getProperty(IArtifactDescriptor.DOWNLOAD_SIZE) != null)
						dlSize += Long.parseLong(descriptors[0].getProperty(IArtifactDescriptor.DOWNLOAD_SIZE));
					else
						statusCode = ProvisionException.ARTIFACT_INCOMPLETE_SIZING;
					found = true;
					break;
				}
			}
			if (!found)
				// The artifact wasn't present in any repository
				return new Status(IStatus.ERROR, EngineActivator.ID, ProvisionException.ARTIFACT_NOT_FOUND, Messages.Phase_Sizing_Error, null);
		}
		if (statusCode != 0)
			return new Status(IStatus.WARNING, EngineActivator.ID, statusCode, Messages.Phase_Sizing_Warning, null);
		return null;
	}

	protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		sizeOnDisk = 0;
		dlSize = 0;
		parameters.put(Collect.PARM_ARTIFACT_REQUESTS, new ArrayList());
		return null;
	}
}
