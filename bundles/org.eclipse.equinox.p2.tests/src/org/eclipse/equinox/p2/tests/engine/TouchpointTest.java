/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.engine.PhaseTest.TestPhaseSet;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

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

		public IStatus completeOperand(Profile profile, String phaseId, Operand operand, Map parameters) {
			completeOperand++;
			return super.completeOperand(profile, phaseId, operand, parameters);
		}

		public IStatus completePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters) {
			completePhase++;
			return super.completePhase(monitor, profile, phaseId, touchpointParameters);
		}

		public IStatus initializeOperand(Profile profile, String phaseId, Operand operand, Map parameters) {
			initializeOperand++;
			return super.initializeOperand(profile, phaseId, operand, parameters);
		}

		public IStatus initializePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters) {
			initializePhase++;
			return super.initializePhase(monitor, profile, phaseId, touchpointParameters);
		}

		public ProvisioningAction getAction(String actionId) {
			return null;
		}

		public TouchpointType getTouchpointType() {
			return MetadataFactory.createTouchpointType("test", new Version("1.0.0"));
		}
	}

	public static class OperandTestTouchpoint extends TestTouchpoint {
		public IStatus completeOperand(Profile profile, String phaseId, Operand operand, Map parameters) {
			assertEquals(1, initializeOperand);
			assertEquals(0, completeOperand);
			super.completeOperand(profile, phaseId, operand, parameters);
			assertEquals(1, initializeOperand);
			assertEquals(1, completeOperand);
			return null;
		}

		public IStatus initializeOperand(Profile profile, String phaseId, Operand operand, Map parameters) {
			assertEquals(0, initializeOperand);
			assertEquals(0, completeOperand);
			super.initializeOperand(profile, phaseId, operand, parameters);
			assertEquals(1, initializeOperand);
			assertEquals(0, completeOperand);
			return null;
		}
	}

	public static class PhaseTestTouchpoint extends TestTouchpoint {
		public IStatus completePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map parameters) {
			assertEquals(1, initializePhase);
			assertEquals(0, completePhase);
			super.completePhase(monitor, profile, phaseId, parameters);
			assertEquals(1, initializePhase);
			assertEquals(1, completePhase);
			return null;
		}

		public IStatus initializePhase(IProgressMonitor monitor, Profile profile, String phaseId, Operand operand, Map parameters) {
			assertEquals(0, initializePhase);
			assertEquals(0, completePhase);
			super.initializePhase(monitor, profile, phaseId, parameters);
			assertEquals(1, initializePhase);
			assertEquals(0, completePhase);
			return null;
		}
	}

	private ServiceReference engineRef;
	private Engine engine;

	public TouchpointTest(String name) {
		super(name);
	}

	public TouchpointTest() {
		super("");
	}

	protected void setUp() throws Exception {
		engineRef = TestActivator.getContext().getServiceReference(Engine.class.getName());
		engine = (Engine) TestActivator.getContext().getService(engineRef);
	}

	protected void tearDown() throws Exception {
		engine = null;
		TestActivator.getContext().ungetService(engineRef);
	}

	public void testInitCompleteOperand() {
		PhaseSet phaseSet = new TestPhaseSet();
		Profile profile = createProfile("testProfile");
		engine.perform(profile, phaseSet, new Operand[] {new Operand(null, createTestIU("operandTest"))}, new NullProgressMonitor());
		assertEquals(1, testTouchpoint.initializeOperand);
		assertEquals(1, testTouchpoint.completeOperand);
	}

	public void testInitCompletePhase() {
		PhaseSet phaseSet = new TestPhaseSet();
		Profile profile = createProfile("testProfile");
		engine.perform(profile, phaseSet, new Operand[] {new Operand(null, createTestIU("phaseTest"))}, new NullProgressMonitor());
		assertEquals(1, testTouchpoint.initializeOperand);
		assertEquals(1, testTouchpoint.completeOperand);
	}

	private IInstallableUnit createTestIU(String touchpointName) {
		InstallableUnitDescription description = new MetadataFactory.InstallableUnitDescription();
		description.setId("org.eclipse.test");
		description.setVersion(new Version("1.0.0"));
		description.setTouchpointType(MetadataFactory.createTouchpointType(touchpointName, new Version("1.0.0")));
		IInstallableUnit unit = MetadataFactory.createInstallableUnit(description);
		return createResolvedIU(unit);
	}

}
