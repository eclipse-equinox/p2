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
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.equinox.internal.provisional.p2.metadata.ITouchpointInstruction;
import org.eclipse.equinox.internal.provisional.p2.metadata.ITouchpointType;
import org.eclipse.osgi.util.NLS;

public class InstructionParser {

	public class ActionEntry {

		protected final VersionRange versionRange;
		protected final String actionId;

		public ActionEntry(String actionId, VersionRange versionRange) {
			this.actionId = actionId;
			this.versionRange = versionRange;
		}
	}

	private static final String VERSION_EQUALS = "version="; //$NON-NLS-1$
	private ActionManager actionManager;

	public InstructionParser(ActionManager actionManager) {
		Assert.isNotNull(actionManager);
		this.actionManager = actionManager;
	}

	public ProvisioningAction[] parseActions(ITouchpointInstruction instruction, ITouchpointType touchpointType) {
		List actions = new ArrayList();
		Map importMap = parseImportAttribute(instruction.getImportAttribute());
		StringTokenizer tokenizer = new StringTokenizer(instruction.getBody(), ";"); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			actions.add(parseAction(tokenizer.nextToken(), importMap, touchpointType));
		}

		return (ProvisioningAction[]) actions.toArray(new ProvisioningAction[actions.size()]);
	}

	private Map parseImportAttribute(String importAttribute) {
		if (importAttribute == null)
			return Collections.EMPTY_MAP;

		Map result = new HashMap();
		StringTokenizer tokenizer = new StringTokenizer(importAttribute, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			StringTokenizer actionTokenizer = new StringTokenizer(tokenizer.nextToken(), ";"); //$NON-NLS-1$
			String actionId = actionTokenizer.nextToken().trim();
			int lastDot = actionId.lastIndexOf('.');
			String actionKey = (lastDot == -1) ? actionId : actionId.substring(lastDot + 1);
			VersionRange actionVersionRange = null;
			while (actionTokenizer.hasMoreTokens()) {
				String actionAttribute = actionTokenizer.nextToken().trim();
				if (actionAttribute.startsWith(VERSION_EQUALS))
					actionVersionRange = new VersionRange(actionAttribute.substring(VERSION_EQUALS.length() + 1));
			}
			result.put(actionKey, new ActionEntry(actionId, actionVersionRange));
			result.put(actionId, new ActionEntry(actionId, actionVersionRange));
		}
		return result;
	}

	private ProvisioningAction parseAction(String statement, Map qualifier, ITouchpointType touchpointType) {
		int openBracket = statement.indexOf('(');
		int closeBracket = statement.lastIndexOf(')');
		if (openBracket == -1 || closeBracket == -1 || openBracket > closeBracket)
			throw new IllegalArgumentException(NLS.bind(Messages.action_syntax_error, statement));
		String actionName = statement.substring(0, openBracket).trim();
		ProvisioningAction action = lookupAction(actionName, qualifier, touchpointType);

		String nameValuePairs = statement.substring(openBracket + 1, closeBracket);
		if (nameValuePairs.length() == 0)
			return new ParameterizedProvisioningAction(action, Collections.EMPTY_MAP);

		StringTokenizer tokenizer = new StringTokenizer(nameValuePairs, ","); //$NON-NLS-1$
		Map parameters = new HashMap();
		while (tokenizer.hasMoreTokens()) {
			String nameValuePair = tokenizer.nextToken();
			int colonIndex = nameValuePair.indexOf(":"); //$NON-NLS-1$
			if (colonIndex == -1)
				throw new IllegalArgumentException(NLS.bind(Messages.action_syntax_error, statement));
			String name = nameValuePair.substring(0, colonIndex).trim();
			String value = nameValuePair.substring(colonIndex + 1).trim();
			parameters.put(name, value);
		}
		return new ParameterizedProvisioningAction(action, parameters);
	}

	private ProvisioningAction lookupAction(String actionId, Map importMap, ITouchpointType touchpointType) {
		VersionRange versionRange = null;
		ActionEntry actionEntry = (ActionEntry) importMap.get(actionId);
		if (actionEntry != null) {
			actionId = actionEntry.actionId;
			versionRange = actionEntry.versionRange;
		}

		ProvisioningAction action = (actionId.indexOf('.') != -1) ? actionManager.getAction(actionId, versionRange) : actionManager.getTouchpointQualifiedAction(actionId, touchpointType);
		if (action == null)
			throw new IllegalArgumentException(NLS.bind(Messages.action_not_found, actionId));

		return action;
	}
}
