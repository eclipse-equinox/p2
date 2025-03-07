/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Wind River - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine.phases;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.PhaseSetFactory;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.engine.spi.Touchpoint;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.util.NLS;

public class Configure extends InstallableUnitPhase {

	final static class BeforeConfigureEventAction extends ProvisioningAction {

		@Override
		public IStatus execute(Map<String, Object> parameters) {
			IProfile profile = (IProfile) parameters.get(PARM_PROFILE);
			String phaseId = (String) parameters.get(PARM_PHASE_ID);
			IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
			IProvisioningAgent agent = (IProvisioningAgent) parameters.get(PARM_AGENT);
			agent.getService(IProvisioningEventBus.class).publishEvent(new InstallableUnitEvent(phaseId, true, profile,
					iu, InstallableUnitEvent.CONFIGURE, getTouchpoint()));
			return null;
		}

		@Override
		public IStatus undo(Map<String, Object> parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			String phaseId = (String) parameters.get(PARM_PHASE_ID);
			IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
			IProvisioningAgent agent = (IProvisioningAgent) parameters.get(PARM_AGENT);
			agent.getService(IProvisioningEventBus.class).publishEvent(new InstallableUnitEvent(phaseId, false, profile,
					iu, InstallableUnitEvent.UNCONFIGURE, getTouchpoint()));
			return null;
		}
	}

	final static class AfterConfigureEventAction extends ProvisioningAction {

		@Override
		public IStatus execute(Map<String, Object> parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			String phaseId = (String) parameters.get(PARM_PHASE_ID);
			IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
			IProvisioningAgent agent = (IProvisioningAgent) parameters.get(PARM_AGENT);
			agent.getService(IProvisioningEventBus.class).publishEvent(new InstallableUnitEvent(phaseId, false, profile,
					iu, InstallableUnitEvent.CONFIGURE, getTouchpoint()));
			return null;
		}

		@Override
		public IStatus undo(Map<String, Object> parameters) {
			IProfile profile = (IProfile) parameters.get(PARM_PROFILE);
			String phaseId = (String) parameters.get(PARM_PHASE_ID);
			IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
			IProvisioningAgent agent = (IProvisioningAgent) parameters.get(PARM_AGENT);
			agent.getService(IProvisioningEventBus.class).publishEvent(new InstallableUnitEvent(phaseId, true, profile,
					iu, InstallableUnitEvent.UNCONFIGURE, getTouchpoint()));
			return null;
		}
	}

	public Configure(int weight) {
		super(PhaseSetFactory.PHASE_CONFIGURE, weight);
	}

	@Override
	protected boolean isApplicable(InstallableUnitOperand op) {
		return (op.second() != null);
	}

	@Override
	protected List<ProvisioningAction> getActions(InstallableUnitOperand currentOperand) {
		IInstallableUnit unit = currentOperand.second();

		ProvisioningAction beforeAction = new BeforeConfigureEventAction();
		ProvisioningAction afterAction = new AfterConfigureEventAction();
		Touchpoint touchpoint = getActionManager().getTouchpointPoint(unit.getTouchpointType());
		if (touchpoint != null) {
			beforeAction.setTouchpoint(touchpoint);
			afterAction.setTouchpoint(touchpoint);
		}
		ArrayList<ProvisioningAction> actions = new ArrayList<>();
		actions.add(beforeAction);
		if (!QueryUtil.isFragment(unit)) {
			List<ProvisioningAction> parsedActions = getActions(unit, phaseId);
			if (parsedActions != null) {
				actions.addAll(parsedActions);
			}
		}
		actions.add(afterAction);
		return actions;
	}

	@Override
	protected String getProblemMessage() {
		return Messages.Phase_Configure_Error;
	}

	@Override
	protected IStatus initializeOperand(IProfile profile, InstallableUnitOperand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
		IInstallableUnit iu = operand.second();
		monitor.subTask(NLS.bind(Messages.Phase_Configure_Task, iu.getId()));
		parameters.put(PARM_IU, iu);

		Collection<IArtifactKey> artifacts = iu.getArtifacts();
		if (artifacts != null && artifacts.size() > 0) {
			parameters.put(PARM_ARTIFACT, artifacts.iterator().next());
		}

		return Status.OK_STATUS;
	}
}
