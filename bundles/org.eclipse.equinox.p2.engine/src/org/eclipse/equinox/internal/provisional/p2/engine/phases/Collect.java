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

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.DownloadManager;
import org.eclipse.equinox.internal.p2.engine.Messages;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.*;

/**
 * The goal of the collect phase is to ask the touchpoints if the artifacts associated with an IU need to be downloaded.
 */
public class Collect extends InstallableUnitPhase {
	private static final String PHASE_ID = "collect"; //$NON-NLS-1$

	public Collect(int weight) {
		super(PHASE_ID, weight);
		//re-balance work since postPerform will do almost all the time-consuming work
		prePerformWork = 0;
		mainPerformWork = 100;
		postPerformWork = 1000;
	}

	protected boolean isApplicable(InstallableUnitOperand op) {
		return (op.second() != null);
	}

	protected ProvisioningAction[] getActions(InstallableUnitOperand currentOperand) {
		ProvisioningAction action = getTouchpoint(currentOperand).getAction(phaseId);
		return new ProvisioningAction[] {action};
	}

	protected String getProblemMessage() {
		return Messages.Phase_Collect_Error;
	}

	protected IStatus completeInstallableUnitPhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		List artifactRequests = (List) parameters.get(PARM_ARTIFACT_REQUESTS);

		DownloadManager dm = new DownloadManager();
		for (Iterator it = artifactRequests.iterator(); it.hasNext();) {
			IArtifactRequest[] requests = (IArtifactRequest[]) it.next();
			dm.add(requests);
		}
		return dm.start(monitor);
	}

	protected IStatus initializeInstallableUnitPhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		parameters.put(PARM_ARTIFACT_REQUESTS, new ArrayList());
		return null;
	}
}
