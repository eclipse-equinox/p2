/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.engine.phases;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.ITouchpointType;
import org.eclipse.equinox.p2.engine.IPhaseSet;

/**
 * An install phase that checks if the certificates used to sign the artifacts
 * being installed are from a trusted source.
 */
public class CheckTrust extends InstallableUnitPhase {

	private static final String PHASE_ID = IPhaseSet.PHASE_CHECK_TRUST;
	public static final String PARM_ARTIFACT_FILES = "artifactFiles"; //$NON-NLS-1$

	public CheckTrust(int weight) {
		super(PHASE_ID, weight);
	}

	protected boolean isApplicable(InstallableUnitOperand op) {
		return (op.second() != null);
	}

	protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		Collection artifactRequests = (Collection) parameters.get(PARM_ARTIFACT_FILES);
		EngineSession session = (EngineSession) parameters.get(PARM_SESSION);

		// Instantiate a check trust manager
		CertificateChecker certificateChecker = new CertificateChecker(session);
		certificateChecker.add(artifactRequests.toArray());
		IStatus status = certificateChecker.start();

		return status;
	}

	protected ProvisioningAction[] getActions(InstallableUnitOperand operand) {
		IInstallableUnit unit = operand.second();
		ProvisioningAction[] parsedActions = getActions(unit, phaseId);
		if (parsedActions != null)
			return parsedActions;

		ITouchpointType type = unit.getTouchpointType();
		if (type == null || type == ITouchpointType.NONE)
			return null;

		String actionId = getActionManager().getTouchpointQualifiedActionId(phaseId, type);
		ProvisioningAction action = getActionManager().getAction(actionId, null);
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
		parameters.put(PARM_ARTIFACT_FILES, new ArrayList());
		return super.initializePhase(monitor, profile, parameters);
	}

}
