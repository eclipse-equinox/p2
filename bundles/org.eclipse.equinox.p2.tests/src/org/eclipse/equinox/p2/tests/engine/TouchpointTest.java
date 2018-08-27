/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.PhaseSet;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.spi.Touchpoint;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.engine.PhaseTest.TestPhaseSet;

/**
 * Simple test of the engine API.
 */
public class TouchpointTest extends AbstractProvisioningTest {

	static volatile TestTouchpoint testTouchpoint;

	abstract static class TestTouchpoint extends Touchpoint {
		int completeOperand;
		int completePhase;
		int initializeOperand;
		int initializePhase;

		public TestTouchpoint() {
			testTouchpoint = this;
		}

		@Override
		public IStatus completeOperand(IProfile profile, Map<String, Object> parameters) {
			completeOperand++;
			return super.completeOperand(profile, parameters);
		}

		@Override
		public IStatus completePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map<String, Object> touchpointParameters) {
			completePhase++;
			return super.completePhase(monitor, profile, phaseId, touchpointParameters);
		}

		@Override
		public IStatus initializeOperand(IProfile profile, Map<String, Object> parameters) {
			initializeOperand++;
			return super.initializeOperand(profile, parameters);
		}

		@Override
		public IStatus initializePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map<String, Object> touchpointParameters) {
			initializePhase++;
			return super.initializePhase(monitor, profile, phaseId, touchpointParameters);
		}

		public void resetCounters() {
			completeOperand = 0;
			completePhase = 0;
			initializeOperand = 0;
			initializePhase = 0;
		}
	}

	public static class OperandTestTouchpoint extends TestTouchpoint {
		@Override
		public IStatus completeOperand(IProfile profile, Map<String, Object> parameters) {
			assertEquals(1, initializeOperand);
			assertEquals(0, completeOperand);
			super.completeOperand(profile, parameters);
			assertEquals(1, initializeOperand);
			assertEquals(1, completeOperand);
			return null;
		}

		@Override
		public IStatus initializeOperand(IProfile profile, Map<String, Object> parameters) {
			assertEquals(0, initializeOperand);
			assertEquals(0, completeOperand);
			assertTrue(parameters.containsKey("TestPhase.initializeOperand"));
			super.initializeOperand(profile, parameters);
			assertEquals(1, initializeOperand);
			assertEquals(0, completeOperand);
			return null;
		}

		@Override
		public String qualifyAction(String actionId) {
			return "operandtest." + actionId;
		}
	}

	public static class PhaseTestTouchpoint extends TestTouchpoint {
		@Override
		public IStatus completePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map<String, Object> parameters) {
			assertEquals(1, initializePhase);
			assertEquals(0, completePhase);
			super.completePhase(monitor, profile, phaseId, parameters);
			assertEquals(1, initializePhase);
			assertEquals(1, completePhase);
			return null;
		}

		@Override
		public IStatus initializePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map<String, Object> parameters) {
			assertEquals(0, initializePhase);
			assertEquals(0, completePhase);
			assertTrue(parameters.containsKey("TestPhase.initializePhase"));
			super.initializePhase(monitor, profile, phaseId, parameters);
			assertEquals(1, initializePhase);
			assertEquals(0, completePhase);
			return null;
		}

		@Override
		public String qualifyAction(String actionId) {
			return "phasetest." + actionId;
		}
	}

	private IEngine engine;

	public TouchpointTest(String name) {
		super(name);
	}

	public TouchpointTest() {
		super("");
	}

	@Override
	protected void setUp() throws Exception {
		engine = getEngine();
	}

	@Override
	protected void tearDown() throws Exception {
		engine = null;
	}

	public void testInitCompleteOperand() {
		if (testTouchpoint != null)
			testTouchpoint.resetCounters();
		PhaseSet phaseSet = new TestPhaseSet();
		IProfile profile = createProfile("testProfile");
		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(createTestIU("operandTest"));
		engine.perform(plan, phaseSet, new NullProgressMonitor());
		assertEquals(1, testTouchpoint.initializeOperand);
		assertEquals(1, testTouchpoint.completeOperand);
	}

	public void testInitCompletePhase() {
		if (testTouchpoint != null)
			testTouchpoint.resetCounters();
		PhaseSet phaseSet = new TestPhaseSet();
		IProfile profile = createProfile("testProfile");
		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(createTestIU("phaseTest"));
		engine.perform(plan, phaseSet, new NullProgressMonitor());
		assertEquals(1, testTouchpoint.initializePhase);
		assertEquals(1, testTouchpoint.completePhase);
	}

	private IInstallableUnit createTestIU(String touchpointName) {
		InstallableUnitDescription description = new MetadataFactory.InstallableUnitDescription();
		description.setId("org.eclipse.test");
		description.setVersion(Version.create("1.0.0"));
		description.setTouchpointType(MetadataFactory.createTouchpointType(touchpointName, Version.create("1.0.0")));
		IInstallableUnit unit = MetadataFactory.createInstallableUnit(description);
		return createResolvedIU(unit);
	}

}
