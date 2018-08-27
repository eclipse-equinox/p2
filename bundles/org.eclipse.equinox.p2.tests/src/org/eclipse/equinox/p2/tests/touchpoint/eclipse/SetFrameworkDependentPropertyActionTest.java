/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.util.*;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.SetProgramPropertyAction;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SetFrameworkDependentPropertyActionTest extends AbstractProvisioningTest {

	public SetFrameworkDependentPropertyActionTest(String name) {
		super(name);
	}

	public SetFrameworkDependentPropertyActionTest() {
		super("");
	}

	public void testExecuteUndo() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		Map<String, String> profileProperties = new HashMap<>();
		profileProperties.put(IProfile.PROP_INSTALL_FOLDER, getTempFolder().toString());
		IProfile profile = createProfile("test", profileProperties);
		InstallableUnitOperand operand = new InstallableUnitOperand(null, createIU("test"));
		touchpoint.initializePhase(null, profile, "test", parameters);
		parameters.put("iu", operand.second());
		touchpoint.initializeOperand(profile, parameters);
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);

		String frameworkDependentPropertyName = "test";
		String frameworkDependentPropertyValue = "true";
		assertFalse(manipulator.getConfigData().getProperties().containsKey(frameworkDependentPropertyName));
		parameters.put(ActionConstants.PARM_PROP_NAME, frameworkDependentPropertyName);
		parameters.put(ActionConstants.PARM_PROP_VALUE, frameworkDependentPropertyValue);
		parameters = Collections.unmodifiableMap(parameters);

		SetProgramPropertyAction action = new SetProgramPropertyAction();
		action.execute(parameters);
		assertEquals("true", manipulator.getConfigData().getProperty(frameworkDependentPropertyName));
		action.undo(parameters);
		assertFalse(manipulator.getConfigData().getProperties().containsKey(frameworkDependentPropertyName));
	}
}