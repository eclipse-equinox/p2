/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.engine.phases;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.engine.*;

public class SizingPhase extends Phase {
	private static final String TP_DATA = "collect"; //$NON-NLS-1$

	private long sizeOnDisk;
	private long dlSize;

	public SizingPhase(int weight, String phaseName) {
		super(TP_DATA, weight, phaseName);
	}

	protected boolean isApplicable(Operand op) {
		if (op.second() != null)
			return true;
		return false;
	}

	//	protected IStatus performOperand(EngineSession session, Profile profile, Operand operand, IProgressMonitor monitor) {
	//		IInstallableUnit unit = operand.second();
	//
	//		if (unit != null) {
	//			monitor.subTask(NLS.bind(Messages.Engine_Collecting_For_IU, unit.getId()));
	//
	//			// TODO: Need do progress reporting
	//
	//			// Ask all the touchpoints if they need to download an artifact
	//			Touchpoint touchpoint = TouchpointManager.getInstance().getTouchpoint(unit.getTouchpointType());
	//			if (touchpoint.supports(TP_DATA)) {
	//				ProvisioningAction[] actions = touchpoint.getActions(TP_DATA, profile, operand);
	//				for (int i = 0; i < actions.length; i++) {
	//					Object result = actions[i].execute();
	//					if (result != null) {
	//						IArtifactRequest[] requests = (IArtifactRequest[]) result;
	//						for (int j = 0; j < requests.length; j++) {
	//							artifactsToObtain.add(requests[j]);
	//						}
	//					}
	//				}
	//			}
	//
	//			if (monitor.isCanceled())
	//				return Status.CANCEL_STATUS;
	//		}
	//
	//		return Status.OK_STATUS;
	//	}
	//
	//	protected void postPerform(MultiStatus status, Profile profile, Operand[] deltas, IProgressMonitor monitor) {
	//		IArtifactRepositoryManager repoMgr = (IArtifactRepositoryManager) ServiceHelper.getService(EngineActivator.getContext(), IArtifactRepositoryManager.class.getName());
	//		IArtifactRepository[] repositories = repoMgr.getKnownRepositories();
	//
	//		for (Iterator iterator = artifactsToObtain.iterator(); iterator.hasNext();) {
	//			for (int i = 0; i < repositories.length; i++) {
	//				IArtifactDescriptor[] descriptors = repositories[i].getArtifactDescriptors(((IArtifactRequest) iterator.next()).getArtifactKey());
	//				if (descriptors.length > 0) {
	//					if (descriptors[0].getProperty(IArtifactDescriptor.ARTIFACT_SIZE) != null)
	//						sizeOnDisk += Long.parseLong(descriptors[0].getProperty(IArtifactDescriptor.ARTIFACT_SIZE));
	//
	//					if (descriptors[0].getProperty(IArtifactDescriptor.DOWNLOAD_SIZE) != null)
	//						dlSize += Long.parseLong(descriptors[0].getProperty(IArtifactDescriptor.DOWNLOAD_SIZE));
	//
	//					break;
	//				}
	//			}
	//		}
	//	}
	//
	//	protected void prePerform(MultiStatus status, Profile profile, Operand[] deltas, IProgressMonitor monitor) {
	//		artifactsToObtain = new HashSet(deltas.length);
	//		sizeOnDisk = 0;
	//		dlSize = 0;
	//	}

	public long getDiskSize() {
		return sizeOnDisk;
	}

	public long getDlSize() {
		return dlSize;
	}

	protected ProvisioningAction[] getActions(Touchpoint touchpoint, Profile profile, Operand currentOperand) {
		ProvisioningAction action = touchpoint.getAction("collect");
		return new ProvisioningAction[] {action};
	}

	protected IStatus completePhase(IProgressMonitor monitor, Profile profile, Map parameters) {
		List artifactRequests = (List) parameters.get("artifactRequests");
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
		IArtifactRepository[] repositories = repoMgr.getKnownRepositories();

		for (Iterator iterator = artifactsToObtain.iterator(); iterator.hasNext();) {
			IArtifactRequest artifactRequest = (IArtifactRequest) iterator.next();
			for (int i = 0; i < repositories.length; i++) {
				IArtifactDescriptor[] descriptors = repositories[i].getArtifactDescriptors(artifactRequest.getArtifactKey());
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
		parameters.put("artifactRequests", new ArrayList());
		return null;
	}
}
