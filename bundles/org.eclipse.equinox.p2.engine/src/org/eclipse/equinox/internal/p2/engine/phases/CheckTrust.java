/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.internal.p2.engine.phases;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitPhase;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.PhaseSetFactory;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ITouchpointType;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

/**
 * An install phase that checks if the certificates used to sign the artifacts
 * being installed are from a trusted source.
 */
public class CheckTrust extends InstallableUnitPhase {

	/**
	 * Parameter used to populate/get artifacts to check trust. The value for this
	 * property is <code>Map&lt;IArtifactDescriptor, File></code>.
	 *
	 * @see org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.CheckTrustAction
	 */
	public static final String PARM_ARTIFACTS = "artifacts"; //$NON-NLS-1$

	public CheckTrust(int weight) {
		super(PhaseSetFactory.PHASE_CHECK_TRUST, weight);
	}

	@Override
	protected boolean isApplicable(InstallableUnitOperand op) {
		return (op.second() != null);
	}

	@Override
	protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
		@SuppressWarnings("unchecked")
		Map<IArtifactDescriptor, File> artifactRequests = (Map<IArtifactDescriptor, File>) parameters
				.get(PARM_ARTIFACTS);
		IProvisioningAgent agent = (IProvisioningAgent) parameters.get(PARM_AGENT);

		// Instantiate a check trust manager
		CertificateChecker certificateChecker = new CertificateChecker(agent);
		certificateChecker.add(artifactRequests);
		certificateChecker.setProfile(profile);
		return certificateChecker.start();
	}

	@Override
	protected List<ProvisioningAction> getActions(InstallableUnitOperand operand) {
		IInstallableUnit unit = operand.second();
		List<ProvisioningAction> parsedActions = getActions(unit, phaseId);
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
		return Collections.singletonList(action);
	}

	@Override
	protected IStatus initializeOperand(IProfile profile, InstallableUnitOperand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
		IInstallableUnit iu = operand.second();
		parameters.put(PARM_IU, iu);

		return super.initializeOperand(profile, operand, parameters, monitor);
	}

	@Override
	protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
		parameters.put(PARM_ARTIFACTS, new HashMap<>());
		return super.initializePhase(monitor, profile, parameters);
	}

}
