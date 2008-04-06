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
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class RemoveJVMArgumentAction extends ProvisioningAction {
	public static final String ID = "removeJvmArg"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		String jvmArg = (String) parameters.get(ActionConstants.PARM_JVM_ARG);
		if (jvmArg == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_JVM_ARG, ID));
		manipulator.getLauncherData().removeJvmArg(jvmArg);
		return Status.OK_STATUS;
	}

	public IStatus undo(Map parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		String jvmArg = (String) parameters.get(ActionConstants.PARM_JVM_ARG);
		if (jvmArg == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_JVM_ARG, ID));
		manipulator.getLauncherData().addJvmArg(jvmArg);
		return Status.OK_STATUS;
	}
}