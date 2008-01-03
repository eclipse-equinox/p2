package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.ProvisioningAction;

public class ParameterizedProvisioningAction extends ProvisioningAction {

	private ProvisioningAction action;
	private Map actionParameters;

	public ParameterizedProvisioningAction(ProvisioningAction action, Map actionParameters) {
		if (action == null || actionParameters == null)
			throw new IllegalArgumentException("Both action and action pararameters must not be null."); //$NON-NLS-1$
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
		String variableValue = parameters.get(variableName).toString();
		String postVariable = processVariables(parameterValue.substring(variableEndIndex + 1), parameters);
		return preVariable + variableValue + postVariable;
	}
}
