/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class RemoveProgramPropertyAction extends ProvisioningAction {
	public static final String ID = "removeProgramProperty"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.spi.ProvisioningAction#execute(java.util.Map)
	 */
	public IStatus execute(Map<String, Object> parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		String propName = (String) parameters.get(ActionConstants.PARM_PROP_NAME);
		if (propName == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_PROP_NAME, ID));
		String propValue = (String) parameters.get(ActionConstants.PARM_PROP_VALUE);

		ConfigData data = manipulator.getConfigData();
		String previous = data.getProperty(propName);
		if (previous == null)
			return Status.OK_STATUS;
		// make a backup - even if it is null 
		getMemento().put(ActionConstants.PARM_PREVIOUS_VALUE, previous);
		// if the value is null, remove the key/value pair. 
		if (propValue == null) {
			data.setProperty(propName, null);
			return Status.OK_STATUS;
		}
		// Otherwise treat the current value as a comma-separated list and remove 
		// just the one value that was specified.
		List<String> list = AddProgramPropertyAction.convertToList(previous);
		// if the value wasn't in the list, then just return
		if (!list.remove(propValue))
			return Status.OK_STATUS;
		// otherwise set the property to the new value, or remove it if it is now empty
		propValue = list.isEmpty() ? null : AddProgramPropertyAction.convertToString(list);
		data.setProperty(propName, propValue);
		return Status.OK_STATUS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.spi.ProvisioningAction#undo(java.util.Map)
	 */
	public IStatus undo(Map<String, Object> parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		String propName = (String) parameters.get(ActionConstants.PARM_PROP_NAME);
		if (propName == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_PROP_NAME, ID));
		String previous = (String) getMemento().get(ActionConstants.PARM_PREVIOUS_VALUE);
		manipulator.getConfigData().setProperty(propName, previous);
		return Status.OK_STATUS;
	}

}
