/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.engine.*;

public class Install extends Phase {

	final static class BeforeInstallEventAction implements ITouchpointAction {
		public IStatus execute(Map parameters) {
			Profile profile = (Profile) parameters.get("profile");
			String phaseId = (String) parameters.get("phaseId");
			ITouchpoint touchpoint = (ITouchpoint) parameters.get("touchpoint");
			Operand operand = (Operand) parameters.get("operand");
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, true, profile, operand, InstallableUnitEvent.INSTALL, touchpoint));
			return null;
		}

		public IStatus undo(Map parameters) {
			Profile profile = (Profile) parameters.get("profile");
			String phaseId = (String) parameters.get("phaseId");
			ITouchpoint touchpoint = (ITouchpoint) parameters.get("touchpoint");
			Operand operand = (Operand) parameters.get("operand");
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, false, profile, operand, InstallableUnitEvent.UNINSTALL, touchpoint));
			return null;
		}
	}

	final static class AfterInstallEventAction implements ITouchpointAction {
		public IStatus execute(Map parameters) {
			Profile profile = (Profile) parameters.get("profile");
			String phaseId = (String) parameters.get("phaseId");
			ITouchpoint touchpoint = (ITouchpoint) parameters.get("touchpoint");
			Operand operand = (Operand) parameters.get("operand");
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, false, profile, operand, InstallableUnitEvent.INSTALL, touchpoint));
			return null;
		}

		public IStatus undo(Map parameters) {
			Profile profile = (Profile) parameters.get("profile");
			String phaseId = (String) parameters.get("phaseId");
			ITouchpoint touchpoint = (ITouchpoint) parameters.get("touchpoint");
			Operand operand = (Operand) parameters.get("operand");
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, true, profile, operand, InstallableUnitEvent.UNINSTALL, touchpoint));
			return null;
		}
	}

	private static final String PHASE_ID = "install"; //$NON-NLS-1$

	public Install(int weight) {
		super(PHASE_ID, weight, Messages.Engine_Install_Phase);
	}

	//	protected IStatus performOperand(EngineSession session, Profile profile, Operand operand, IProgressMonitor monitor) {
	//		IInstallableUnit unit = operand.second();
	//
	//		monitor.subTask(NLS.bind(Messages.Engine_Installing_IU, unit.getId()));
	//
	//		ITouchpoint touchpoint = TouchpointManager.getInstance().getTouchpoint(unit.getTouchpointType());
	//		if (!touchpoint.supports(PHASE_ID))
	//			return Status.OK_STATUS;
	//
	//		((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(PHASE_ID, true, profile, operand, InstallableUnitEvent.INSTALL, touchpoint));
	//
	//		ITouchpointAction[] actions = getActions();
	//		MultiStatus result = new MultiStatus();
	//		for (int i = 0; i < actions.length; i++) {
	//			IStatus actionStatus = (IStatus) actions[i].execute();
	//			result.add(actionStatus);
	//			if (!actionStatus.isOK())
	//				return result;
	//
	//			session.record(actions[i]);
	//		}
	//		((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(PHASE_ID, false, profile, operand, InstallableUnitEvent.INSTALL, touchpoint, result));
	//		return result;
	//	}

	protected boolean isApplicable(Operand op) {
		if (op.second() != null)
			return true;
		return false;
	}

	protected ITouchpointAction[] getActions(ITouchpoint touchpoint, Profile profile, Operand currentOperand) {
		//TODO: monitor.subTask(NLS.bind(Messages.Engine_Installing_IU, unit.getId()));

		ITouchpointAction[] actions = new ITouchpointAction[3];
		actions[0] = new BeforeInstallEventAction();
		actions[1] = touchpoint.getAction("install");
		actions[2] = new AfterInstallEventAction();
		return actions;
	}
}