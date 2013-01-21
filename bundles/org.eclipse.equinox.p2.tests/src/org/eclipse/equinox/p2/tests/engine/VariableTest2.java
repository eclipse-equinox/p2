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

public class VariableTest2 extends AbstractProvisioningTest {

	private IInstallableUnit createIUWithVariable() {
		InstallableUnitDescription description = new MetadataFactory.InstallableUnitDescription();
		description.setId("artifactWithZip");
		description.setVersion(Version.create("1.0.0"));
		Map touchpointData = new HashMap();
		touchpointData.put("install", "test.actionForVariableTesting2( arg1: expectedValue ); test.actionForVariableTesting2( arg1: ${lastResult} );");

		description.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		return MetadataFactory.createInstallableUnit(description);
	}

	//Test that the values returned from the action are properly passed in and not mangled to string
	public void testNonStringValue() {
		Map properties = new HashMap();
		properties.put(IProfile.PROP_INSTALL_FOLDER, getTempFolder().getAbsolutePath());
		IProfile profile = createProfile(this.getName(), properties);

		Iterator ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());

		IProvisioningPlan plan = getEngine().createPlan(profile, null);
		plan.addInstallableUnit(createIUWithVariable());
		IStatus result = getEngine().perform(plan, PhaseSetFactory.createPhaseSetIncluding(new String[] {PhaseSetFactory.PHASE_INSTALL}), new NullProgressMonitor());
		assertOK(result);
		assertEquals(2, Action.inputValues.size());
		assertEquals(String.class, Action.inputValues.get(0).getClass());
		assertEquals(Object.class, Action.inputValues.get(1).getClass());
	}

	public static class Action extends ProvisioningAction {
		public static Object result;
		public static ArrayList<Object> inputValues = new ArrayList<Object>();

		@Override
		public IStatus execute(Map<String, Object> parameters) {
			inputValues.add(parameters.get("arg1"));
			result = new Object();
			return Status.OK_STATUS;
		}

		public Value<Object> getResult() {
			return new Value<Object>(result);
		}

		@Override
		public IStatus undo(Map<String, Object> parameters) {
			return null;
		}
	}
}
