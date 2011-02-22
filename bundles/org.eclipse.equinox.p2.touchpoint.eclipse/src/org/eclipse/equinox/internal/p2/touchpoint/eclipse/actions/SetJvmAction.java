/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *    IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.io.File;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

/**
 * Touchpoint action which allows the user to set the -vm parameter in the 
 * eclipse.ini file.
 *
 */
public class SetJvmAction extends ProvisioningAction {
	public static final String ID = "setJvm"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.spi.ProvisioningAction#execute(java.util.Map)
	 */
	public IStatus execute(Map<String, Object> parameters) {
		String jvmArg = (String) parameters.get(ActionConstants.PARM_JVM);
		if (jvmArg == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_JVM, ID));
		LauncherData launcherData = ((Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR)).getLauncherData();
		File previous = launcherData.getJvm();
		File jvm = "null".equals(jvmArg) ? null : new File(jvmArg); //$NON-NLS-1$
		// make a backup - even if it is null 
		getMemento().put(ActionConstants.PARM_PREVIOUS_VALUE, previous == null ? null : previous.getPath());
		launcherData.setJvm(jvm);
		return Status.OK_STATUS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.spi.ProvisioningAction#undo(java.util.Map)
	 */
	public IStatus undo(Map<String, Object> parameters) {
		String jvmArg = (String) parameters.get(ActionConstants.PARM_JVM);
		if (jvmArg == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_JVM, ID));
		// make a backup - even if it is null 
		String previous = (String) getMemento().get(ActionConstants.PARM_PREVIOUS_VALUE);
		LauncherData launcherData = ((Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR)).getLauncherData();
		launcherData.setJvm(previous == null ? null : new File(previous));
		return Status.OK_STATUS;
	}

}