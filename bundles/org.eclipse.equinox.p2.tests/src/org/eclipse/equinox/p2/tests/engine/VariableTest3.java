/*******************************************************************************
 * Copyright (c) 2013 Landmark Graphics Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Landmark Graphics Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.engine.spi.Value;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class VariableTest3 extends AbstractProvisioningTest {

	private IInstallableUnit createIUWithVariable() {
		InstallableUnitDescription description = new MetadataFactory.InstallableUnitDescription();
		description.setId("artifactWithZip");
		description.setVersion(Version.create("1.0.0"));
		Map touchpointData = new HashMap();
		touchpointData.put("install", "test.actionForVariableTesting3( arg1: val1 ); test.actionForVariableTesting3 ( arg1: ${lastResult}); test.actionForVariableTesting3( arg1: ${lastResult} );");

		description.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		return MetadataFactory.createInstallableUnit(description);
	}

	//Test that lastResult is reset when an action does not return a value
	//In the first invocation of the action a return value is set
	//In the second invocation of the action no value is returned (we are returning the special value, no value)
	//In the third invocation we verify that lastResult is empty
	public void testLastResultIsReinitialized() {
		Map properties = new HashMap();
		properties.put(IProfile.PROP_INSTALL_FOLDER, getTempFolder().getAbsolutePath());
		IProfile profile = createProfile(this.getName(), properties);

		Iterator ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());

		IProvisioningPlan plan = getEngine().createPlan(profile, null);
		plan.addInstallableUnit(createIUWithVariable());
		Action.expectedInputValues.add("val1");
		Action.expectedReturnValues.add("returnValue1");
		Action.expectedInputValues.add("returnValue1");
		Action.expectedReturnValues.add(Value.NO_VALUE);
		Action.expectedInputValues.add(null);
		Action.expectedReturnValues.add("unusedValue");
		IStatus result = getEngine().perform(plan, PhaseSetFactory.createPhaseSetIncluding(new String[] {PhaseSetFactory.PHASE_INSTALL}), new NullProgressMonitor());
		assertOK(result);
	}

	public static class Action extends ProvisioningAction {
		public static Object result;
		public static ArrayList<Object> expectedInputValues = new ArrayList<Object>();
		public static ArrayList<Object> expectedReturnValues = new ArrayList<Object>();
		public static int invocationCounter = 0;

		@Override
		public IStatus execute(Map<String, Object> parameters) {
			assertEquals(expectedInputValues.get(invocationCounter), parameters.get("arg1"));
			result = expectedReturnValues.get(invocationCounter);
			invocationCounter++;
			return Status.OK_STATUS;
		}

		public Value<Object> getResult() {
			if (Value.NO_VALUE == result)
				return Value.NO_VALUE;
			return new Value<Object>(result);
		}

		@Override
		public IStatus undo(Map<String, Object> parameters) {
			return null;
		}
	}
}
