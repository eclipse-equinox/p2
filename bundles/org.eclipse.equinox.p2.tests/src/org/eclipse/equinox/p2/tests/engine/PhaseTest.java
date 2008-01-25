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
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;

/**
 * Simple test of the engine API.
 */
public class PhaseTest extends AbstractProvisioningTest {
	public static class TestPhaseSet extends PhaseSet {

		public TestPhaseSet() {
			super(new Phase[] {new TestPhase()});
		}

		public TestPhaseSet(Phase phase) {
			super(new Phase[] {phase});
		}

		public TestPhaseSet(Phase[] phases) {
			super(phases);
		}
	}

	public static class TestPhase extends Phase {

		boolean completeOperand;
		boolean getAction;
		boolean initializeOperand;
		boolean completePhase;
		boolean initializePhase;

		protected TestPhase() {
			super("test", 1);
		}

		protected TestPhase(String phaseId, int weight) {
			super(phaseId, weight);
		}

		protected ProvisioningAction[] getActions(Operand currentOperand) {
			return null;
		}

		protected boolean isApplicable(Operand op) {
			return true;
		}

		protected IStatus completeOperand(Operand operand, Map parameters) {
			completeOperand = true;
			return super.completeOperand(operand, parameters);
		}

		public ProvisioningAction getAction(String actionId) {
			getAction = true;
			return super.getAction(actionId);
		}

		protected IStatus initializeOperand(Profile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
			initializeOperand = true;
			return super.initializeOperand(profile, operand, parameters, monitor);
		}

		protected IStatus completePhase(IProgressMonitor monitor, Profile profile, Map parameters) {
			completePhase = true;
			return super.completePhase(monitor, profile, parameters);
		}

		protected IStatus initializePhase(IProgressMonitor monitor, Profile profile, Map parameters) {
			initializePhase = true;
			return super.initializePhase(monitor, profile, parameters);
		}
	}

	private ServiceReference engineRef;
	private Engine engine;

	public PhaseTest(String name) {
		super(name);
	}

	public PhaseTest() {
		super("");
	}

	protected void setUp() throws Exception {
		engineRef = TestActivator.getContext().getServiceReference(Engine.class.getName());
		engine = (Engine) TestActivator.getContext().getService(engineRef);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		engine = null;
		TestActivator.getContext().ungetService(engineRef);
	}

	public void testNullPhaseId() {
		try {
			new TestPhase(null, 1);
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testEmptyPhaseId() {
		try {
			new TestPhase("", 1);
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testNegativeWeight() {
		try {
			new TestPhase("xyz", -1);
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testZeroWeight() {
		try {
			new TestPhase("xyz", 0);
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testPerform() {
		PhaseSet phaseSet = new TestPhaseSet();
		Profile profile = createProfile("PhaseTest");

		engine.perform(profile, phaseSet, new Operand[0], null, new NullProgressMonitor());
	}

	public void testInitCompletePhase() {
		TestPhase phase = new TestPhase() {
			protected IStatus initializePhase(IProgressMonitor monitor, Profile profile, Map parameters) {
				assertFalse(initializePhase);
				assertFalse(completePhase);
				super.initializePhase(monitor, profile, parameters);
				assertTrue(initializePhase);
				assertFalse(completePhase);
				return null;
			}

			protected IStatus completePhase(IProgressMonitor monitor, Profile profile, Map parameters) {
				assertTrue(initializePhase);
				assertFalse(completePhase);
				super.completePhase(monitor, profile, parameters);
				assertTrue(initializePhase);
				assertTrue(completePhase);
				return null;
			}
		};
		PhaseSet phaseSet = new TestPhaseSet(phase);
		Profile profile = createProfile("PhaseTest");
		IInstallableUnit unit = createIU("unit");
		engine.perform(profile, phaseSet, new Operand[] {new Operand(null, unit)}, null, new NullProgressMonitor());
		assertTrue(phase.initializePhase);
		assertTrue(phase.completePhase);
	}

	public void testInitCompleteOperand() {
		TestPhase phase = new TestPhase() {
			protected IStatus completeOperand(Operand operand, Map parameters) {
				assertTrue(initializeOperand);
				assertFalse(completeOperand);
				super.completeOperand(operand, parameters);
				assertTrue(initializeOperand);
				assertTrue(completeOperand);
				return null;
			}

			protected IStatus initializeOperand(Profile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
				assertFalse(initializeOperand);
				assertFalse(completeOperand);
				super.initializeOperand(profile, operand, parameters, monitor);
				assertTrue(initializeOperand);
				assertFalse(completeOperand);
				return null;
			}
		};
		PhaseSet phaseSet = new TestPhaseSet(phase);
		Profile profile = createProfile("PhaseTest");
		IInstallableUnit unit = createIU("testInitCompleteOperand");

		engine.perform(profile, phaseSet, new Operand[] {new Operand(null, unit)}, null, new NullProgressMonitor());
		assertTrue(phase.initializeOperand);
		assertTrue(phase.completeOperand);
	}
}
