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

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.engine.phases.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;

/**
 * Simple test of the engine API.
 * 
 * Note:
 * Currently you MUST have previously generated metadata from a 3.3.1 install.
 * There are ordering dependencies for the tests temporarily 
 */
public class EngineTest extends AbstractProvisioningTest {

	private static class NPEPhase extends Phase {
		protected NPEPhase(int weight) {
			super("NPE", 1);
		}

		protected ProvisioningAction[] getActions(Operand operand) {
			throw new NullPointerException();
		}
	}

	private static class NPEPhaseSet extends PhaseSet {
		public NPEPhaseSet() {
			super(new Phase[] {new Collect(100), new Unconfigure(10), new Uninstall(50), new Property(1), new CheckTrust(10), new Install(50), new Configure(10), new NPEPhase(1)});
		}
	}

	private ServiceReference engineRef;
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
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return directory.delete();
	}

	protected void setUp() throws Exception {
		engineRef = TestActivator.getContext().getServiceReference(IEngine.SERVICE_NAME);
		engine = (IEngine) TestActivator.getContext().getService(engineRef);
		testProvisioning = new File(System.getProperty("java.io.tmpdir"), "testProvisioning");
		deleteDirectory(testProvisioning);
		testProvisioning.mkdir();
	}

	protected void tearDown() throws Exception {
		engine = null;
		TestActivator.getContext().ungetService(engineRef);
		deleteDirectory(testProvisioning);
	}

	public void testNullProfile() {

		IProfile profile = null;
		PhaseSet phaseSet = new DefaultPhaseSet();
		InstallableUnitOperand[] operands = new InstallableUnitOperand[] {};
		try {
			engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testNullPhaseSet() {

		IProfile profile = createProfile("test");
		PhaseSet phaseSet = null;
		InstallableUnitOperand[] operands = new InstallableUnitOperand[] {};
		try {
			engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testNullOperands() {

		IProfile profile = createProfile("test");
		PhaseSet phaseSet = new DefaultPhaseSet();
		InstallableUnitOperand[] operands = null;
		try {
			engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
			fail();
		} catch (IllegalArgumentException expected) {
			//expected
		}
	}

	public void testEmptyOperands() {

		IProfile profile = createProfile("test");
		PhaseSet phaseSet = new DefaultPhaseSet();
		InstallableUnitOperand[] operands = new InstallableUnitOperand[] {};
		IStatus result = engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		assertTrue(result.isOK());
	}

	public void testEmptyPhaseSet() {

		IProfile profile = createProfile("testEmptyPhaseSet");
		PhaseSet phaseSet = new PhaseSet(new Phase[] {}) {
			// empty PhaseSet
		};

		InstallableUnitOperand op = new InstallableUnitOperand(createResolvedIU(createIU("name")), null);
		InstallableUnitOperand[] operands = new InstallableUnitOperand[] {op};
		IStatus result = engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		assertTrue(result.isOK());
	}

	public void testPerformSingleNullOperand() {
		try {
			new InstallableUnitOperand(null, null);
			fail("Should not allow null operand");
		} catch (RuntimeException e) {
			//expected
		}
	}

	public void testPerformPropertyInstallUninstall() {

		IProfile profile = createProfile("testPerformPropertyInstallUninstall");
		PhaseSet phaseSet = new DefaultPhaseSet();

		PropertyOperand propOp = new PropertyOperand("test", null, "test");
		IInstallableUnit testIU = createResolvedIU(createIU("test"));
		InstallableUnitOperand iuOp = new InstallableUnitOperand(null, testIU);
		InstallableUnitPropertyOperand iuPropOp = new InstallableUnitPropertyOperand(testIU, "test", null, "test");

		Operand[] operands = new Operand[] {propOp, iuOp, iuPropOp};
		IStatus result = engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		assertTrue(result.isOK());
		assertEquals("test", profile.getProperty("test"));
		assertEquals("test", profile.getInstallableUnitProperty(testIU, "test"));

		PropertyOperand uninstallPropOp = new PropertyOperand("test", "test", null);
		InstallableUnitPropertyOperand uninstallIuPropOp = new InstallableUnitPropertyOperand(testIU, "test", "test", null);
		operands = new Operand[] {uninstallPropOp, uninstallIuPropOp};
		result = engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		assertTrue(result.isOK());
		assertNull("test", profile.getProperty("test"));
		assertNull("test", profile.getInstallableUnitProperty(testIU, "test"));
	}

	// This tests currently does not download anything. We need another sizing test to ensure sizes are retrieved
	public void testPerformSizingOSGiFrameworkNoArtifacts() {
		Map properties = new HashMap();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());

		IProfile profile = createProfile("testPerformSizing", null, properties);
		for (Iterator it = getInstallableUnits(profile); it.hasNext();) {
			PhaseSet phaseSet = new DefaultPhaseSet();
			IInstallableUnit doomed = (IInstallableUnit) it.next();
			InstallableUnitOperand[] operands = new InstallableUnitOperand[] {new InstallableUnitOperand(createResolvedIU(doomed), null)};
			engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		}
		final Sizing sizingPhase = new Sizing(100, "sizing");
		PhaseSet phaseSet = new PhaseSet(new Phase[] {sizingPhase}) {};

		InstallableUnitOperand[] operands = new InstallableUnitOperand[] {new InstallableUnitOperand(null, createOSGiIU())};
		IStatus result = engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		assertTrue(result.isOK());
		assertTrue(sizingPhase.getDiskSize() == 0);
		assertTrue(sizingPhase.getDlSize() == 0);
	}

	public void testPerformInstallOSGiFramework() {
		Map properties = new HashMap();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());

		IProfile profile = createProfile("testPerformInstallOSGiFramework", null, properties);
		for (Iterator it = getInstallableUnits(profile); it.hasNext();) {
			PhaseSet phaseSet = new DefaultPhaseSet();
			IInstallableUnit doomed = (IInstallableUnit) it.next();
			InstallableUnitOperand[] operands = new InstallableUnitOperand[] {new InstallableUnitOperand(createResolvedIU(doomed), null)};
			engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		}
		PhaseSet phaseSet = new DefaultPhaseSet();

		InstallableUnitOperand[] operands = new InstallableUnitOperand[] {new InstallableUnitOperand(null, createOSGiIU())};
		IStatus result = engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		assertTrue(result.isOK());
		Iterator ius = getInstallableUnits(profile);
		assertTrue(ius.hasNext());
	}

	public void testPerformUpdateOSGiFramework() {
		Map properties = new HashMap();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		IProfile profile = createProfile("testPerformUpdateOSGiFramework", null, properties);
		PhaseSet phaseSet = new DefaultPhaseSet();

		IInstallableUnit iu33 = createOSGiIU("3.3");
		IInstallableUnit iu34 = createOSGiIU("3.4");

		InstallableUnitOperand[] installOperands = new InstallableUnitOperand[] {new InstallableUnitOperand(null, iu33)};
		IStatus result = engine.perform(profile, phaseSet, installOperands, null, new NullProgressMonitor());
		assertTrue(result.isOK());
		Iterator ius = profile.query(new InstallableUnitQuery(iu33.getId(), iu33.getVersion()), new Collector(), null).iterator();
		assertTrue(ius.hasNext());

		InstallableUnitOperand[] updateOperands = new InstallableUnitOperand[] {new InstallableUnitOperand(iu33, iu34)};
		result = engine.perform(profile, phaseSet, updateOperands, null, new NullProgressMonitor());
		assertTrue(result.isOK());
		ius = profile.query(new InstallableUnitQuery(iu34.getId(), iu34.getVersion()), new Collector(), null).iterator();
		assertTrue(ius.hasNext());
	}

	public void testPerformUninstallOSGiFramework() {

		Map properties = new HashMap();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());

		IProfile profile = createProfile("testPerformUninstallOSGiFramework", null, properties);
		PhaseSet phaseSet = new DefaultPhaseSet();
		InstallableUnitOperand[] operands = new InstallableUnitOperand[] {new InstallableUnitOperand(createOSGiIU(), null)};
		IStatus result = engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		assertTrue(result.isOK());
		assertEmptyProfile(profile);
	}

	public void testPerformRollback() {

		Map properties = new HashMap();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		IProfile profile = createProfile("testPerformRollback", null, properties);
		PhaseSet phaseSet = new DefaultPhaseSet();

		Iterator ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());

		InstallableUnitOperand[] operands = new InstallableUnitOperand[] {new InstallableUnitOperand(null, createOSGiIU()), new InstallableUnitOperand(null, createBadIU())};
		IStatus result = engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		assertFalse(result.isOK());
		ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());
	}

	public void testPerformRollbackOnError() {

		Map properties = new HashMap();
		properties.put(IProfile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		IProfile profile = createProfile("testPerformRollbackOnError", null, properties);
		PhaseSet phaseSet = new NPEPhaseSet();

		Iterator ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());

		InstallableUnitOperand[] operands = new InstallableUnitOperand[] {new InstallableUnitOperand(null, createOSGiIU())};
		IStatus result = engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		assertFalse(result.isOK());
		ius = getInstallableUnits(profile);
		assertFalse(ius.hasNext());
	}

	public void testOrphanedIUProperty() {
		IProfile profile = createProfile("testOrphanedIUProperty");
		PhaseSet phaseSet = new DefaultPhaseSet();
		IInstallableUnit iu = createIU("someIU");
		Operand[] operands = new InstallableUnitPropertyOperand[] {new InstallableUnitPropertyOperand(iu, "key", null, "value")};
		IStatus result = engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		assertTrue(result.isOK());
		assertFalse(profile.getInstallableUnitProperties(iu).containsKey("key"));

		operands = new Operand[] {new InstallableUnitOperand(null, iu), new InstallableUnitPropertyOperand(iu, "adifferentkey", null, "value")};
		result = engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		assertTrue(result.isOK());
		assertTrue(profile.getInstallableUnitProperties(iu).containsKey("adifferentkey"));
		assertFalse(profile.getInstallableUnitProperties(iu).containsKey("key"));
	}

	private IInstallableUnit createOSGiIU() {
		return createOSGiIU("3.3.1.R33x_v20070828");
	}

	private IInstallableUnit createOSGiIU(String version) {
		InstallableUnitDescription description = new MetadataFactory.InstallableUnitDescription();
		description.setId("org.eclipse.osgi");
		description.setVersion(new Version(version));
		description.setTouchpointType(AbstractProvisioningTest.TOUCHPOINT_OSGI);
		Map touchpointData = new HashMap();
		String manifest = "Manifest-Version: 1.0\r\n" + "Bundle-Activator: org.eclipse.osgi.framework.internal.core.SystemBundl\r\n" + " eActivator\r\n" + "Bundle-RequiredExecutionEnvironment: J2SE-1.4,OSGi/Minimum-1.0\r\n" + "Export-Package: org.eclipse.osgi.event;version=\"1.0\",org.eclipse.osgi.\r\n" + " framework.console;version=\"1.0\",org.eclipse.osgi.framework.eventmgr;v\r\n" + " ersion=\"1.0\",org.eclipse.osgi.framework.log;version=\"1.0\",org.eclipse\r\n" + " .osgi.service.datalocation;version=\"1.0\",org.eclipse.osgi.service.deb\r\n" + " ug;version=\"1.0\",org.eclipse.osgi.service.environment;version=\"1.0\",o\r\n" + " rg.eclipse.osgi.service.localization;version=\"1.0\",org.eclipse.osgi.s\r\n" + " ervice.pluginconversion;version=\"1.0\",org.eclipse.osgi.service.resolv\r\n"
				+ " er;version=\"1.1\",org.eclipse.osgi.service.runnable;version=\"1.0\",org.\r\n" + " eclipse.osgi.service.urlconversion;version=\"1.0\",org.eclipse.osgi.sto\r\n" + " ragemanager;version=\"1.0\",org.eclipse.osgi.util;version=\"1.0\",org.osg\r\n" + " i.framework;version=\"1.3\",org.osgi.service.condpermadmin;version=\"1.0\r\n" + " \",org.osgi.service.packageadmin;version=\"1.2\",org.osgi.service.permis\r\n" + " sionadmin;version=\"1.2\",org.osgi.service.startlevel;version=\"1.0\",org\r\n" + " .osgi.service.url;version=\"1.0\",org.osgi.util.tracker;version=\"1.3.2\"\r\n" + " ,org.eclipse.core.runtime.adaptor;x-friends:=\"org.eclipse.core.runtim\r\n" + " e\",org.eclipse.core.runtime.internal.adaptor;x-internal:=true,org.ecl\r\n"
				+ " ipse.core.runtime.internal.stats;x-friends:=\"org.eclipse.core.runtime\r\n" + " \",org.eclipse.osgi.baseadaptor;x-internal:=true,org.eclipse.osgi.base\r\n" + " adaptor.bundlefile;x-internal:=true,org.eclipse.osgi.baseadaptor.hook\r\n" + " s;x-internal:=true,org.eclipse.osgi.baseadaptor.loader;x-internal:=tr\r\n" + " ue,org.eclipse.osgi.framework.adaptor;x-internal:=true,org.eclipse.os\r\n" + " gi.framework.debug;x-internal:=true,org.eclipse.osgi.framework.intern\r\n" + " al.core;x-internal:=true,org.eclipse.osgi.framework.internal.protocol\r\n" + " ;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.bundle\r\n" + " entry;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.b\r\n"
				+ " undleresource;x-internal:=true,org.eclipse.osgi.framework.internal.pr\r\n" + " otocol.reference;x-internal:=true,org.eclipse.osgi.framework.internal\r\n" + " .reliablefile;x-internal:=true,org.eclipse.osgi.framework.launcher;x-\r\n" + " internal:=true,org.eclipse.osgi.framework.util;x-internal:=true,org.e\r\n" + " clipse.osgi.internal.baseadaptor;x-internal:=true,org.eclipse.osgi.in\r\n" + " ternal.module;x-internal:=true,org.eclipse.osgi.internal.profile;x-in\r\n" + " ternal:=true,org.eclipse.osgi.internal.resolver;x-internal:=true,org.\r\n" + " eclipse.osgi.internal.verifier;x-internal:=true,org.eclipse.osgi.inte\r\n" + " rnal.provisional.verifier;x-friends:=\"org.eclipse.update.core,org.ecl\r\n" + " ipse.ui.workbench\"\r\n" + "Bundle-Version: 3.3.0.v20060925\r\n"
				+ "Eclipse-SystemBundle: true\r\n" + "Bundle-Copyright: %copyright\r\n" + "Bundle-Name: %systemBundle\r\n" + "Bundle-Description: %systemBundle\r\n" + "Bundle-DocUrl: http://www.eclipse.org\r\n" + "Bundle-ManifestVersion: 2\r\n" + "Export-Service: org.osgi.service.packageadmin.PackageAdmin,org.osgi.se\r\n" + " rvice.permissionadmin.PermissionAdmin,org.osgi.service.startlevel.Sta\r\n" + " rtLevel,org.eclipse.osgi.service.debug.DebugOptions\r\n" + "Bundle-Vendor: %eclipse.org\r\n" + "Main-Class: org.eclipse.core.runtime.adaptor.EclipseStarter\r\n" + "Bundle-SymbolicName: org.eclipse.osgi; singleton:=true\r\n" + "Bundle-Localization: systembundle\r\n" + "Eclipse-ExtensibleAPI: true\r\n" + "\r\n" + "";
		touchpointData.put("manifest", manifest);
		//touchpointData.put("install", "installBundle(bundle:${artifact});");
		//touchpointData.put("uninstall", "uninstallBundle(bundle:${artifact});");

		IInstallableUnitFragment[] cus = new IInstallableUnitFragment[1];
		InstallableUnitFragmentDescription desc = new InstallableUnitFragmentDescription();
		desc.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		IInstallableUnitFragment fragment = MetadataFactory.createInstallableUnitFragment(desc);
		cus[0] = fragment;

		//IArtifactKey key = new ArtifactKey("eclipse", "plugin", "org.eclipse.osgi", new Version("3.3.1.R33x_v20070828"));
		//iu.setArtifacts(new IArtifactKey[] {key});

		IInstallableUnit iu = MetadataFactory.createInstallableUnit(description);
		return MetadataFactory.createResolvedInstallableUnit(iu, cus);
	}

	private IInstallableUnit createBadIU() {
		InstallableUnitDescription description = new MetadataFactory.InstallableUnitDescription();
		description.setId("org.eclipse.osgi.bad");
		description.setVersion(new Version("3.3.1.R33x_v20070828"));
		description.setTouchpointType(AbstractProvisioningTest.TOUCHPOINT_OSGI);
		Map touchpointData = new HashMap();
		String manifest = "Manifest-Version: 1.0\r\n" + "Bundle-Activator: org.eclipse.osgi.framework.internal.core.SystemBundl\r\n" + " eActivator\r\n" + "Bundle-RequiredExecutionEnvironment: J2SE-1.4,OSGi/Minimum-1.0\r\n" + "Export-Package: org.eclipse.osgi.event;version=\"1.0\",org.eclipse.osgi.\r\n" + " framework.console;version=\"1.0\",org.eclipse.osgi.framework.eventmgr;v\r\n" + " ersion=\"1.0\",org.eclipse.osgi.framework.log;version=\"1.0\",org.eclipse\r\n" + " .osgi.service.datalocation;version=\"1.0\",org.eclipse.osgi.service.deb\r\n" + " ug;version=\"1.0\",org.eclipse.osgi.service.environment;version=\"1.0\",o\r\n" + " rg.eclipse.osgi.service.localization;version=\"1.0\",org.eclipse.osgi.s\r\n" + " ervice.pluginconversion;version=\"1.0\",org.eclipse.osgi.service.resolv\r\n"
				+ " er;version=\"1.1\",org.eclipse.osgi.service.runnable;version=\"1.0\",org.\r\n" + " eclipse.osgi.service.urlconversion;version=\"1.0\",org.eclipse.osgi.sto\r\n" + " ragemanager;version=\"1.0\",org.eclipse.osgi.util;version=\"1.0\",org.osg\r\n" + " i.framework;version=\"1.3\",org.osgi.service.condpermadmin;version=\"1.0\r\n" + " \",org.osgi.service.packageadmin;version=\"1.2\",org.osgi.service.permis\r\n" + " sionadmin;version=\"1.2\",org.osgi.service.startlevel;version=\"1.0\",org\r\n" + " .osgi.service.url;version=\"1.0\",org.osgi.util.tracker;version=\"1.3.2\"\r\n" + " ,org.eclipse.core.runtime.adaptor;x-friends:=\"org.eclipse.core.runtim\r\n" + " e\",org.eclipse.core.runtime.internal.adaptor;x-internal:=true,org.ecl\r\n"
				+ " ipse.core.runtime.internal.stats;x-friends:=\"org.eclipse.core.runtime\r\n" + " \",org.eclipse.osgi.baseadaptor;x-internal:=true,org.eclipse.osgi.base\r\n" + " adaptor.bundlefile;x-internal:=true,org.eclipse.osgi.baseadaptor.hook\r\n" + " s;x-internal:=true,org.eclipse.osgi.baseadaptor.loader;x-internal:=tr\r\n" + " ue,org.eclipse.osgi.framework.adaptor;x-internal:=true,org.eclipse.os\r\n" + " gi.framework.debug;x-internal:=true,org.eclipse.osgi.framework.intern\r\n" + " al.core;x-internal:=true,org.eclipse.osgi.framework.internal.protocol\r\n" + " ;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.bundle\r\n" + " entry;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.b\r\n"
				+ " undleresource;x-internal:=true,org.eclipse.osgi.framework.internal.pr\r\n" + " otocol.reference;x-internal:=true,org.eclipse.osgi.framework.internal\r\n" + " .reliablefile;x-internal:=true,org.eclipse.osgi.framework.launcher;x-\r\n" + " internal:=true,org.eclipse.osgi.framework.util;x-internal:=true,org.e\r\n" + " clipse.osgi.internal.baseadaptor;x-internal:=true,org.eclipse.osgi.in\r\n" + " ternal.module;x-internal:=true,org.eclipse.osgi.internal.profile;x-in\r\n" + " ternal:=true,org.eclipse.osgi.internal.resolver;x-internal:=true,org.\r\n" + " eclipse.osgi.internal.verifier;x-internal:=true,org.eclipse.osgi.inte\r\n" + " rnal.provisional.verifier;x-friends:=\"org.eclipse.update.core,org.ecl\r\n" + " ipse.ui.workbench\"\r\n" + "Bundle-Version: 3.3.0.v20060925\r\n"
				+ "Eclipse-SystemBundle: true\r\n" + "Bundle-Copyright: %copyright\r\n" + "Bundle-Name: %systemBundle\r\n" + "Bundle-Description: %systemBundle\r\n" + "Bundle-DocUrl: http://www.eclipse.org\r\n" + "Bundle-ManifestVersion: 2\r\n" + "Export-Service: org.osgi.service.packageadmin.PackageAdmin,org.osgi.se\r\n" + " rvice.permissionadmin.PermissionAdmin,org.osgi.service.startlevel.Sta\r\n" + " rtLevel,org.eclipse.osgi.service.debug.DebugOptions\r\n" + "Bundle-Vendor: %eclipse.org\r\n" + "Main-Class: org.eclipse.core.runtime.adaptor.EclipseStarter\r\n" + "Bundle-SymbolicName: org.eclipse.osgi; singleton:=true\r\n" + "Bundle-Localization: systembundle\r\n" + "Eclipse-ExtensibleAPI: true\r\n" + "\r\n" + "";
		touchpointData.put("manifest", manifest);
		touchpointData.put("install", "BAD");

		IInstallableUnitFragment[] cus = new IInstallableUnitFragment[1];
		InstallableUnitFragmentDescription desc = new InstallableUnitFragmentDescription();
		desc.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		cus[0] = MetadataFactory.createInstallableUnitFragment(desc);

		//IArtifactKey key = new ArtifactKey("eclipse", "plugin", "org.eclipse.osgi", new Version("3.3.1.R33x_v20070828"));
		//iu.setArtifacts(new IArtifactKey[] {key});

		IInstallableUnit iu = MetadataFactory.createInstallableUnit(description);
		return MetadataFactory.createResolvedInstallableUnit(iu, cus);
	}

	public void testIncompatibleProfile() {

		IProfile profile = new IProfile() {
			public Collector available(Query query, Collector collector, IProgressMonitor monitor) {
				return null;
			}

			public Map getInstallableUnitProperties(IInstallableUnit iu) {
				return null;
			}

			public String getInstallableUnitProperty(IInstallableUnit iu, String key) {
				return null;
			}

			public Map getLocalProperties() {
				return null;
			}

			public String getLocalProperty(String key) {
				return null;
			}

			public IProfile getParentProfile() {
				return null;
			}

			public String getProfileId() {
				return null;
			}

			public Map getProperties() {
				return null;
			}

			public String getProperty(String key) {
				return null;
			}

			public String[] getSubProfileIds() {
				return null;
			}

			public long getTimestamp() {
				return 0;
			}

			public boolean hasSubProfiles() {
				return false;
			}

			public boolean isRootProfile() {
				return false;
			}

			public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
				return null;
			}
		};
		PhaseSet phaseSet = new DefaultPhaseSet();
		InstallableUnitOperand[] operands = new InstallableUnitOperand[] {};
		try {
			engine.perform(profile, phaseSet, operands, null, new NullProgressMonitor());
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

}
