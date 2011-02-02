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

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class AddProgramPropertyAction extends ProvisioningAction {
	public static final String ID = "addProgramProperty"; //$NON-NLS-1$

	// treat the given string as a comma-separated list and parse and 
	// convert it to a real list
	protected static List<String> convertToList(String value) {
		List<String> result = new ArrayList<String>();
		for (StringTokenizer tokenizer = new StringTokenizer(value, ","); tokenizer.hasMoreTokens();) //$NON-NLS-1$
			result.add(tokenizer.nextToken());
		return result;
	}

	// convert the given list to a comma-separated string
	protected static String convertToString(List<String> list) {
		StringBuffer buffer = new StringBuffer();
		for (Iterator<String> iter = list.iterator(); iter.hasNext();) {
			buffer.append(iter.next());
			if (iter.hasNext())
				buffer.append(',');
		}
		return buffer.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.spi.ProvisioningAction#execute(java.util.Map)
	 */
	public IStatus execute(Map<String, Object> parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		String propName = (String) parameters.get(ActionConstants.PARM_PROP_NAME);
		if (propName == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_PROP_NAME, ID));
		String propValue = (String) parameters.get(ActionConstants.PARM_PROP_VALUE);
		if (propValue == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_PROP_VALUE, ID));
		if (propValue != null && propValue.equals(ActionConstants.PARM_AT_ARTIFACT)) {
			try {
				propValue = Util.resolveArtifactParam(parameters);
			} catch (CoreException e) {
				return e.getStatus();
			}
		}

		// if there was no value previously, then just set our key/value pair and return.
		// otherwise we have to merge. 
		ConfigData data = manipulator.getConfigData();
		String previous = data.getProperty(propName);
		// make a backup - even if it is null 
		getMemento().put(ActionConstants.PARM_PREVIOUS_VALUE, previous);
		// assume the value is a comma-separated list and just add ourselves to the end
		if (previous != null)
			propValue = previous + ',' + propValue;
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
