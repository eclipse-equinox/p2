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

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.p2.engine.phases.Collect;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

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

	public static class TestPhase extends InstallableUnitPhase {

		public boolean initializePhase;
		public boolean completePhase;
		public boolean initializeOperand;
		public boolean completeOperand;

		protected TestPhase() {
			super("test", 1);
		}

		protected TestPhase(String phaseId, int weight) {
			super(phaseId, weight);
		}

		protected IStatus completeOperand(IProfile profile, InstallableUnitOperand operand, Map parameters, IProgressMonitor monitor) {
			completeOperand = true;
			return super.completeOperand(profile, operand, parameters, monitor);
		}

		protected IStatus initializeOperand(IProfile profile, InstallableUnitOperand operand, Map parameters, IProgressMonitor monitor) {
			parameters.put("TestPhase.initializeOperand", "true");
			initializeOperand = true;
			return super.initializeOperand(profile, operand, parameters, monitor);
		}

		protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
			completePhase = true;
			return super.completePhase(monitor, profile, parameters);
		}

		protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
			parameters.put("TestPhase.initializePhase", "true");
			initializePhase = true;
			return super.initializePhase(monitor, profile, parameters);
		}

		protected List<ProvisioningAction> getActions(InstallableUnitOperand operand) {
			IInstallableUnit unit = operand.second();
			List<ProvisioningAction> parsedActions = getActions(unit, phaseId);
			if (parsedActions != null)
				return parsedActions;

			ITouchpointType type = unit.getTouchpointType();
			if (type == null || type == ITouchpointType.NONE)
				return null;

			String actionId = getActionManager().getTouchpointQualifiedActionId(phaseId, type);
			ProvisioningAction action = getActionManager().getAction(actionId, null);
			if (action == null) {
				throw new IllegalArgumentException("action not found: " + phaseId);
			}
			return Collections.singletonList(action);
		}
	}

	private IEngine engine;

	public PhaseTest(String name) {
		super(name);
	}

	public PhaseTest() {
		super("");
	}

	protected void setUp() throws Exception {
		engine = getEngine();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		engine = null;
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
		PhaseSet phaseSet = new TestPhaseSet(new TestPhase());
		IProfile profile = createProfile("PhaseTest");

		engine.perform(engine.createPlan(profile, null), phaseSet, new NullProgressMonitor());
	}

	public void testInitCompletePhase() {
		TestPhase phase = new TestPhase() {
			protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
				assertFalse(parameters.containsKey("TestPhase.initializePhase"));
				assertFalse(completePhase);
				super.initializePhase(monitor, profile, parameters);
				assertTrue(parameters.containsKey("TestPhase.initializePhase"));
				assertFalse(completePhase);
				return null;
			}

			protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
				assertTrue(parameters.containsKey("TestPhase.initializePhase"));
				assertFalse(completePhase);
				super.completePhase(monitor, profile, parameters);
				assertTrue(parameters.containsKey("TestPhase.initializePhase"));
				assertTrue(completePhase);
				return null;
			}
		};
		PhaseSet phaseSet = new TestPhaseSet(phase);
		IProfile profile = createProfile("PhaseTest");
		IInstallableUnit unit = createIU("unit");
		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(unit);
		engine.perform(plan, phaseSet, new NullProgressMonitor());
		assertTrue(phase.initializePhase);
		assertTrue(phase.completePhase);
	}

	public void testInitCompleteOperand() {
		TestPhase phase = new TestPhase() {
			protected IStatus completeOperand(IProfile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
				assertTrue(parameters.containsKey("TestPhase.initializeOperand"));
				assertFalse(completeOperand);
				super.completeOperand(profile, operand, parameters, monitor);
				assertTrue(parameters.containsKey("TestPhase.initializeOperand"));
				assertTrue(completeOperand);
				return null;
			}

			protected IStatus initializeOperand(IProfile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
				assertFalse(parameters.containsKey("TestPhase.initializeOperand"));
				assertFalse(completeOperand);
				super.initializeOperand(profile, operand, parameters, monitor);
				assertTrue(parameters.containsKey("TestPhase.initializeOperand"));
				assertFalse(completeOperand);
				return null;
			}
		};
		PhaseSet phaseSet = new TestPhaseSet(phase);
		IProfile profile = createProfile("PhaseTest");
		IInstallableUnit unit = createIU("testInitCompleteOperand");

		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(unit);
		engine.perform(plan, phaseSet, new NullProgressMonitor());
		assertTrue(phase.initializeOperand);
		assertTrue(phase.completeOperand);
	}

	public void testGetProfileDataArea() {
		TestPhase phase = new TestPhase() {
			protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
				File profileDataArea = (File) parameters.get("profileDataDirectory");
				assertTrue(profileDataArea.isDirectory());
				File test = new File(profileDataArea, "test");
				assertFalse(test.exists());
				try {
					test.createNewFile();
				} catch (IOException e) {
					fail(e.getMessage());
				}
				assertTrue(test.exists());
				return super.initializePhase(monitor, profile, parameters);
			}

			protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
				File profileDataArea = (File) parameters.get("profileDataDirectory");
				assertTrue(profileDataArea.isDirectory());
				File test = new File(profileDataArea, "test");
				assertTrue(test.exists());
				test.delete();
				assertFalse(test.exists());
				return super.completePhase(monitor, profile, parameters);
			}
		};
		PhaseSet phaseSet = new TestPhaseSet(phase);
		IProfile profile = createProfile("PhaseTest");
		IInstallableUnit unit = createIU("testGetProfileDataArea");

		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(unit);
		engine.perform(plan, phaseSet, new NullProgressMonitor());
		assertTrue(phase.initializePhase);
		assertTrue(phase.completePhase);
	}

	public static class TestAction extends ProvisioningAction {

		public IStatus execute(Map parameters) {
			return null;
		}

		public IStatus undo(Map parameters) {
			return null;
		}
	}

	public void testGetAction() {
		final ArrayList actionsList1 = new ArrayList();
		InstallableUnitPhase phase1 = new InstallableUnitPhase("test", 1) {
			protected List<ProvisioningAction> getActions(InstallableUnitOperand operand) {
				List<ProvisioningAction> actions = getActions(operand.second(), "test1");
				actionsList1.addAll(actions);
				return actions;
			}
		};
		final ArrayList actionsList2 = new ArrayList();
		InstallableUnitPhase phase2 = new InstallableUnitPhase("test", 1) {
			protected List<ProvisioningAction> getActions(InstallableUnitOperand operand) {
				List<ProvisioningAction> actions = getActions(operand.second(), "test2");
				actionsList2.addAll(actions);
				return actions;
			}
		};

		PhaseSet phaseSet = new TestPhaseSet(new Phase[] {phase1, phase2});
		IProfile profile = createProfile("PhaseTest");

		Map instructions = new HashMap();
		instructions.put("test1", MetadataFactory.createTouchpointInstruction("test1.test()", null));
		instructions.put("test2", MetadataFactory.createTouchpointInstruction("test2.test()", null));
		ITouchpointData touchpointData = MetadataFactory.createTouchpointData(instructions);
		IInstallableUnit unit = createIU("test", Version.create("1.0.0"), null, NO_REQUIRES, new IProvidedCapability[0], NO_PROPERTIES, ITouchpointType.NONE, touchpointData, false);
		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(unit);
		IStatus status = engine.perform(plan, phaseSet, new NullProgressMonitor());
		if (!status.isOK()) {
			fail(status.toString());
		}

		assertEquals(TestAction.class, ((ParameterizedProvisioningAction) actionsList1.get(0)).getAction().getClass());
		assertEquals(TestAction.class, ((ParameterizedProvisioningAction) actionsList2.get(0)).getAction().getClass());
	}

	public void testCancelHappenBeforeCompleteCollectPhase() {
		final String testDataLocation = "testData/mirror/mirrorSourceRepo3";
		Set<IInstallableUnit> ius = null;
		try {
			IArtifactRepositoryManager mgr = getArtifactRepositoryManager();
			mgr.loadRepository((getTestData("test artifact repo", testDataLocation).toURI()), null);
			IMetadataRepositoryManager metaManager = getMetadataRepositoryManager();
			IMetadataRepository metaRepo = metaManager.loadRepository((getTestData("test metadata repo", testDataLocation).toURI()), null);
			ius = metaRepo.query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet();
		} catch (Exception e) {
			fail("1.0", e);
		}
		class MyCollect extends Collect {
			boolean isCancelled = false;
			int progress = 0;
			final static int THREHOLD = 2;

			public MyCollect(int weight) {
				super(weight);
			}

			@Override
			protected List<ProvisioningAction> getActions(InstallableUnitOperand operand) {
				List<ProvisioningAction> actions = super.getActions(operand);
				if (actions != null)
					progress++;
				if (progress > THREHOLD)
					isCancelled = true;
				return actions;
			}
		}
		final MyCollect collect = new MyCollect(100);
		PhaseSet phaseSet = new TestPhaseSet(new Phase[] {collect});
		IProfile profile = createProfile("PhaseTest");
		IProvisioningPlan plan = engine.createPlan(profile, null);
		for (IInstallableUnit iu : ius)
			plan.addInstallableUnit(iu);
		class TestListener implements ProvisioningListener {
			boolean collectEvent = false;

			public void notify(EventObject o) {
				if (o instanceof CollectEvent)
					collectEvent = true;
			}

		}
		TestListener listener = new TestListener();
		getEventBus().addListener(listener);
		try {
			IStatus status = engine.perform(plan, phaseSet, new NullProgressMonitor() {
				@Override
				public boolean isCanceled() {
					return collect.isCancelled;
				}
			});
			if (!status.matches(IStatus.CANCEL)) {
				fail(status.toString());
			}
			assertFalse("Collect actually happened!", listener.collectEvent);
		} finally {
			getEventBus().removeListener(listener);
		}
	}
}
