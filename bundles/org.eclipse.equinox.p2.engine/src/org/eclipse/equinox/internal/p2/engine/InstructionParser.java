package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import org.eclipse.equinox.p2.engine.*;

public class InstructionParser {

	Phase phase;
	Touchpoint touchpoint;

	public InstructionParser(Phase phase, Touchpoint touchpoint) {
		this.phase = phase;
		this.touchpoint = touchpoint;
	}

	public ProvisioningAction[] parseActions(String instruction) {
		List actions = new ArrayList();
		StringTokenizer tokenizer = new StringTokenizer(instruction, ";"); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			actions.add(parseAction(tokenizer.nextToken()));
		}

		return (ProvisioningAction[]) actions.toArray(new ProvisioningAction[actions.size()]);
	}

	private ProvisioningAction parseAction(String statement) {
		int openBracket = statement.indexOf('(');
		int closeBracket = statement.lastIndexOf(')');
		String actionName = statement.substring(0, openBracket).trim();
		ProvisioningAction action = lookupAction(actionName);

		String nameValuePairs = statement.substring(openBracket + 1, closeBracket);
		StringTokenizer tokenizer = new StringTokenizer(nameValuePairs, ","); //$NON-NLS-1$
		Map parameters = new HashMap();
		while (tokenizer.hasMoreTokens()) {
			String nameValuePair = tokenizer.nextToken();
			int colonIndex = nameValuePair.indexOf(":"); //$NON-NLS-1$
			String name = nameValuePair.substring(0, colonIndex).trim();
			String value = nameValuePair.substring(colonIndex + 1).trim();
			parameters.put(name, value);
		}
		return new ParameterizedProvisioningAction(action, parameters);
	}

	private ProvisioningAction lookupAction(String actionId) {

		ProvisioningAction action = phase.getAction(actionId);
		if (action == null)
			action = touchpoint.getAction(actionId);

		if (action == null)
			throw new IllegalArgumentException("No action found for " + actionId + "."); //$NON-NLS-2$

		return action;
	}
}
