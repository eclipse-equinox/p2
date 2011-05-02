/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
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
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;

public class SetLauncherNameAction extends ProvisioningAction {
	public static final String ID = "setLauncherName"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		getMemento().put(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME, profile.getProperty(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME));
		String launcherName = (String) parameters.get(ActionConstants.PARM_LAUNCHERNAME);
		setLauncher(manipulator, profile, launcherName);
		return Status.OK_STATUS;
	}

	public IStatus undo(Map<String, Object> parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		String previousLauncherName = (String) getMemento().get(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME);
		setLauncher(manipulator, profile, previousLauncherName);
		return Status.OK_STATUS;
	}

	private static void setLauncher(Manipulator manipulator, IProfile profile, String launcherName) {
		//Get the launcherData before changing the name so we don't lose anything from the old launcher.ini
		LauncherData launcherData = manipulator.getLauncherData();
		if (launcherName != null)
			((Profile) profile).setProperty(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME, launcherName);
		else
			((Profile) profile).removeProperty(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME);
		launcherData.setLauncher(Util.getLauncherPath(profile));
	}
}