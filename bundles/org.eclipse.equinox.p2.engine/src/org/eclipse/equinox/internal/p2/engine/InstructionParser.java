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
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.internal.provisional.p2.engine.*;

public class InstructionParser {

	Phase phase;
	Touchpoint touchpoint;

	public InstructionParser(Phase phase, Touchpoint touchpoint) {
		Assert.isNotNull(phase);
		Assert.isNotNull(touchpoint);
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
			throw new IllegalArgumentException("No action found for " + actionId + '.'); //$NON-NLS-1$

		return action;
	}
}
