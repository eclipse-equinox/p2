/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.ParameterizedProvisioningAction;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ParameterizedProvisioningActionTest extends AbstractProvisioningTest {

	boolean passTest = false;
	String value;
	ProvisioningAction action = new ProvisioningAction() {

		@Override
		public IStatus execute(Map<String, Object> parameters) {
			value = (String) parameters.get("test");
			return null;
		}

		@Override
		public IStatus undo(Map<String, Object> parameters) {
			return null;
		}
	};

	public ParameterizedProvisioningActionTest(String name) {
		super(name);
	}

	public void testBasicParameter() {
		value = null;

		Map<String, String> actionParameters = new HashMap<>();
		actionParameters.put("test", "testValue");

		Map<String, Object> phaseParameters = new HashMap<>();
		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);

		assertEquals("testValue", value);
	}

	public void testVariableParameter() {
		passTest = false;

		Map<String, String> actionParameters = new HashMap<>();
		actionParameters.put("test", "test${variable}");

		Map<String, Object> phaseParameters = new HashMap<>();
		phaseParameters.put("variable", "Value");
		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("testValue", value);
	}

	public void testEscapedCharacterParameter() {
		passTest = false;

		Map<String, String> actionParameters = new HashMap<>();
		actionParameters.put("test", "testV${#97}lue");

		Map<String, Object> phaseParameters = new HashMap<>();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("testValue", value);
	}

	public void testOutOfRangeEscapedCharacterParameter() {
		passTest = false;

		Map<String, String> actionParameters = new HashMap<>();
		actionParameters.put("test", "${#999999999999999999999999999999999999999999999}");

		Map<String, Object> phaseParameters = new HashMap<>();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("", value);
	}

	public void testLargerThanCharEscapedCharacterParameter() {
		passTest = false;

		Map<String, String> actionParameters = new HashMap<>();
		actionParameters.put("test", "testV${#65633}lue"); // #65633 should be "a" if we allow overflow but we do not so null

		Map<String, Object> phaseParameters = new HashMap<>();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("testVlue", value);
	}

	public void testNegativeOutOfRangeCharEscapedCharacterParameter() {
		passTest = false;

		Map<String, String> actionParameters = new HashMap<>();
		actionParameters.put("test", "testV${#-65439}lue"); // #65633 should be "a" if we allow underflow but we do not so null

		Map<String, Object> phaseParameters = new HashMap<>();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("testVlue", value);
	}

	public void testNotNumberEscapedCharacterParameter() {
		passTest = false;

		Map<String, String> actionParameters = new HashMap<>();
		actionParameters.put("test", "${#xFFFF}");

		Map<String, Object> phaseParameters = new HashMap<>();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("", value);
	}

	public void testNaughtyEscapedCharactersParameter() {
		passTest = false;

		Map<String, String> actionParameters = new HashMap<>();
		actionParameters.put("test", "${#36} ${#44} ${#58} ${#59} ${#123} ${#125}");

		Map<String, Object> phaseParameters = new HashMap<>();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("$ , : ; { }", value);
	}

	public void testNullCharEscapedCharactersParameter() {
		passTest = false;

		Map<String, String> actionParameters = new HashMap<>();
		actionParameters.put("test", "a${#0}b");

		Map<String, Object> phaseParameters = new HashMap<>();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("a\0b", value);
	}

	public void testMaxCharEscapedCharactersParameter() {
		passTest = false;

		Map<String, String> actionParameters = new HashMap<>();
		actionParameters.put("test", "a${#65535}b");

		Map<String, Object> phaseParameters = new HashMap<>();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("a\uFFFFb", value);
	}

	public void testOverMaxCharEscapedCharactersParameter() {
		passTest = false;

		Map<String, String> actionParameters = new HashMap<>();
		actionParameters.put("test", "a${#65536}b");

		Map<String, Object> phaseParameters = new HashMap<>();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("ab", value);
	}

	public void testUnderMinCharEscapedCharactersParameter() {
		passTest = false;

		Map<String, String> actionParameters = new HashMap<>();
		actionParameters.put("test", "a${#-1}b");

		Map<String, Object> phaseParameters = new HashMap<>();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("ab", value);
	}

}
