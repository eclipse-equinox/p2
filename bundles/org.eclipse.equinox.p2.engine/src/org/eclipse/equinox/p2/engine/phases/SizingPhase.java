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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.p2.engine.TouchpointManager;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.core.helpers.MultiStatus;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class SizingPhase extends IUPhase {
	private static final String TP_DATA = "collect"; //$NON-NLS-1$

	private Set artifactsToObtain;
	private long sizeOnDisk;
	private long dlSize;

	public SizingPhase(int weight, String phaseName) {
		super(weight, phaseName);
	}

	protected boolean isApplicable(Operand op) {
		return true;
	}

	protected IStatus performOperand(EngineSession session, Profile profile, Operand operand, IProgressMonitor monitor) {
		IInstallableUnit unit = operand.second();

		if (unit != null) {
			monitor.subTask(NLS.bind(Messages.Engine_Collecting_For_IU, unit.getId()));

			// TODO: Need do progress reporting

			// Ask all the touchpoints if they need to download an artifact
			ITouchpoint touchpoint = TouchpointManager.getInstance().getTouchpoint(unit.getTouchpointType());
			if (touchpoint.supports(TP_DATA)) {
				ITouchpointAction[] actions = touchpoint.getActions(TP_DATA, profile, operand);
				for (int i = 0; i < actions.length; i++) {
					Object result = actions[i].execute();
					if (result != null) {
						IArtifactRequest[] requests = (IArtifactRequest[]) result;
						for (int j = 0; j < requests.length; j++) {
							artifactsToObtain.add(requests[j]);
						}
					}
				}
			}

			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
		}

		return Status.OK_STATUS;
	}

	protected void postPerform(MultiStatus status, Profile profile, Operand[] deltas, IProgressMonitor monitor) {
		IArtifactRepositoryManager repoMgr = (IArtifactRepositoryManager) ServiceHelper.getService(EngineActivator.getContext(), IArtifactRepositoryManager.class.getName());
		IArtifactRepository[] repositories = repoMgr.getKnownRepositories();

		for (Iterator iterator = artifactsToObtain.iterator(); iterator.hasNext();) {
			for (int i = 0; i < repositories.length; i++) {
				IArtifactDescriptor[] descriptors = repositories[i].getArtifactDescriptors(((IArtifactRequest) iterator.next()).getArtifactKey());
				if (descriptors.length > 0) {
					if (descriptors[0].getProperty(IArtifactDescriptor.ARTIFACT_SIZE) != null)
						sizeOnDisk += Long.parseLong(descriptors[0].getProperty(IArtifactDescriptor.ARTIFACT_SIZE));

					if (descriptors[0].getProperty(IArtifactDescriptor.DOWNLOAD_SIZE) != null)
						dlSize += Long.parseLong(descriptors[0].getProperty(IArtifactDescriptor.DOWNLOAD_SIZE));

					break;
				}
			}
		}
	}

	protected void prePerform(MultiStatus status, Profile profile, Operand[] deltas, IProgressMonitor monitor) {
		artifactsToObtain = new HashSet(deltas.length);
		sizeOnDisk = 0;
		dlSize = 0;
	}

	public long getDiskSize() {
		return sizeOnDisk;
	}

	public long getDlSize() {
		return dlSize;
	}
}
