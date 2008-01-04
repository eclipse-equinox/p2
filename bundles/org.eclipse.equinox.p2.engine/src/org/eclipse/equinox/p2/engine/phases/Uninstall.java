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

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.p2.engine.Messages;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class Uninstall extends Phase {

	final static class BeforeUninstallEventAction extends ProvisioningAction {
		public IStatus execute(Map parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE); 
			String phaseId = (String) parameters.get(PARM_PHASE_ID); 
			Touchpoint touchpoint = (Touchpoint) parameters.get(PARM_TOUCHPOINT); 
			Operand operand = (Operand) parameters.get(PARM_OPERAND); 
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, true, profile, operand, InstallableUnitEvent.UNINSTALL, touchpoint));
			return null;
		}

		public IStatus undo(Map parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE); 
			String phaseId = (String) parameters.get(PARM_PHASE_ID); 
			Touchpoint touchpoint = (Touchpoint) parameters.get(PARM_TOUCHPOINT); 
			Operand operand = (Operand) parameters.get(PARM_OPERAND); 
			IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU); 
			profile.addInstallableUnit(iu);
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, false, profile, operand, InstallableUnitEvent.INSTALL, touchpoint));
			return null;
		}
	}

	final static class AfterUninstallEventAction extends ProvisioningAction {
		public IStatus execute(Map parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE); 
			String phaseId = (String) parameters.get(PARM_PHASE_ID); 
			Touchpoint touchpoint = (Touchpoint) parameters.get(PARM_TOUCHPOINT); 
			Operand operand = (Operand) parameters.get(PARM_OPERAND); 
			IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU); 
			profile.removeInstallableUnit(iu);
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, false, profile, operand, InstallableUnitEvent.UNINSTALL, touchpoint));
			return null;
		}

		public IStatus undo(Map parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE); 
			String phaseId = (String) parameters.get(PARM_PHASE_ID); 
			Touchpoint touchpoint = (Touchpoint) parameters.get(PARM_TOUCHPOINT); 
			Operand operand = (Operand) parameters.get(PARM_OPERAND); 
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, true, profile, operand, InstallableUnitEvent.INSTALL, touchpoint));
			return null;
		}
	}

	private static final String PHASE_ID = "uninstall"; //$NON-NLS-1$

	public Uninstall(int weight) {
		super(PHASE_ID, weight);
	}

	protected boolean isApplicable(Operand op) {
		return (op.first() != null);
	}

	protected ProvisioningAction[] getActions(Operand currentOperand) {
		//TODO: monitor.subTask(NLS.bind(Messages.Engine_Uninstalling_IU, unit.getId()));

		ProvisioningAction beforeAction = new BeforeUninstallEventAction();
		ProvisioningAction afterAction = new AfterUninstallEventAction();

		IInstallableUnit unit = currentOperand.first();
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
		return Messages.Phase_Uninstall_Error;
	}

	protected IStatus initializeOperand(Profile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
		IInstallableUnit iu = operand.first();
		parameters.put("iu", iu); //$NON-NLS-1$

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts != null && artifacts.length > 0)
			parameters.put("artifact", artifacts[0]); //$NON-NLS-1$

		return Status.OK_STATUS;
	}
}
