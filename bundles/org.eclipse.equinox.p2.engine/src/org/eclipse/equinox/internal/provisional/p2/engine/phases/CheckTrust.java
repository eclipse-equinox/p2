/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

/**
 * An install phase that checks if the certificates used to sign the artifacts
 * being installed are from a trusted source.
 */
public class CheckTrust extends InstallableUnitPhase {

	private static final String PHASE_ID = "checkTrust"; //$NON-NLS-1$

	public CheckTrust(int weight) {
		super(PHASE_ID, weight);
	}

	protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		Collection artifactRequests = (Collection) parameters.get(PARM_ARTIFACT_REQUESTS);

		// Instantiate a check trust manager
		CertificateChecker certificateChecker = new CertificateChecker();
		certificateChecker.add(artifactRequests.toArray());
		IStatus status = certificateChecker.start();

		return status;
	}

	protected ProvisioningAction[] getActions(InstallableUnitOperand currentOperand) {
		ProvisioningAction action = getTouchpoint(currentOperand).getAction(phaseId);
		if (action == null) {
			return null;
		}
		return new ProvisioningAction[] {action};
	}

	protected IStatus initializeOperand(IProfile profile, InstallableUnitOperand operand, Map parameters, IProgressMonitor monitor) {
		IInstallableUnit iu = operand.second();
		parameters.put(PARM_IU, iu);

		return super.initializeOperand(profile, operand, parameters, monitor);
	}

	protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		parameters.put(PARM_ARTIFACT_REQUESTS, new ArrayList());
		return super.initializePhase(monitor, profile, parameters);
	}

}
