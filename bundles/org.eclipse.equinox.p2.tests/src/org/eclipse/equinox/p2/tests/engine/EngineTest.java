/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import static org.junit.Assert.assertThrows;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.Operand;
import org.eclipse.equinox.internal.p2.engine.Phase;
import org.eclipse.equinox.internal.p2.engine.PhaseSet;
import org.eclipse.equinox.internal.p2.engine.phases.CheckTrust;
import org.eclipse.equinox.internal.p2.engine.phases.Collect;
import org.eclipse.equinox.internal.p2.engine.phases.Configure;
import org.eclipse.equinox.internal.p2.engine.phases.Install;
import org.eclipse.equinox.internal.p2.engine.phases.Property;
import org.eclipse.equinox.internal.p2.engine.phases.Sizing;
import org.eclipse.equinox.internal.p2.engine.phases.Unconfigure;
import org.eclipse.equinox.internal.p2.engine.phases.Uninstall;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IPhaseSet;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.PhaseSetFactory;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Simple test of the engine API.
 *
 * Note:
 * Currently you MUST have previously generated metadata from a 3.3.1 install.
 * There are ordering dependencies for the tests temporarily
 */
public class EngineTest extends AbstractProvisioningTest {

	private static class CountPhase extends Phase {
		int operandCount = 0;
		int phaseCount = 0;

		protected CountPhase(String name, boolean forced) {
			super(name, 1, forced);
		}

		protected CountPhase(String name) {
			this(name, false);
		}

		@Override
		protected IStatus completeOperand(IProfile profile, Operand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
			operandCount--;
			return super.completeOperand(profile, operand, parameters, monitor);
		}

		@Override
		protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
			phaseCount--;
			return super.completePhase(monitor, profile, parameters);
		}

		@Override
		protected IStatus initializeOperand(IProfile profile, Operand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
			operandCount++;
			return super.initializeOperand(profile, operand, parameters, monitor);
		}

		@Override
		protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
			phaseCount++;
			return super.initializePhase(monitor, profile, parameters);
		}

		@Override
		protected List<ProvisioningAction> getActions(Operand operand) {
			return null;
		}

		public boolean isConsistent() {
			return operandCount == 0 && phaseCount == 0;
		}
	}

	private static class NPEPhase extends CountPhase {
		protected NPEPhase() {
			super("NPE");
		}

		@Override
		protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
			super.completePhase(monitor, profile, parameters);
			throw new NullPointerException();
		}

		@Override
		protected List<ProvisioningAction> getActions(Operand operand) {
			return null;
		}
	}

	private static class ActionNPEPhase extends CountPhase {
		protected ActionNPEPhase(boolean forced) {
			super("ActionNPEPhase", forced);
		}

		protected ActionNPEPhase() {
			this(false);
		}

		@Override
		protected List<ProvisioningAction> getActions(Operand operand) {
			ProvisioningAction action = new ProvisioningAction() {

				@Override
				public IStatus undo(Map<String, Object> parameters) {
					throw new NullPointerException();
				}

				@Override
				public IStatus execute(Map<String, Object> parameters) {
					throw new NullPointerException();
				}
			};
			return Collections.singletonList(action);
		}
	}

	private static class TestPhaseSet extends PhaseSet {
		public TestPhaseSet(Phase phase) {
			super(new Phase[] {new Collect(100), new Unconfigure(10), new Uninstall(50), new Property(1), new CheckTrust(10), new Install(50), new Configure(10), phase});
		}

		public TestPhaseSet(boolean forced) {
			super(new Phase[] {new Collect(100), new Unconfigure(10, forced), new Uninstall(50, forced), new Property(1), new CheckTrust(10), new Install(50), new Configure(10)});
		}
	}

	private IEngine engine;
	private File testProvisioning;

	public EngineTest(String name) {
		super(name);
	}

	public EngineTest() {
		this("");
	}

	private static boolean deleteDirectory(File directory) {
		if (directory.exists() && directory.isDirectory()) {
			File[] files = directory.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					deleteDirectory(file);
				} else {
					file.delete();
				}
			}
		}
		return directory.delete();
	}

	@Override
	protected void setUp() throws Exception {
		engine = getEngine();
		testProvisioning = new File(System.getProperty("java.io.tmpdir"), "testProvisioning");
		deleteDirectory(testProvisioning);
		testProvisioning.mkdir();
	}

	@Override
	protected void tearDown() throws Exception {
		engine = null;
		deleteDirectory(testProvisioning);
	}

	public void testNullProfile() {

		IProfile profile = null;
		assertThrows(AssertionFailedException.class,
				() ->
			engine.perform(engine.createPlan(profile, null), new NullProgressMonitor()));
	}

	public void testNullPhaseSet() {

		IProfile profile = createProfile("test");
		PhaseSet phaseSet = null;
		assertThrows(IllegalArgumentException.class,
				() -> engine.perform(engine.createPlan(profile, null), phaseSet, new NullProgressMonitor()));
	}

	public void testNullPlan() {
		assertThrows(RuntimeException.class, () -> engine.perform(null, new NullProgressMonitor()));
	}

	/*
	 * Tests for {@link IEngine#createPhaseSetExcluding}.
	 */
	public void testCreatePhaseSetExcluding() {
		//null argument
		IPhaseSet set = PhaseSetFactory.createDefaultPhaseSetExcluding(null);
		assertEquals("1.0", 7, set.getPhaseIds().length);

		//empty argument
		set = PhaseSetFactory.createDefaultPhaseSetExcluding(new String[0]);
		assertEquals("2.0", 7, set.getPhaseIds().length);

		//bogus argument
		set = PhaseSetFactory.createDefaultPhaseSetExcluding(new String[] {"blort"});
		assertEquals("3.0", 7, set.getPhaseIds().length);

		//valid argument
		set = PhaseSetFactory.createDefaultPhaseSetExcluding(new String[] {PhaseSetFactory.PHASE_CHECK_TRUST});
		final String[] phases = set.getPhaseIds();
		assertEquals("4.0", 6, phases.length);
		for (int i = 0; i < phases.length; i++)
			if (phases[i].equals(PhaseSetFactory.PHASE_CHECK_TRUST))
				fail("4.1." + i);

	}

	/*
	 * Tests for {@link IEngine#createPhaseSetIncluding}.
	 */
	public void testCreatePhaseSetIncluding() {
		//null argument
		IPhaseSet set = PhaseSetFactory.createPhaseSetIncluding(null);
		assertNotNull("1.0", set);
		assertEquals("1.1", 0, set.getPhaseIds().length);
		//expected
		//empty argument
		set = PhaseSetFactory.createPhaseSetIncluding(new String[0]);
		assertNotNull("2.0", set);
		assertEquals("2.1", 0, set.getPhaseIds().length);

		//unknown argument
		set = PhaseSetFactory.createPhaseSetIncluding(new String[] {"blort", "not a phase", "bad input"});
		assertNotNull("3.0", set);
		assertEquals("3.1", 0, set.getPhaseIds().length);

		//one valid phase
		set = PhaseSetFactory.createPhaseSetIncluding(new String[] {PhaseSetFactory.PHASE_COLLECT});
		assertNotNull("4.0", set);
		assertEquals("4.1", 1, set.getPhaseIds().length);
		assertEquals("4.2", PhaseSetFactory.PHASE_COLLECT, set.getPhaseIds()[0]);

		//one valid phase and one bogus
		set = PhaseSetFactory.createPhaseSetIncluding(new String[] {PhaseSetFactory.PHASE_COLLECT, "bogus"});
		assertNotNull("4.0", set);
		assertEquals("4.1", 1, set.getPhaseIds().length);
		assertEquals("4.2", PhaseSetFactory.PHASE_COLLECT, set.getPhaseIds()[0]);

	}

	public void testEmptyOperands() {

		IProfile profile = createProfile("test");
		IStatus result = engine.perform(engine.createPlan(profile, null), new NullProgressMonitor());
		assertTrue(result.isOK());
	}

	public void testEmptyPhaseSet() {

		IProfile profile = createProfile("testEmptyPhaseSet");
		PhaseSet phaseSet = new PhaseSet(new Phase[] {}) {
			// empty PhaseSet
		};

		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.removeInstallableUnit(createResolvedIU(createIU("name")));
		IStatus result = engine.perform(plan, phaseSet, new NullProgressMonitor());
		assertTrue(result.isOK());
	}

	public void testPerformAddSingleNullIU() {
		try {
			IProfile profile = createProfile("testPerformAddSingleNullIU");
			IProvisioningPlan plan = engine.createPlan(profile, null);
			plan.addInstallableUnit(null);
			fail("Should not allow null iu");
		} catch (RuntimeException e) {
			//expected
		}
	}

	public void testPerformPropertyInstallUninstall() {

		IProfile profile = createProfile("testPerformPropertyInstallUninstall");

		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.setProfileProperty("test", "test");
		IInstallableUnit testIU = createResolvedIU(createIU("test"));
		plan.addInstallableUnit(testIU);
		plan.setInstallableUnitProfileProperty(testIU, "test", "test");

		IStatus result = engine.perform(plan, new NullProgressMonitor());
		assertTrue(result.isOK());
		assertEquals("test", profile.getProperty("test"));
		assertEquals("test", profile.getInstallableUnitProperty(testIU, "test"));

		plan = engine.createPlan(profile, null);
		plan.setProfileProperty("test", null);

		plan.removeInstallableUnit(testIU);
		plan.setInstallableUnitProfileProperty(testIU, "test", null);
		result = engine.perform(plan, new NullProgressMonitor());
		assertTrue(result.isOK());
		assertNull("test", profile.getProperty("test"));
		assertNull("test", profile.getInstallableUnitProperty(testIU, "test"));
	}

	// This tests currently does not download anything. We need another sizing test to ensure sizes are retrieved
	public void testPerformSizingOSGiFrameworkNoArtifacts() {
		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());

		IProfile profile = createProfile("testPerformSizing", properties);
		for (Iterator<IInstallableUnit> it = getInstallableUnits(profile); it.hasNext();) {
			IProvisioningPlan plan = engine.createPlan(profile, null);
			IInstallableUnit doomed = it.next();
			plan.removeInstallableUnit(doomed);
			engine.perform(plan, new NullProgressMonitor());
		}
		final Sizing sizingPhase = new Sizing(100);
		PhaseSet phaseSet = new PhaseSet(new Phase[] {sizingPhase});

		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(createOSGiIU());
		IStatus result = engine.perform(plan, phaseSet, new NullProgressMonitor());
		assertTrue(result.isOK());
		assertTrue(sizingPhase.getDiskSize() == 0);
		assertTrue(sizingPhase.getDownloadSize() == 0);
	}

	// removing validate from engine api
	//	public void testValidateInstallOSGiFramework() {
	//		Map properties = new HashMap();
	//		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
	//
	//		IProfile profile = createProfile("testPerformInstallOSGiFramework", properties);
	//		for (Iterator it = getInstallableUnits(profile); it.hasNext();) {
	//			IInstallableUnit doomed = (IInstallableUnit) it.next();
	//			InstallableUnitOperand[] operands = new InstallableUnitOperand[] {new InstallableUnitOperand(createResolvedIU(doomed), null)};
	//			engine.perform(engine.createPlan(profile, null), new NullProgressMonitor());
	//		}
	//		PhaseSet phaseSet = new PhaseSetFactory();
	//		InstallableUnitOperand[] operands = new InstallableUnitOperand[] {new InstallableUnitOperand(null, createOSGiIU())};
	//		IStatus result = ((Engine) engine).validate(profile, phaseSet, operands, null, new NullProgressMonitor());
	//		assertTrue(result.isOK());
	//	}

	public void testPerformInstallOSGiFramework() {
		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());

		IProfile profile = createProfile("testPerformInstallOSGiFramework", properties);
		for (Iterator<IInstallableUnit> it = getInstallableUnits(profile); it.hasNext();) {
			IProvisioningPlan plan = engine.createPlan(profile, null);
			IInstallableUnit doomed = it.next();
			plan.removeInstallableUnit(doomed);
			engine.perform(plan, new NullProgressMonitor());
		}
		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(createOSGiIU());
		IStatus result = engine.perform(plan, new NullProgressMonitor());
		assertTrue(result.isOK());
		Iterator<IInstallableUnit> ius = getInstallableUnits(profile);
		assertTrue(ius.hasNext());
	}

	public void testPerformUpdateOSGiFramework() {
		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		IProfile profile = createProfile("testPerformUpdateOSGiFramework", properties);

		IInstallableUnit iu33 = createOSGiIU("3.3");
		IInstallableUnit iu34 = createOSGiIU("3.4");

		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(iu33);
		IStatus result = engine.perform(plan, new NullProgressMonitor());
		assertTrue(result.isOK());
		Iterator<IInstallableUnit> ius = profile.query(QueryUtil.createIUQuery(iu33), null).iterator();
		assertTrue(ius.hasNext());

		plan = engine.createPlan(profile, null);
		plan.updateInstallableUnit(iu33, iu34);
		result = engine.perform(plan, new NullProgressMonitor());
		assertTrue(result.isOK());
		ius = profile.query(QueryUtil.createIUQuery(iu34), null).iterator();
		assertTrue(ius.hasNext());
	}

	public void testPerformUninstallOSGiFramework() {

		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());

		IProfile profile = createProfile("testPerformUninstallOSGiFramework", properties);
		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.removeInstallableUnit(createOSGiIU());
		IStatus result = engine.perform(plan, new NullProgressMonitor());
		assertTrue(result.isOK());
		assertEmptyProfile(profile);
	}

	public void testPerformRollback() {

		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		IProfile profile = createProfile("testPerformRollback", properties);

		Iterator<IInstallableUnit> ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());

		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(createOSGiIU());
		plan.addInstallableUnit(createBadIU());
		IStatus result = engine.perform(plan, new NullProgressMonitor());
		assertFalse(result.isOK());

		ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());
	}

	// removing validate from engine api
	//	public void testValidateMissingAction() {
	//
	//		Map properties = new HashMap();
	//		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
	//		IProfile profile = createProfile("testPerformRollback", properties);
	//		PhaseSet phaseSet = new PhaseSetFactory();
	//
	//		Iterator ius = getInstallableUnits(profile);
	//		assertFalse(ius.hasNext());
	//
	//		InstallableUnitOperand[] operands = new InstallableUnitOperand[] {new InstallableUnitOperand(null, createOSGiIU()), new InstallableUnitOperand(null, createMissingActionIU())};
	//		IStatus result = ((Engine) engine).validate(profile, phaseSet, operands, null, new NullProgressMonitor());
	//		assertFalse(result.isOK());
	//
	//		Throwable t = result.getException();
	//		assertTrue(t instanceof MissingActionsException);
	//		MissingActionsException e = (MissingActionsException) t;
	//		assertEquals("org.eclipse.equinox.p2.touchpoint.eclipse.thisactionismissing", e.getMissingActions()[0].getActionId());
	//	}

	public void testPerformMissingAction() {

		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		IProfile profile = createProfile("testPerformMissingAction", properties);

		Iterator<IInstallableUnit> ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());

		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(createOSGiIU());
		plan.addInstallableUnit(createMissingActionIU());
		IStatus result = engine.perform(plan, new NullProgressMonitor());
		assertFalse(result.isOK());
		ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());
	}

	public void testPerformRollbackOnPhaseError() {

		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		IProfile profile = createProfile("testPerformRollbackOnError", properties);
		NPEPhase phase = new NPEPhase();
		PhaseSet phaseSet = new TestPhaseSet(phase);

		Iterator<IInstallableUnit> ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());

		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(createOSGiIU());

		IStatus result = engine.perform(plan, phaseSet, new NullProgressMonitor());
		assertTrue(result.toString().contains("java.lang.NullPointerException"));
		assertFalse(result.isOK());
		ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());
		assertTrue(phase.isConsistent());
	}

	public void testPerformRollbackOnActionError() {

		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		IProfile profile = createProfile("testPerformRollbackOnError", properties);
		ActionNPEPhase phase = new ActionNPEPhase();
		PhaseSet phaseSet = new TestPhaseSet(phase);

		Iterator<IInstallableUnit> ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());

		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(createOSGiIU());
		IStatus result = engine.perform(plan, phaseSet, new NullProgressMonitor());
		assertFalse(result.isOK());
		ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());
		assertTrue(phase.isConsistent());
	}

	public void testPerformForcedPhaseWithActionError() {
		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		IProfile profile = createProfile("testPerformForceWithActionError", properties);
		ActionNPEPhase phase = new ActionNPEPhase(true);
		PhaseSet phaseSet = new TestPhaseSet(phase);

		Iterator<?> ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());

		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(createOSGiIU());

		IStatus result = engine.perform(plan, phaseSet, new NullProgressMonitor());
		//		assertTrue(result.toString().contains("An error occurred during the org.eclipse.equinox.p2.tests.engine.EngineTest$ActionNPEPhase phase"));
		assertTrue(result.isOK());
		ius = getInstallableUnits(profile);
		assertTrue(ius.hasNext());
		assertTrue(phase.isConsistent());
	}

	public void testPerformForcedUninstallWithBadUninstallIUActionThrowsException() {
		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		IProfile profile = createProfile("testPerformForcedUninstallWithBadUninstallIUActionThrowsException", properties);

		// forcedUninstall is false by default
		IPhaseSet phaseSet = PhaseSetFactory.createDefaultPhaseSet();

		Iterator<IInstallableUnit> ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());

		IProvisioningPlan plan = engine.createPlan(profile, null);
		IInstallableUnit badUninstallIU = createBadUninstallIUThrowsException();
		plan.addInstallableUnit(badUninstallIU);
		IStatus result = engine.perform(plan, phaseSet, new NullProgressMonitor());
		assertTrue(result.isOK());
		ius = getInstallableUnits(profile);
		assertTrue(ius.hasNext());

		plan = engine.createPlan(profile, null);
		plan.removeInstallableUnit(badUninstallIU);
		result = engine.perform(plan, phaseSet, new NullProgressMonitor());
		assertFalse(result.isOK());
		ius = getInstallableUnits(profile);
		assertTrue(ius.hasNext());

		// this simulates a PhaseSetFactory with forcedUninstall set
		phaseSet = new TestPhaseSet(true);
		plan = engine.createPlan(profile, null);
		plan.removeInstallableUnit(badUninstallIU);

		result = engine.perform(plan, phaseSet, new NullProgressMonitor());
		//		assertTrue(result.toString().contains("An error occurred while uninstalling"));
		assertTrue(result.isOK());
		ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());
	}

	public void testPerformForcedUninstallWithBadUninstallIUActionReturnsError() {
		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		IProfile profile = createProfile("testPerformForcedUninstallWithBadUninstallIUActionReturnsError", properties);

		// forcedUninstall is false by default

		Iterator<IInstallableUnit> ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());

		IProvisioningPlan plan = engine.createPlan(profile, null);
		IInstallableUnit badUninstallIU = createBadUninstallIUReturnsError();
		plan.addInstallableUnit(badUninstallIU);

		IStatus result = engine.perform(plan, new NullProgressMonitor());
		assertTrue(result.isOK());
		ius = getInstallableUnits(profile);
		assertTrue(ius.hasNext());

		plan = engine.createPlan(profile, null);
		plan.removeInstallableUnit(badUninstallIU);
		result = engine.perform(plan, new NullProgressMonitor());
		assertFalse(result.isOK());
		ius = getInstallableUnits(profile);
		assertTrue(ius.hasNext());

		// this simulates a PhaseSetFactory with forcedUninstall set
		IPhaseSet phaseSet = new TestPhaseSet(true);
		plan = engine.createPlan(profile, null);
		plan.removeInstallableUnit(badUninstallIU);
		result = engine.perform(plan, phaseSet, new NullProgressMonitor());
		assertTrue(result.isOK());
		ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());
	}

	public void testOrphanedIUProperty() {
		IProfile profile = createProfile("testOrphanedIUProperty");
		IInstallableUnit iu = createIU("someIU");
		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.setInstallableUnitProfileProperty(iu, "key", "value");
		IStatus result = engine.perform(plan, new NullProgressMonitor());
		assertTrue(result.isOK());
		assertFalse(profile.getInstallableUnitProperties(iu).containsKey("key"));

		plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(iu);
		plan.setInstallableUnitProfileProperty(iu, "adifferentkey", "value");
		result = engine.perform(plan, new NullProgressMonitor());
		assertTrue(result.isOK());
		assertTrue(profile.getInstallableUnitProperties(iu).containsKey("adifferentkey"));
		assertFalse(profile.getInstallableUnitProperties(iu).containsKey("key"));
	}

	private IInstallableUnit createOSGiIU() {
		return createOSGiIU("3.3.1.R33x_v20070828");
	}

	private IInstallableUnit createOSGiIU(String version) {
		MetadataFactory.InstallableUnitDescription description = new MetadataFactory.InstallableUnitDescription();
		description.setId("org.eclipse.osgi");
		description.setVersion(Version.create(version));
		description.setTouchpointType(AbstractProvisioningTest.TOUCHPOINT_OSGI);
		Map<String, Object> touchpointData = new HashMap<>();
		String manifest = "Manifest-Version: 1.0\r\n" + "Bundle-Activator: org.eclipse.osgi.framework.internal.core.SystemBundl\r\n" + " eActivator\r\n" + "Bundle-RequiredExecutionEnvironment: J2SE-1.4,OSGi/Minimum-1.0\r\n" + "Export-Package: org.eclipse.osgi.event;version=\"1.0\",org.eclipse.osgi.\r\n" + " framework.console;version=\"1.0\",org.eclipse.osgi.framework.eventmgr;v\r\n" + " ersion=\"1.0\",org.eclipse.osgi.framework.log;version=\"1.0\",org.eclipse\r\n" + " .osgi.service.datalocation;version=\"1.0\",org.eclipse.osgi.service.deb\r\n" + " ug;version=\"1.0\",org.eclipse.osgi.service.environment;version=\"1.0\",o\r\n" + " rg.eclipse.osgi.service.localization;version=\"1.0\",org.eclipse.osgi.s\r\n" + " ervice.pluginconversion;version=\"1.0\",org.eclipse.osgi.service.resolv\r\n"
				+ " er;version=\"1.1\",org.eclipse.osgi.service.runnable;version=\"1.0\",org.\r\n" + " eclipse.osgi.service.urlconversion;version=\"1.0\",org.eclipse.osgi.sto\r\n" + " ragemanager;version=\"1.0\",org.eclipse.osgi.util;version=\"1.0\",org.osg\r\n" + " i.framework;version=\"1.3\",org.osgi.service.condpermadmin;version=\"1.0\r\n" + " \",org.osgi.service.packageadmin;version=\"1.2\",org.osgi.service.permis\r\n" + " sionadmin;version=\"1.2\",org.osgi.service.startlevel;version=\"1.0\",org\r\n" + " .osgi.service.url;version=\"1.0\",org.osgi.util.tracker;version=\"1.3.2\"\r\n" + " ,org.eclipse.core.runtime.adaptor;x-friends:=\"org.eclipse.core.runtim\r\n" + " e\",org.eclipse.core.runtime.internal.adaptor;x-internal:=true,org.ecl\r\n"
				+ " ipse.core.runtime.internal.stats;x-friends:=\"org.eclipse.core.runtime\r\n" + " \",org.eclipse.osgi.baseadaptor;x-internal:=true,org.eclipse.osgi.base\r\n" + " adaptor.bundlefile;x-internal:=true,org.eclipse.osgi.baseadaptor.hook\r\n" + " s;x-internal:=true,org.eclipse.osgi.baseadaptor.loader;x-internal:=tr\r\n" + " ue,org.eclipse.osgi.framework.adaptor;x-internal:=true,org.eclipse.os\r\n" + " gi.framework.debug;x-internal:=true,org.eclipse.osgi.framework.intern\r\n" + " al.core;x-internal:=true,org.eclipse.osgi.framework.internal.protocol\r\n" + " ;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.bundle\r\n" + " entry;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.b\r\n"
				+ " undleresource;x-internal:=true,org.eclipse.osgi.framework.internal.pr\r\n" + " otocol.reference;x-internal:=true,org.eclipse.osgi.framework.internal\r\n" + " .reliablefile;x-internal:=true,org.eclipse.osgi.framework.launcher;x-\r\n" + " internal:=true,org.eclipse.osgi.framework.util;x-internal:=true,org.e\r\n" + " clipse.osgi.internal.baseadaptor;x-internal:=true,org.eclipse.osgi.in\r\n" + " ternal.module;x-internal:=true,org.eclipse.osgi.internal.profile;x-in\r\n" + " ternal:=true,org.eclipse.osgi.internal.resolver;x-internal:=true,org.\r\n" + " eclipse.osgi.internal.verifier;x-internal:=true,org.eclipse.osgi.inte\r\n" + " rnal.provisional.verifier;x-friends:=\"org.eclipse.ui.workbench\"\r\n" + "Bundle-Version: 3.3.0.v20060925\r\n" + "Eclipse-SystemBundle: true\r\n"
				+ "Bundle-Copyright: %copyright\r\n" + "Bundle-Name: %systemBundle\r\n" + "Bundle-Description: %systemBundle\r\n" + "Bundle-DocUrl: http://www.eclipse.org\r\n" + "Bundle-ManifestVersion: 2\r\n" + "Export-Service: org.osgi.service.packageadmin.PackageAdmin,org.osgi.se\r\n" + " rvice.permissionadmin.PermissionAdmin,org.osgi.service.startlevel.Sta\r\n" + " rtLevel,org.eclipse.osgi.service.debug.DebugOptions\r\n" + "Bundle-Vendor: %eclipse.org\r\n" + "Main-Class: org.eclipse.core.runtime.adaptor.EclipseStarter\r\n" + "Bundle-SymbolicName: org.eclipse.osgi; singleton:=true\r\n" + "Bundle-Localization: systembundle\r\n" + "Eclipse-ExtensibleAPI: true\r\n" + "\r\n" + "";
		touchpointData.put("manifest", manifest);
		//touchpointData.put("install", "installBundle(bundle:${artifact});");
		//touchpointData.put("uninstall", "uninstallBundle(bundle:${artifact});");

		IInstallableUnitFragment[] cus = new IInstallableUnitFragment[1];
		InstallableUnitFragmentDescription desc = new InstallableUnitFragmentDescription();
		desc.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		IInstallableUnitFragment fragment = MetadataFactory.createInstallableUnitFragment(desc);
		cus[0] = fragment;

		//IArtifactKey key = new ArtifactKey("eclipse", "plugin", "org.eclipse.osgi", Version.create("3.3.1.R33x_v20070828"));
		//iu.setArtifacts(new IArtifactKey[] {key});

		IInstallableUnit iu = MetadataFactory.createInstallableUnit(description);
		return MetadataFactory.createResolvedInstallableUnit(iu, cus);
	}

	private IInstallableUnit createBadIU() {
		InstallableUnitDescription description = new MetadataFactory.InstallableUnitDescription();
		description.setId("org.eclipse.osgi.bad");
		description.setVersion(Version.create("3.3.1.R33x_v20070828"));
		description.setTouchpointType(AbstractProvisioningTest.TOUCHPOINT_OSGI);
		Map<String, Object> touchpointData = new HashMap<>();
		String manifest = "Manifest-Version: 1.0\r\n" + "Bundle-Activator: org.eclipse.osgi.framework.internal.core.SystemBundl\r\n" + " eActivator\r\n" + "Bundle-RequiredExecutionEnvironment: J2SE-1.4,OSGi/Minimum-1.0\r\n" + "Export-Package: org.eclipse.osgi.event;version=\"1.0\",org.eclipse.osgi.\r\n" + " framework.console;version=\"1.0\",org.eclipse.osgi.framework.eventmgr;v\r\n" + " ersion=\"1.0\",org.eclipse.osgi.framework.log;version=\"1.0\",org.eclipse\r\n" + " .osgi.service.datalocation;version=\"1.0\",org.eclipse.osgi.service.deb\r\n" + " ug;version=\"1.0\",org.eclipse.osgi.service.environment;version=\"1.0\",o\r\n" + " rg.eclipse.osgi.service.localization;version=\"1.0\",org.eclipse.osgi.s\r\n" + " ervice.pluginconversion;version=\"1.0\",org.eclipse.osgi.service.resolv\r\n"
				+ " er;version=\"1.1\",org.eclipse.osgi.service.runnable;version=\"1.0\",org.\r\n" + " eclipse.osgi.service.urlconversion;version=\"1.0\",org.eclipse.osgi.sto\r\n" + " ragemanager;version=\"1.0\",org.eclipse.osgi.util;version=\"1.0\",org.osg\r\n" + " i.framework;version=\"1.3\",org.osgi.service.condpermadmin;version=\"1.0\r\n" + " \",org.osgi.service.packageadmin;version=\"1.2\",org.osgi.service.permis\r\n" + " sionadmin;version=\"1.2\",org.osgi.service.startlevel;version=\"1.0\",org\r\n" + " .osgi.service.url;version=\"1.0\",org.osgi.util.tracker;version=\"1.3.2\"\r\n" + " ,org.eclipse.core.runtime.adaptor;x-friends:=\"org.eclipse.core.runtim\r\n" + " e\",org.eclipse.core.runtime.internal.adaptor;x-internal:=true,org.ecl\r\n"
				+ " ipse.core.runtime.internal.stats;x-friends:=\"org.eclipse.core.runtime\r\n" + " \",org.eclipse.osgi.baseadaptor;x-internal:=true,org.eclipse.osgi.base\r\n" + " adaptor.bundlefile;x-internal:=true,org.eclipse.osgi.baseadaptor.hook\r\n" + " s;x-internal:=true,org.eclipse.osgi.baseadaptor.loader;x-internal:=tr\r\n" + " ue,org.eclipse.osgi.framework.adaptor;x-internal:=true,org.eclipse.os\r\n" + " gi.framework.debug;x-internal:=true,org.eclipse.osgi.framework.intern\r\n" + " al.core;x-internal:=true,org.eclipse.osgi.framework.internal.protocol\r\n" + " ;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.bundle\r\n" + " entry;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.b\r\n"
				+ " undleresource;x-internal:=true,org.eclipse.osgi.framework.internal.pr\r\n" + " otocol.reference;x-internal:=true,org.eclipse.osgi.framework.internal\r\n" + " .reliablefile;x-internal:=true,org.eclipse.osgi.framework.launcher;x-\r\n" + " internal:=true,org.eclipse.osgi.framework.util;x-internal:=true,org.e\r\n" + " clipse.osgi.internal.baseadaptor;x-internal:=true,org.eclipse.osgi.in\r\n" + " ternal.module;x-internal:=true,org.eclipse.osgi.internal.profile;x-in\r\n" + " ternal:=true,org.eclipse.osgi.internal.resolver;x-internal:=true,org.\r\n" + " eclipse.osgi.internal.verifier;x-internal:=true,org.eclipse.osgi.inte\r\n" + " rnal.provisional.verifier;x-friends:=\"org.eclipse.ui.workbench\"\r\n" + "Bundle-Version: 3.3.0.v20060925\r\n" + "Eclipse-SystemBundle: true\r\n"
				+ "Bundle-Copyright: %copyright\r\n" + "Bundle-Name: %systemBundle\r\n" + "Bundle-Description: %systemBundle\r\n" + "Bundle-DocUrl: http://www.eclipse.org\r\n" + "Bundle-ManifestVersion: 2\r\n" + "Export-Service: org.osgi.service.packageadmin.PackageAdmin,org.osgi.se\r\n" + " rvice.permissionadmin.PermissionAdmin,org.osgi.service.startlevel.Sta\r\n" + " rtLevel,org.eclipse.osgi.service.debug.DebugOptions\r\n" + "Bundle-Vendor: %eclipse.org\r\n" + "Main-Class: org.eclipse.core.runtime.adaptor.EclipseStarter\r\n" + "Bundle-SymbolicName: org.eclipse.osgi; singleton:=true\r\n" + "Bundle-Localization: systembundle\r\n" + "Eclipse-ExtensibleAPI: true\r\n" + "\r\n" + "";
		touchpointData.put("manifest", manifest);
		touchpointData.put("install", "BAD");

		IInstallableUnitFragment[] cus = new IInstallableUnitFragment[1];
		InstallableUnitFragmentDescription desc = new InstallableUnitFragmentDescription();
		desc.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		cus[0] = MetadataFactory.createInstallableUnitFragment(desc);

		//IArtifactKey key = new ArtifactKey("eclipse", "plugin", "org.eclipse.osgi", Version.create("3.3.1.R33x_v20070828"));
		//iu.setArtifacts(new IArtifactKey[] {key});

		IInstallableUnit iu = MetadataFactory.createInstallableUnit(description);
		return MetadataFactory.createResolvedInstallableUnit(iu, cus);
	}

	private IInstallableUnit createBadUninstallIUReturnsError() {
		InstallableUnitDescription description = new MetadataFactory.InstallableUnitDescription();
		description.setId("org.eclipse.osgi.bad");
		description.setVersion(Version.create("3.3.1.R33x_v20070828"));
		description.setTouchpointType(AbstractProvisioningTest.TOUCHPOINT_OSGI);
		Map<String, Object> touchpointData = new HashMap<>();
		String manifest = "Manifest-Version: 1.0\r\n" + "Bundle-Activator: org.eclipse.osgi.framework.internal.core.SystemBundl\r\n" + " eActivator\r\n" + "Bundle-RequiredExecutionEnvironment: J2SE-1.4,OSGi/Minimum-1.0\r\n" + "Export-Package: org.eclipse.osgi.event;version=\"1.0\",org.eclipse.osgi.\r\n" + " framework.console;version=\"1.0\",org.eclipse.osgi.framework.eventmgr;v\r\n" + " ersion=\"1.0\",org.eclipse.osgi.framework.log;version=\"1.0\",org.eclipse\r\n" + " .osgi.service.datalocation;version=\"1.0\",org.eclipse.osgi.service.deb\r\n" + " ug;version=\"1.0\",org.eclipse.osgi.service.environment;version=\"1.0\",o\r\n" + " rg.eclipse.osgi.service.localization;version=\"1.0\",org.eclipse.osgi.s\r\n" + " ervice.pluginconversion;version=\"1.0\",org.eclipse.osgi.service.resolv\r\n"
				+ " er;version=\"1.1\",org.eclipse.osgi.service.runnable;version=\"1.0\",org.\r\n" + " eclipse.osgi.service.urlconversion;version=\"1.0\",org.eclipse.osgi.sto\r\n" + " ragemanager;version=\"1.0\",org.eclipse.osgi.util;version=\"1.0\",org.osg\r\n" + " i.framework;version=\"1.3\",org.osgi.service.condpermadmin;version=\"1.0\r\n" + " \",org.osgi.service.packageadmin;version=\"1.2\",org.osgi.service.permis\r\n" + " sionadmin;version=\"1.2\",org.osgi.service.startlevel;version=\"1.0\",org\r\n" + " .osgi.service.url;version=\"1.0\",org.osgi.util.tracker;version=\"1.3.2\"\r\n" + " ,org.eclipse.core.runtime.adaptor;x-friends:=\"org.eclipse.core.runtim\r\n" + " e\",org.eclipse.core.runtime.internal.adaptor;x-internal:=true,org.ecl\r\n"
				+ " ipse.core.runtime.internal.stats;x-friends:=\"org.eclipse.core.runtime\r\n" + " \",org.eclipse.osgi.baseadaptor;x-internal:=true,org.eclipse.osgi.base\r\n" + " adaptor.bundlefile;x-internal:=true,org.eclipse.osgi.baseadaptor.hook\r\n" + " s;x-internal:=true,org.eclipse.osgi.baseadaptor.loader;x-internal:=tr\r\n" + " ue,org.eclipse.osgi.framework.adaptor;x-internal:=true,org.eclipse.os\r\n" + " gi.framework.debug;x-internal:=true,org.eclipse.osgi.framework.intern\r\n" + " al.core;x-internal:=true,org.eclipse.osgi.framework.internal.protocol\r\n" + " ;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.bundle\r\n" + " entry;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.b\r\n"
				+ " undleresource;x-internal:=true,org.eclipse.osgi.framework.internal.pr\r\n" + " otocol.reference;x-internal:=true,org.eclipse.osgi.framework.internal\r\n" + " .reliablefile;x-internal:=true,org.eclipse.osgi.framework.launcher;x-\r\n" + " internal:=true,org.eclipse.osgi.framework.util;x-internal:=true,org.e\r\n" + " clipse.osgi.internal.baseadaptor;x-internal:=true,org.eclipse.osgi.in\r\n" + " ternal.module;x-internal:=true,org.eclipse.osgi.internal.profile;x-in\r\n" + " ternal:=true,org.eclipse.osgi.internal.resolver;x-internal:=true,org.\r\n" + " eclipse.osgi.internal.verifier;x-internal:=true,org.eclipse.osgi.inte\r\n" + " rnal.provisional.verifier;x-friends:=\"org.eclipse.ui.workbench\"\r\n" + "Bundle-Version: 3.3.0.v20060925\r\n" + "Eclipse-SystemBundle: true\r\n"
				+ "Bundle-Copyright: %copyright\r\n" + "Bundle-Name: %systemBundle\r\n" + "Bundle-Description: %systemBundle\r\n" + "Bundle-DocUrl: http://www.eclipse.org\r\n" + "Bundle-ManifestVersion: 2\r\n" + "Export-Service: org.osgi.service.packageadmin.PackageAdmin,org.osgi.se\r\n" + " rvice.permissionadmin.PermissionAdmin,org.osgi.service.startlevel.Sta\r\n" + " rtLevel,org.eclipse.osgi.service.debug.DebugOptions\r\n" + "Bundle-Vendor: %eclipse.org\r\n" + "Main-Class: org.eclipse.core.runtime.adaptor.EclipseStarter\r\n" + "Bundle-SymbolicName: org.eclipse.osgi; singleton:=true\r\n" + "Bundle-Localization: systembundle\r\n" + "Eclipse-ExtensibleAPI: true\r\n" + "\r\n" + "";
		touchpointData.put("manifest", manifest);
		touchpointData.put("uninstall", "setProgramProperty(missing_mandatory_parameters:xyz)");

		IInstallableUnitFragment[] cus = new IInstallableUnitFragment[1];
		InstallableUnitFragmentDescription desc = new InstallableUnitFragmentDescription();
		desc.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		cus[0] = MetadataFactory.createInstallableUnitFragment(desc);

		//IArtifactKey key = new ArtifactKey("eclipse", "plugin", "org.eclipse.osgi", Version.create("3.3.1.R33x_v20070828"));
		//iu.setArtifacts(new IArtifactKey[] {key});

		IInstallableUnit iu = MetadataFactory.createInstallableUnit(description);
		return MetadataFactory.createResolvedInstallableUnit(iu, cus);
	}

	private IInstallableUnit createBadUninstallIUThrowsException() {
		InstallableUnitDescription description = new MetadataFactory.InstallableUnitDescription();
		description.setId("org.eclipse.osgi.bad");
		description.setVersion(Version.create("3.3.1.R33x_v20070828"));
		description.setTouchpointType(AbstractProvisioningTest.TOUCHPOINT_OSGI);
		Map<String, Object> touchpointData = new HashMap<>();
		String manifest = "Manifest-Version: 1.0\r\n" + "Bundle-Activator: org.eclipse.osgi.framework.internal.core.SystemBundl\r\n" + " eActivator\r\n" + "Bundle-RequiredExecutionEnvironment: J2SE-1.4,OSGi/Minimum-1.0\r\n" + "Export-Package: org.eclipse.osgi.event;version=\"1.0\",org.eclipse.osgi.\r\n" + " framework.console;version=\"1.0\",org.eclipse.osgi.framework.eventmgr;v\r\n" + " ersion=\"1.0\",org.eclipse.osgi.framework.log;version=\"1.0\",org.eclipse\r\n" + " .osgi.service.datalocation;version=\"1.0\",org.eclipse.osgi.service.deb\r\n" + " ug;version=\"1.0\",org.eclipse.osgi.service.environment;version=\"1.0\",o\r\n" + " rg.eclipse.osgi.service.localization;version=\"1.0\",org.eclipse.osgi.s\r\n" + " ervice.pluginconversion;version=\"1.0\",org.eclipse.osgi.service.resolv\r\n"
				+ " er;version=\"1.1\",org.eclipse.osgi.service.runnable;version=\"1.0\",org.\r\n" + " eclipse.osgi.service.urlconversion;version=\"1.0\",org.eclipse.osgi.sto\r\n" + " ragemanager;version=\"1.0\",org.eclipse.osgi.util;version=\"1.0\",org.osg\r\n" + " i.framework;version=\"1.3\",org.osgi.service.condpermadmin;version=\"1.0\r\n" + " \",org.osgi.service.packageadmin;version=\"1.2\",org.osgi.service.permis\r\n" + " sionadmin;version=\"1.2\",org.osgi.service.startlevel;version=\"1.0\",org\r\n" + " .osgi.service.url;version=\"1.0\",org.osgi.util.tracker;version=\"1.3.2\"\r\n" + " ,org.eclipse.core.runtime.adaptor;x-friends:=\"org.eclipse.core.runtim\r\n" + " e\",org.eclipse.core.runtime.internal.adaptor;x-internal:=true,org.ecl\r\n"
				+ " ipse.core.runtime.internal.stats;x-friends:=\"org.eclipse.core.runtime\r\n" + " \",org.eclipse.osgi.baseadaptor;x-internal:=true,org.eclipse.osgi.base\r\n" + " adaptor.bundlefile;x-internal:=true,org.eclipse.osgi.baseadaptor.hook\r\n" + " s;x-internal:=true,org.eclipse.osgi.baseadaptor.loader;x-internal:=tr\r\n" + " ue,org.eclipse.osgi.framework.adaptor;x-internal:=true,org.eclipse.os\r\n" + " gi.framework.debug;x-internal:=true,org.eclipse.osgi.framework.intern\r\n" + " al.core;x-internal:=true,org.eclipse.osgi.framework.internal.protocol\r\n" + " ;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.bundle\r\n" + " entry;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.b\r\n"
				+ " undleresource;x-internal:=true,org.eclipse.osgi.framework.internal.pr\r\n" + " otocol.reference;x-internal:=true,org.eclipse.osgi.framework.internal\r\n" + " .reliablefile;x-internal:=true,org.eclipse.osgi.framework.launcher;x-\r\n" + " internal:=true,org.eclipse.osgi.framework.util;x-internal:=true,org.e\r\n" + " clipse.osgi.internal.baseadaptor;x-internal:=true,org.eclipse.osgi.in\r\n" + " ternal.module;x-internal:=true,org.eclipse.osgi.internal.profile;x-in\r\n" + " ternal:=true,org.eclipse.osgi.internal.resolver;x-internal:=true,org.\r\n" + " eclipse.osgi.internal.verifier;x-internal:=true,org.eclipse.osgi.inte\r\n" + " rnal.provisional.verifier;x-friends:=\"org.eclipse.ui.workbench\"\r\n" + "Bundle-Version: 3.3.0.v20060925\r\n" + "Eclipse-SystemBundle: true\r\n"
				+ "Bundle-Copyright: %copyright\r\n" + "Bundle-Name: %systemBundle\r\n" + "Bundle-Description: %systemBundle\r\n" + "Bundle-DocUrl: http://www.eclipse.org\r\n" + "Bundle-ManifestVersion: 2\r\n" + "Export-Service: org.osgi.service.packageadmin.PackageAdmin,org.osgi.se\r\n" + " rvice.permissionadmin.PermissionAdmin,org.osgi.service.startlevel.Sta\r\n" + " rtLevel,org.eclipse.osgi.service.debug.DebugOptions\r\n" + "Bundle-Vendor: %eclipse.org\r\n" + "Main-Class: org.eclipse.core.runtime.adaptor.EclipseStarter\r\n" + "Bundle-SymbolicName: org.eclipse.osgi; singleton:=true\r\n" + "Bundle-Localization: systembundle\r\n" + "Eclipse-ExtensibleAPI: true\r\n" + "\r\n" + "";
		touchpointData.put("manifest", manifest);
		touchpointData.put("uninstall", "thisactionismissing()");

		IInstallableUnitFragment[] cus = new IInstallableUnitFragment[1];
		InstallableUnitFragmentDescription desc = new InstallableUnitFragmentDescription();
		desc.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		cus[0] = MetadataFactory.createInstallableUnitFragment(desc);

		//IArtifactKey key = new ArtifactKey("eclipse", "plugin", "org.eclipse.osgi", Version.create("3.3.1.R33x_v20070828"));
		//iu.setArtifacts(new IArtifactKey[] {key});

		IInstallableUnit iu = MetadataFactory.createInstallableUnit(description);
		return MetadataFactory.createResolvedInstallableUnit(iu, cus);
	}

	private IInstallableUnit createMissingActionIU() {
		InstallableUnitDescription description = new MetadataFactory.InstallableUnitDescription();
		description.setId("org.eclipse.osgi.bad");
		description.setVersion(Version.create("3.3.1.R33x_v20070828"));
		description.setTouchpointType(AbstractProvisioningTest.TOUCHPOINT_OSGI);
		Map<String, Object> touchpointData = new HashMap<>();
		String manifest = "Manifest-Version: 1.0\r\n" + "Bundle-Activator: org.eclipse.osgi.framework.internal.core.SystemBundl\r\n" + " eActivator\r\n" + "Bundle-RequiredExecutionEnvironment: J2SE-1.4,OSGi/Minimum-1.0\r\n" + "Export-Package: org.eclipse.osgi.event;version=\"1.0\",org.eclipse.osgi.\r\n" + " framework.console;version=\"1.0\",org.eclipse.osgi.framework.eventmgr;v\r\n" + " ersion=\"1.0\",org.eclipse.osgi.framework.log;version=\"1.0\",org.eclipse\r\n" + " .osgi.service.datalocation;version=\"1.0\",org.eclipse.osgi.service.deb\r\n" + " ug;version=\"1.0\",org.eclipse.osgi.service.environment;version=\"1.0\",o\r\n" + " rg.eclipse.osgi.service.localization;version=\"1.0\",org.eclipse.osgi.s\r\n" + " ervice.pluginconversion;version=\"1.0\",org.eclipse.osgi.service.resolv\r\n"
				+ " er;version=\"1.1\",org.eclipse.osgi.service.runnable;version=\"1.0\",org.\r\n" + " eclipse.osgi.service.urlconversion;version=\"1.0\",org.eclipse.osgi.sto\r\n" + " ragemanager;version=\"1.0\",org.eclipse.osgi.util;version=\"1.0\",org.osg\r\n" + " i.framework;version=\"1.3\",org.osgi.service.condpermadmin;version=\"1.0\r\n" + " \",org.osgi.service.packageadmin;version=\"1.2\",org.osgi.service.permis\r\n" + " sionadmin;version=\"1.2\",org.osgi.service.startlevel;version=\"1.0\",org\r\n" + " .osgi.service.url;version=\"1.0\",org.osgi.util.tracker;version=\"1.3.2\"\r\n" + " ,org.eclipse.core.runtime.adaptor;x-friends:=\"org.eclipse.core.runtim\r\n" + " e\",org.eclipse.core.runtime.internal.adaptor;x-internal:=true,org.ecl\r\n"
				+ " ipse.core.runtime.internal.stats;x-friends:=\"org.eclipse.core.runtime\r\n" + " \",org.eclipse.osgi.baseadaptor;x-internal:=true,org.eclipse.osgi.base\r\n" + " adaptor.bundlefile;x-internal:=true,org.eclipse.osgi.baseadaptor.hook\r\n" + " s;x-internal:=true,org.eclipse.osgi.baseadaptor.loader;x-internal:=tr\r\n" + " ue,org.eclipse.osgi.framework.adaptor;x-internal:=true,org.eclipse.os\r\n" + " gi.framework.debug;x-internal:=true,org.eclipse.osgi.framework.intern\r\n" + " al.core;x-internal:=true,org.eclipse.osgi.framework.internal.protocol\r\n" + " ;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.bundle\r\n" + " entry;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.b\r\n"
				+ " undleresource;x-internal:=true,org.eclipse.osgi.framework.internal.pr\r\n" + " otocol.reference;x-internal:=true,org.eclipse.osgi.framework.internal\r\n" + " .reliablefile;x-internal:=true,org.eclipse.osgi.framework.launcher;x-\r\n" + " internal:=true,org.eclipse.osgi.framework.util;x-internal:=true,org.e\r\n" + " clipse.osgi.internal.baseadaptor;x-internal:=true,org.eclipse.osgi.in\r\n" + " ternal.module;x-internal:=true,org.eclipse.osgi.internal.profile;x-in\r\n" + " ternal:=true,org.eclipse.osgi.internal.resolver;x-internal:=true,org.\r\n" + " eclipse.osgi.internal.verifier;x-internal:=true,org.eclipse.osgi.inte\r\n" + " rnal.provisional.verifier;x-friends:=\"org.eclipse.ui.workbench\"\r\n" + "Bundle-Version: 3.3.0.v20060925\r\n" + "Eclipse-SystemBundle: true\r\n"
				+ "Bundle-Copyright: %copyright\r\n" + "Bundle-Name: %systemBundle\r\n" + "Bundle-Description: %systemBundle\r\n" + "Bundle-DocUrl: http://www.eclipse.org\r\n" + "Bundle-ManifestVersion: 2\r\n" + "Export-Service: org.osgi.service.packageadmin.PackageAdmin,org.osgi.se\r\n" + " rvice.permissionadmin.PermissionAdmin,org.osgi.service.startlevel.Sta\r\n" + " rtLevel,org.eclipse.osgi.service.debug.DebugOptions\r\n" + "Bundle-Vendor: %eclipse.org\r\n" + "Main-Class: org.eclipse.core.runtime.adaptor.EclipseStarter\r\n" + "Bundle-SymbolicName: org.eclipse.osgi; singleton:=true\r\n" + "Bundle-Localization: systembundle\r\n" + "Eclipse-ExtensibleAPI: true\r\n" + "\r\n" + "";
		touchpointData.put("manifest", manifest);
		touchpointData.put("install", "thisactionismissing()");

		IInstallableUnitFragment[] cus = new IInstallableUnitFragment[1];
		InstallableUnitFragmentDescription desc = new InstallableUnitFragmentDescription();
		desc.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		cus[0] = MetadataFactory.createInstallableUnitFragment(desc);

		//IArtifactKey key = new ArtifactKey("eclipse", "plugin", "org.eclipse.osgi", Version.create("3.3.1.R33x_v20070828"));
		//iu.setArtifacts(new IArtifactKey[] {key});

		IInstallableUnit iu = MetadataFactory.createInstallableUnit(description);
		return MetadataFactory.createResolvedInstallableUnit(iu, cus);
	}

	public void testIncompatibleProfile() {

		IProfile profile = new IProfile() {
			@Override
			public IQueryResult<IInstallableUnit> available(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
				return new Collector<>();
			}

			@Override
			public Map<String, String> getInstallableUnitProperties(IInstallableUnit iu) {
				return null;
			}

			@Override
			public String getInstallableUnitProperty(IInstallableUnit iu, String key) {
				return null;
			}

			@Override
			public String getProfileId() {
				return null;
			}

			@Override
			public Map<String, String> getProperties() {
				return null;
			}

			@Override
			public String getProperty(String key) {
				return null;
			}

			@Override
			public IProvisioningAgent getProvisioningAgent() {
				return getAgent();
			}

			@Override
			public long getTimestamp() {
				return 0;
			}

			@Override
			public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
				return new Collector<>();
			}
		};
		IProvisioningPlan plan = engine.createPlan(profile, null);
		plan.addInstallableUnit(createOSGiIU());
		assertThrows(IllegalArgumentException.class, () -> engine.perform(plan, new NullProgressMonitor()));
	}
}
