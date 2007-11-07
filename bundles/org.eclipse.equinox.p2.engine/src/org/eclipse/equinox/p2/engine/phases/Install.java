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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.osgi.util.NLS;

public class Install extends Phase {

	final static class BeforeInstallEventAction extends ProvisioningAction {
		public IStatus execute(Map parameters) {
			Profile profile = (Profile) parameters.get("profile");
			String phaseId = (String) parameters.get("phaseId");
			Touchpoint touchpoint = (Touchpoint) parameters.get("touchpoint");
			Operand operand = (Operand) parameters.get("operand");
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, true, profile, operand, InstallableUnitEvent.INSTALL, touchpoint));
			return null;
		}

		public IStatus undo(Map parameters) {
			Profile profile = (Profile) parameters.get("profile");
			String phaseId = (String) parameters.get("phaseId");
			Touchpoint touchpoint = (Touchpoint) parameters.get("touchpoint");
			Operand operand = (Operand) parameters.get("operand");
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, false, profile, operand, InstallableUnitEvent.UNINSTALL, touchpoint));
			return null;
		}
	}

	final static class AfterInstallEventAction extends ProvisioningAction {
		public IStatus execute(Map parameters) {
			Profile profile = (Profile) parameters.get("profile");
			String phaseId = (String) parameters.get("phaseId");
			Touchpoint touchpoint = (Touchpoint) parameters.get("touchpoint");
			Operand operand = (Operand) parameters.get("operand");
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, false, profile, operand, InstallableUnitEvent.INSTALL, touchpoint));
			return null;
		}

		public IStatus undo(Map parameters) {
			Profile profile = (Profile) parameters.get("profile");
			String phaseId = (String) parameters.get("phaseId");
			Touchpoint touchpoint = (Touchpoint) parameters.get("touchpoint");
			Operand operand = (Operand) parameters.get("operand");
			((ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName())).publishEvent(new InstallableUnitEvent(phaseId, true, profile, operand, InstallableUnitEvent.UNINSTALL, touchpoint));
			return null;
		}
	}

	private static final String PHASE_ID = "install"; //$NON-NLS-1$

	public Install(int weight) {
		super(PHASE_ID, weight, Messages.Engine_Install_Phase);
	}

	protected boolean isApplicable(Operand op) {
		return (op.second() != null);
	}

	protected ProvisioningAction[] getActions(Operand currentOperand) {
		//TODO: monitor.subTask(NLS.bind(Messages.Engine_Installing_IU, unit.getId()));

		ProvisioningAction beforeAction = new BeforeInstallEventAction();
		ProvisioningAction afterAction = new AfterInstallEventAction();

		IInstallableUnit unit = currentOperand.second();
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

	protected IStatus initializeOperand(Profile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
		IResolvedInstallableUnit iu = operand.second();
		monitor.subTask(NLS.bind("Installing {0}", iu.getId()));
		parameters.put("iu", iu);

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts != null && artifacts.length > 0)
			parameters.put("artifact", artifacts[0]);

		return Status.OK_STATUS;
	}
}