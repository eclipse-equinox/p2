/*******************************************************************************
 *  Copyright (c) 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.util.*;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.RemoveProgramPropertyAction;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class RemoveProgramPropertyActionTest extends AbstractProvisioningTest {

	public RemoveProgramPropertyActionTest(String name) {
		super(name);
	}

	public RemoveProgramPropertyActionTest() {
		super("");
	}

	public void testExecuteUndo() {
		// setup
		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		Properties profileProperties = new Properties();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, getTempFolder().toString());
		IProfile profile = createProfile("test", profileProperties);
		InstallableUnitOperand operand = new InstallableUnitOperand(null, createIU("test"));
		touchpoint.initializePhase(null, profile, "test", parameters);
		parameters.put("iu", operand.second());
		touchpoint.initializeOperand(profile, parameters);
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);
		ConfigData data = manipulator.getConfigData();

		String key = getUniqueString();
		String value1 = "foo";
		String value2 = "bar";
		String value3 = "quux";
		String initial = value1 + "," + value2 + "," + value3;
		assertNull(data.getProperty(key));
		data.setProperty(key, initial);

		// set the parms
		parameters.put(ActionConstants.PARM_PROP_NAME, key);
		parameters.put(ActionConstants.PARM_PROP_VALUE, value2);

		// execute the action and check the values
		RemoveProgramPropertyAction action = new RemoveProgramPropertyAction();
		action.execute(parameters);
		String current = data.getProperty(key);
		assertNotNull(current);
		assertEquals(value1 + "," + value3, current);

		// undo action
		action.undo(parameters);
		current = data.getProperty(key);
		assertNotNull(current);
		assertEquals(initial, current);

		// cleanup
		data.setProperty(key, null);
	}

}