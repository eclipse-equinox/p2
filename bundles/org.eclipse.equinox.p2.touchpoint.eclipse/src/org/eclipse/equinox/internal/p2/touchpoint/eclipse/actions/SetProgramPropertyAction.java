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

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class SetProgramPropertyAction extends ProvisioningAction {
	public static final String ID = "setProgramProperty"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		String propName = (String) parameters.get(ActionConstants.PARM_PROP_NAME);
		if (propName == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_PROP_NAME, ID));
		String propValue = (String) parameters.get(ActionConstants.PARM_PROP_VALUE);
		if (propValue != null && propValue.equals(ActionConstants.PARM_AT_ARTIFACT)) {
			try {
				propValue = Util.resolveArtifactParam(parameters);
			} catch (CoreException e) {
				return e.getStatus();
			}
		}

		getMemento().put(ActionConstants.PARM_PREVIOUS_VALUE, manipulator.getConfigData().getProperty(propName));
		manipulator.getConfigData().setProperty(propName, propValue);
		return Status.OK_STATUS;
	}

	public IStatus undo(Map<String, Object> parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		String propName = (String) parameters.get(ActionConstants.PARM_PROP_NAME);
		if (propName == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_PROP_NAME, ID));
		String previousValue = (String) getMemento().get(ActionConstants.PARM_PREVIOUS_VALUE);
		manipulator.getConfigData().setProperty(propName, previousValue);
		return Status.OK_STATUS;
	}
}