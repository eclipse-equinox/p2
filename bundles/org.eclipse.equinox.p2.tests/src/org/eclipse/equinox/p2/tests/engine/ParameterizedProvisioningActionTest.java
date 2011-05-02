/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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

		public IStatus execute(Map parameters) {
			value = (String) parameters.get("test");
			return null;
		}

		public IStatus undo(Map parameters) {
			return null;
		}
	};

	public ParameterizedProvisioningActionTest(String name) {
		super(name);
	}

	public void testBasicParameter() {
		value = null;

		Map actionParameters = new HashMap();
		actionParameters.put("test", "testValue");

		Map phaseParameters = new HashMap();
		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);

		assertEquals("testValue", value);
	}

	public void testVariableParameter() {
		passTest = false;

		Map actionParameters = new HashMap();
		actionParameters.put("test", "test${variable}");

		Map phaseParameters = new HashMap();
		phaseParameters.put("variable", "Value");
		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("testValue", value);
	}

	public void testEscapedCharacterParameter() {
		passTest = false;

		Map actionParameters = new HashMap();
		actionParameters.put("test", "testV${#97}lue");

		Map phaseParameters = new HashMap();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("testValue", value);
	}

	public void testOutOfRangeEscapedCharacterParameter() {
		passTest = false;

		Map actionParameters = new HashMap();
		actionParameters.put("test", "${#999999999999999999999999999999999999999999999}");

		Map phaseParameters = new HashMap();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("", value);
	}

	public void testLargerThanCharEscapedCharacterParameter() {
		passTest = false;

		Map actionParameters = new HashMap();
		actionParameters.put("test", "testV${#65633}lue"); // #65633 should be "a" if we allow overflow but we do not so null

		Map phaseParameters = new HashMap();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("testVlue", value);
	}

	public void testNegativeOutOfRangeCharEscapedCharacterParameter() {
		passTest = false;

		Map actionParameters = new HashMap();
		actionParameters.put("test", "testV${#-65439}lue"); // #65633 should be "a" if we allow underflow but we do not so null

		Map phaseParameters = new HashMap();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("testVlue", value);
	}

	public void testNotNumberEscapedCharacterParameter() {
		passTest = false;

		Map actionParameters = new HashMap();
		actionParameters.put("test", "${#xFFFF}");

		Map phaseParameters = new HashMap();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("", value);
	}

	public void testNaughtyEscapedCharactersParameter() {
		passTest = false;

		Map actionParameters = new HashMap();
		actionParameters.put("test", "${#36} ${#44} ${#58} ${#59} ${#123} ${#125}");

		Map phaseParameters = new HashMap();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("$ , : ; { }", value);
	}

	public void testNullCharEscapedCharactersParameter() {
		passTest = false;

		Map actionParameters = new HashMap();
		actionParameters.put("test", "a${#0}b");

		Map phaseParameters = new HashMap();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("a\0b", value);
	}

	public void testMaxCharEscapedCharactersParameter() {
		passTest = false;

		Map actionParameters = new HashMap();
		actionParameters.put("test", "a${#65535}b");

		Map phaseParameters = new HashMap();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("a\uFFFFb", value);
	}

	public void testOverMaxCharEscapedCharactersParameter() {
		passTest = false;

		Map actionParameters = new HashMap();
		actionParameters.put("test", "a${#65536}b");

		Map phaseParameters = new HashMap();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("ab", value);
	}

	public void testUnderMinCharEscapedCharactersParameter() {
		passTest = false;

		Map actionParameters = new HashMap();
		actionParameters.put("test", "a${#-1}b");

		Map phaseParameters = new HashMap();
		phaseParameters.put("variable", "Value");

		ParameterizedProvisioningAction pAction = new ParameterizedProvisioningAction(action, actionParameters, null);
		pAction.execute(phaseParameters);
		assertEquals("ab", value);
	}

}
