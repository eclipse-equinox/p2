/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;

public class SetLauncherNameAction extends ProvisioningAction {
	public static final String ID = "setLauncherName"; //$NON-NLS-1$

	private IStatus changeName(String newName, Manipulator manipulator, Profile profile) {
		//force the load to make sure we read the values in the old filename
		IStatus status = EclipseTouchpoint.loadManipulator(manipulator);
		if (status != null && !status.isOK())
			return status;
		getMemento().put(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME, profile.getProperty(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME));
		profile.setProperty(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME, newName);
		manipulator.getLauncherData().setLauncher(Util.getLauncherPath(profile));
		return Status.OK_STATUS;
	}

	public IStatus execute(Map parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		Profile profile = (Profile) parameters.get(ActionConstants.PARM_PROFILE);
		return changeName((String) parameters.get(ActionConstants.PARM_LAUNCHERNAME), manipulator, profile);
	}

	public IStatus undo(Map parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		Profile profile = (Profile) parameters.get(ActionConstants.PARM_PROFILE);
		return changeName((String) getMemento().get(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME), manipulator, profile);
	}
}