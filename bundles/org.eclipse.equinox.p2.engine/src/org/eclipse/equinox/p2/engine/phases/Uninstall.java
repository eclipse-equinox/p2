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

public class Uninstall extends Phase {

	final static class BeforeUninstallEventAction extends ProvisioningAction {
		public IStatus execute(Map parameters) {
			Profile profile = (Profile) parameters.get("profile");
			String phaseId = (String) parameters.get("phaseId");
			Touchpoint touchpoint = (Touchpoint) parameters.get("touchpoint");
			Operand operand = (Operand) parameters.get("operand");
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, true, profile, operand, InstallableUnitEvent.UNINSTALL, touchpoint));
			return null;
		}

		public IStatus undo(Map parameters) {
			Profile profile = (Profile) parameters.get("profile");
			String phaseId = (String) parameters.get("phaseId");
			Touchpoint touchpoint = (Touchpoint) parameters.get("touchpoint");
			Operand operand = (Operand) parameters.get("operand");
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, false, profile, operand, InstallableUnitEvent.INSTALL, touchpoint));
			return null;
		}
	}

	final static class AfterUninstallEventAction extends ProvisioningAction {
		public IStatus execute(Map parameters) {
			Profile profile = (Profile) parameters.get("profile");
			String phaseId = (String) parameters.get("phaseId");
			Touchpoint touchpoint = (Touchpoint) parameters.get("touchpoint");
			Operand operand = (Operand) parameters.get("operand");
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, false, profile, operand, InstallableUnitEvent.UNINSTALL, touchpoint));
			return null;
		}

		public IStatus undo(Map parameters) {
			Profile profile = (Profile) parameters.get("profile");
			String phaseId = (String) parameters.get("phaseId");
			Touchpoint touchpoint = (Touchpoint) parameters.get("touchpoint");
			Operand operand = (Operand) parameters.get("operand");
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, true, profile, operand, InstallableUnitEvent.INSTALL, touchpoint));
			return null;
		}
	}

	private static final String PHASE_ID = "uninstall"; //$NON-NLS-1$

	public Uninstall(int weight) {
		super(PHASE_ID, weight, Messages.Engine_Uninstall_Phase);
	}

	//	protected IStatus performOperand(EngineSession session, Profile profile, Operand operand, IProgressMonitor monitor) {
	//		IInstallableUnit unit = operand.first();
	//
	//		monitor.subTask(NLS.bind(Messages.Engine_Uninstalling_IU, unit.getId()));
	//
	//		Touchpoint touchpoint = TouchpointManager.getInstance().getTouchpoint(unit.getTouchpointType());
	//		if (!touchpoint.supports(PHASE_ID))
	//			return Status.OK_STATUS;
	//
	//		((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(PHASE_ID, true, profile, operand, InstallableUnitEvent.UNINSTALL, touchpoint));
	//
	//		//TODO need to protect the actual operation on a try / catch to ensure the delivery of event. 
	//		ProvisioningAction[] actions = touchpoint.getActions(PHASE_ID, profile, operand);
	//		MultiStatus result = new MultiStatus();
	//		for (int i = 0; i < actions.length; i++) {
	//			result.add((IStatus) actions[i].execute());
	//			session.record(actions[i]);
	//		}
	//
	//		((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(PHASE_ID, false, profile, operand, InstallableUnitEvent.UNINSTALL, touchpoint, result));
	//		return result;
	//	}

	protected boolean isApplicable(Operand op) {
		if (op.first() != null)
			return true;
		return false;
	}

	protected ProvisioningAction[] getActions(Touchpoint touchpoint, Profile profile, Operand currentOperand) {
		//TODO: monitor.subTask(NLS.bind(Messages.Engine_Uninstalling_IU, unit.getId()));

		ProvisioningAction[] actions = new ProvisioningAction[3];
		actions[0] = new BeforeUninstallEventAction();
		actions[1] = touchpoint.getAction("uninstall");
		actions[2] = new AfterUninstallEventAction();
		return actions;
	}
}