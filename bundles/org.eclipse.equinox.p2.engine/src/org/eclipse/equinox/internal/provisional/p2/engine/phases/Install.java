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

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class Install extends InstallableUnitPhase {

	final static class BeforeInstallEventAction extends ProvisioningAction {

		public IStatus execute(Map parameters) {
			IProfile profile = (IProfile) parameters.get(PARM_PROFILE);
			String phaseId = (String) parameters.get(PARM_PHASE_ID);
			InstallableUnitOperand operand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);
			((IProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), IProvisioningEventBus.SERVICE_NAME)).publishEvent(new InstallableUnitEvent(phaseId, true, profile, operand, InstallableUnitEvent.INSTALL, getTouchpoint()));
			return null;
		}

		public IStatus undo(Map parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			String phaseId = (String) parameters.get(PARM_PHASE_ID);
			InstallableUnitOperand operand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);
			IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
			profile.removeInstallableUnit(iu);
			((IProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), IProvisioningEventBus.SERVICE_NAME)).publishEvent(new InstallableUnitEvent(phaseId, false, profile, operand, InstallableUnitEvent.UNINSTALL, getTouchpoint()));
			return null;
		}
	}

	final static class AfterInstallEventAction extends ProvisioningAction {

		public IStatus execute(Map parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			String phaseId = (String) parameters.get(PARM_PHASE_ID);
			InstallableUnitOperand operand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);
			IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
			profile.addInstallableUnit(iu);
			((IProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), IProvisioningEventBus.SERVICE_NAME)).publishEvent(new InstallableUnitEvent(phaseId, false, profile, operand, InstallableUnitEvent.INSTALL, getTouchpoint()));
			return null;
		}

		public IStatus undo(Map parameters) {
			IProfile profile = (IProfile) parameters.get(PARM_PROFILE);
			String phaseId = (String) parameters.get(PARM_PHASE_ID);
			InstallableUnitOperand operand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);
			((IProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), IProvisioningEventBus.SERVICE_NAME)).publishEvent(new InstallableUnitEvent(phaseId, true, profile, operand, InstallableUnitEvent.UNINSTALL, getTouchpoint()));
			return null;
		}
	}

	private static final String PHASE_ID = "install"; //$NON-NLS-1$

	public Install(int weight) {
		super(PHASE_ID, weight);
	}

	protected boolean isApplicable(InstallableUnitOperand op) {
		return (op.second() != null && !op.second().equals(op.first()));
	}

	protected ProvisioningAction[] getActions(InstallableUnitOperand currentOperand) {
		//TODO: monitor.subTask(NLS.bind(Messages.Engine_Installing_IU, unit.getId()));

		ProvisioningAction beforeAction = new BeforeInstallEventAction();
		ProvisioningAction afterAction = new AfterInstallEventAction();

		IInstallableUnit unit = currentOperand.second();
		Touchpoint touchpoint = actionManager.getTouchpointPoint(unit.getTouchpointType());
		if (touchpoint != null) {
			beforeAction.setTouchpoint(touchpoint);
			afterAction.setTouchpoint(touchpoint);
		}

		if (unit.isFragment())
			return new ProvisioningAction[] {beforeAction, afterAction};

		ProvisioningAction[] parsedActions = getActions(unit, phaseId);
		if (parsedActions == null)
			return new ProvisioningAction[] {beforeAction, afterAction};

		ProvisioningAction[] actions = new ProvisioningAction[parsedActions.length + 2];
		actions[0] = beforeAction;
		System.arraycopy(parsedActions, 0, actions, 1, parsedActions.length);
		actions[actions.length - 1] = afterAction;
		return actions;
	}

	protected String getProblemMessage() {
		return Messages.Phase_Install_Error;
	}

	protected IStatus initializeOperand(IProfile profile, InstallableUnitOperand operand, Map parameters, IProgressMonitor monitor) {
		IInstallableUnit iu = operand.second();
		monitor.subTask(NLS.bind(Messages.Phase_Install_Task, iu.getId()));
		parameters.put(PARM_IU, iu);

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts != null && artifacts.length > 0)
			parameters.put(PARM_ARTIFACT, artifacts[0]);

		return Status.OK_STATUS;
	}
}
