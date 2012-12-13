/*******************************************************************************
 * Copyright (c) 2012 Landmark Graphics Corporation
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

public class VariableTest extends AbstractProvisioningTest {

	private IInstallableUnit createIUWithVariable() {
		InstallableUnitDescription description = new MetadataFactory.InstallableUnitDescription();
		description.setId("artifactWithZip");
		description.setVersion(Version.create("1.0.0"));
		Map touchpointData = new HashMap();
		touchpointData.put("install", "test.actionForVariableTesting( arg1: expectedValue ); test.actionForVariableTesting ( arg1: ${lastResult} ); test.actionForVariableTesting ( arg1: ${lastResult} )");

		description.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		return MetadataFactory.createInstallableUnit(description);
	}

	public void testVariable() {
		Action.reinitForNextTest();
		Map properties = new HashMap();
		properties.put(IProfile.PROP_INSTALL_FOLDER, getTempFolder().getAbsolutePath());
		IProfile profile = createProfile("testVariable1", properties);

		Iterator ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());

		IProvisioningPlan plan = getEngine().createPlan(profile, null);
		plan.addInstallableUnit(createIUWithVariable());
		IStatus result = getEngine().perform(plan, PhaseSetFactory.createPhaseSetIncluding(new String[] {PhaseSetFactory.PHASE_INSTALL}), new NullProgressMonitor());
		assertOK(result);
		assertEquals("expectedValueaaa", Action.result);
	}

	public void testUndo() {
		Action.reinitForNextTest();
		Action.failMode = true;
		Map properties = new HashMap();
		properties.put(IProfile.PROP_INSTALL_FOLDER, getTempFolder().getAbsolutePath());
		IProfile profile = createProfile("testVariable2", properties);

		Iterator ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());

		IProvisioningPlan plan = getEngine().createPlan(profile, null);
		plan.addInstallableUnit(createIUWithVariable());
		IStatus result = getEngine().perform(plan, PhaseSetFactory.createPhaseSetIncluding(new String[] {PhaseSetFactory.PHASE_INSTALL}), new NullProgressMonitor());
		assertNotOK(result);
		assertEquals(3, Action.inputValues.size());
		assertEquals(3, Action.undoValues.size());

		//The undo values should be the same than the input values
		assertEquals(Action.inputValues.get(0), Action.undoValues.get(2));
		assertEquals(Action.inputValues.get(1), Action.undoValues.get(1));
		assertEquals(Action.inputValues.get(2), Action.undoValues.get(0));
	}

	public static class Action extends ProvisioningAction {
		public static String result;
		public static boolean failMode;
		private static int count = 0;
		private static final int failAt = 3;

		public static ArrayList<String> inputValues = new ArrayList<String>();
		public static ArrayList<String> undoValues = new ArrayList<String>();

		public static void reinitForNextTest() {
			inputValues = new ArrayList<String>();
			undoValues = new ArrayList<String>();
			failMode = false;
			count = 0;
		}

		@Override
		public IStatus execute(Map<String, Object> parameters) {
			inputValues.add((String) parameters.get("arg1"));
			System.out.println(this.hashCode());
			System.out.println((String) parameters.get("arg1"));
			count++;
			if (failMode && count == failAt)
				throw new RuntimeException("GENERATED Exception");
			result = ((String) parameters.get("arg1")) + "a";

			return Status.OK_STATUS;
		}

		@Override
		public IStatus undo(Map<String, Object> parameters) {
			undoValues.add((String) parameters.get("arg1"));
			System.out.println("undo arg --> " + (String) parameters.get("arg1"));
			return Status.OK_STATUS;
		}

		public Value<String> getResult() {
			return new Value<String>(result);
		}
	}
}
