/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;

public class ParameterizedProvisioningAction extends ProvisioningAction {

	private ProvisioningAction action;
	private Map actionParameters;

	public ParameterizedProvisioningAction(ProvisioningAction action, Map actionParameters) {
		if (action == null || actionParameters == null)
			throw new IllegalArgumentException(Messages.ParameterizedProvisioningAction_action_or_parameters_null);
		this.action = action;
		this.actionParameters = actionParameters;
	}

	public IStatus execute(Map parameters) {
		parameters = processActionParameters(parameters);
		return action.execute(parameters);
	}

	public IStatus undo(Map parameters) {
		parameters = processActionParameters(parameters);
		return action.undo(parameters);
	}

	private Map processActionParameters(Map parameters) {
		Map result = new HashMap(parameters);
		for (Iterator it = actionParameters.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			String name = (String) entry.getKey();
			String value = processVariables((String) entry.getValue(), parameters);
			result.put(name, value);
		}
		return Collections.unmodifiableMap(result);
	}

	private String processVariables(String parameterValue, Map parameters) {

		int variableBeginIndex = parameterValue.indexOf("${"); //$NON-NLS-1$
		if (variableBeginIndex == -1)
			return parameterValue;

		int variableEndIndex = parameterValue.indexOf('}', variableBeginIndex + 2);
		if (variableEndIndex == -1)
			return parameterValue;

		String preVariable = parameterValue.substring(0, variableBeginIndex);
		String variableName = parameterValue.substring(variableBeginIndex + 2, variableEndIndex);
		Object value = parameters.get(variableName);

		// try to replace this parameter with a character
		if (value == null && variableName.charAt(0) == '#') {
			try {
				int code = Integer.parseInt(variableName.substring(1));
				if (code >= 0 && code < 65536)
					value = Character.toString((char) code);
			} catch (Throwable t) {
				// ignore and leave value as null
			}
		}

		String variableValue = value == null ? "" : value.toString(); //$NON-NLS-1$
		String postVariable = processVariables(parameterValue.substring(variableEndIndex + 1), parameters);
		return preVariable + variableValue + postVariable;
	}
}
