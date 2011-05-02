/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class RemoveProgramArgumentAction extends ProvisioningAction {
	public static final String ID = "removeProgramArg"; //$NON-NLS-1$
	public static final String EMPTY_ARGUMENT = ""; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		String programArg = (String) parameters.get(ActionConstants.PARM_PROGRAM_ARG);
		if (programArg == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_PROGRAM_ARG, ID));

		if (programArg.startsWith("-")) {//$NON-NLS-1$
			List<String> programArgs = Arrays.asList(manipulator.getLauncherData().getProgramArgs());

			int index = programArgs.indexOf(programArg);
			if (index == -1)
				return Status.OK_STATUS;

			index++; // move index to potential programArgValue
			if (programArgs.size() > index) {
				String programArgValue = programArgs.get(index);
				if (!programArgValue.startsWith("-")) //$NON-NLS-1$
					getMemento().put(ActionConstants.PARM_PROGRAM_ARG_VALUE, programArgValue);
			}
			manipulator.getLauncherData().removeProgramArg(programArg);
		}

		return Status.OK_STATUS;
	}

	public IStatus undo(Map<String, Object> parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		String programArg = (String) parameters.get(ActionConstants.PARM_PROGRAM_ARG);
		if (programArg == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_PROGRAM_ARG, ID));

		if (programArg.startsWith("-")) {//$NON-NLS-1$ {
			manipulator.getLauncherData().addProgramArg(programArg);

			String programArgValue = (String) getMemento().get(ActionConstants.PARM_PROGRAM_ARG_VALUE);
			if (programArgValue != null)
				manipulator.getLauncherData().addProgramArg(programArgValue);
		}
		return Status.OK_STATUS;
	}

}