/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
import junit.framework.TestCase;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * Simple test of the engine API.
 * 
 * Note:
 * Currently you MUST have previously generated metadata from a 3.3.1 install.
 * There are ordering dependencies for the tests temporarily 
 */
public class EngineTest extends TestCase {
	private ServiceReference engineRef;
	private Engine engine;
	private File testProvisioning;

	public EngineTest(String name) {
		super(name);
		testProvisioning = new File(System.getProperty("java.io.tmpdir"), "testProvisioining");
		deleteDirectory(testProvisioning);
		testProvisioning.mkdir();
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
		engineRef = TestActivator.getContext().getServiceReference(Engine.class.getName());
		engine = (Engine) TestActivator.getContext().getService(engineRef);
	}

	protected void tearDown() throws Exception {
		engine = null;
		TestActivator.getContext().ungetService(engineRef);
	}

	public void testNullProfile() {

		Profile profile = null;
		PhaseSet phaseSet = new DefaultPhaseSet();
		Operand[] operands = new Operand[] {};
		try {
			engine.perform(profile, phaseSet, operands, new NullProgressMonitor());
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testNullPhaseSet() {

		Profile profile = new Profile("test");
		PhaseSet phaseSet = null;
		Operand[] operands = new Operand[] {};
		try {
			engine.perform(profile, phaseSet, operands, new NullProgressMonitor());
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testNullOperands() {

		Profile profile = new Profile("test");
		PhaseSet phaseSet = new DefaultPhaseSet();
		Operand[] operands = null;
		try {
			engine.perform(profile, phaseSet, operands, new NullProgressMonitor());
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testEmptyOperands() {

		Profile profile = new Profile("test");
		PhaseSet phaseSet = new DefaultPhaseSet();
		Operand[] operands = new Operand[] {};
		IStatus result = engine.perform(profile, phaseSet, operands, new NullProgressMonitor());
		assertTrue(result.isOK());
	}

	public void testEmptyPhaseSet() {

		Profile profile = new Profile("test");
		PhaseSet phaseSet = new PhaseSet(new Phase[] {}) {
			// empty PhaseSet
		};
		Operand op = new Operand(new ResolvedInstallableUnit(new InstallableUnit()), null);
		Operand[] operands = new Operand[] {op};
		IStatus result = engine.perform(profile, phaseSet, operands, new NullProgressMonitor());
		assertTrue(result.isOK());
	}

	public void testPerformSingleNullOperand() {

		Profile profile = new Profile("test");
		PhaseSet phaseSet = new DefaultPhaseSet();
		Operand[] operands = new Operand[] {new Operand(null, null)};
		IStatus result = engine.perform(profile, phaseSet, operands, new NullProgressMonitor());
		assertTrue(result.isOK());
	}

	public void testPerformInstallOSGiFramework() {

		Profile profile = new Profile("test");
		profile.setValue(Profile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		for (Iterator it = profile.getInstallableUnits(); it.hasNext();) {
			PhaseSet phaseSet = new DefaultPhaseSet();
			InstallableUnit doomed = (InstallableUnit) it.next();
			Operand[] operands = new Operand[] {new Operand(doomed.getResolved(), null)};
			engine.perform(profile, phaseSet, operands, new NullProgressMonitor());
		}
		PhaseSet phaseSet = new DefaultPhaseSet();

		Operand[] operands = new Operand[] {new Operand(null, createOSGiIU())};
		IStatus result = engine.perform(profile, phaseSet, operands, new NullProgressMonitor());
		assertTrue(result.isOK());
		Iterator ius = profile.getInstallableUnits();
		assertTrue(ius.hasNext());
	}

	public void testPerformUpdateOSGiFramework() {

		Profile profile = new Profile("test");
		profile.setValue(Profile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		PhaseSet phaseSet = new DefaultPhaseSet();
		Operand[] operands = new Operand[] {new Operand(createOSGiIU(), createOSGiIU())};
		IStatus result = engine.perform(profile, phaseSet, operands, new NullProgressMonitor());
		assertTrue(result.isOK());
		Iterator ius = profile.getInstallableUnits();
		assertTrue(ius.hasNext());
	}

	public void testPerformUninstallOSGiFramework() {

		Profile profile = new Profile("test");
		profile.setValue(Profile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		PhaseSet phaseSet = new DefaultPhaseSet();
		Operand[] operands = new Operand[] {new Operand(createOSGiIU(), null)};
		IStatus result = engine.perform(profile, phaseSet, operands, new NullProgressMonitor());
		assertTrue(result.isOK());
		Iterator ius = profile.getInstallableUnits();
		assertFalse(ius.hasNext());
	}

	public void testPerformRollback() {

		Profile profile = new Profile("test");
		profile.setValue(Profile.PROP_INSTALL_FOLDER, testProvisioning.getAbsolutePath());
		PhaseSet phaseSet = new DefaultPhaseSet();

		Operand[] operands = new Operand[] {new Operand(null, createOSGiIU()), new Operand(null, createBadIU())};
		IStatus result = engine.perform(profile, phaseSet, operands, new NullProgressMonitor());
		assertFalse(result.isOK());
		Iterator ius = profile.getInstallableUnits();
		assertFalse(ius.hasNext());
	}

	private IResolvedInstallableUnit createOSGiIU() {
		InstallableUnit iu = new InstallableUnit();
		iu.setId("org.eclipse.osgi");
		iu.setVersion(new Version("3.3.1.R33x_v20070828"));
		iu.setTouchpointType(new TouchpointType("eclipse", new Version("1.0.0")));
		Map touchpointData = new HashMap();
		String manifest = "Manifest-Version: 1.0\r\n" + "Bundle-Activator: org.eclipse.osgi.framework.internal.core.SystemBundl\r\n" + " eActivator\r\n" + "Bundle-RequiredExecutionEnvironment: J2SE-1.4,OSGi/Minimum-1.0\r\n" + "Export-Package: org.eclipse.osgi.event;version=\"1.0\",org.eclipse.osgi.\r\n" + " framework.console;version=\"1.0\",org.eclipse.osgi.framework.eventmgr;v\r\n" + " ersion=\"1.0\",org.eclipse.osgi.framework.log;version=\"1.0\",org.eclipse\r\n" + " .osgi.service.datalocation;version=\"1.0\",org.eclipse.osgi.service.deb\r\n" + " ug;version=\"1.0\",org.eclipse.osgi.service.environment;version=\"1.0\",o\r\n" + " rg.eclipse.osgi.service.localization;version=\"1.0\",org.eclipse.osgi.s\r\n" + " ervice.pluginconversion;version=\"1.0\",org.eclipse.osgi.service.resolv\r\n"
				+ " er;version=\"1.1\",org.eclipse.osgi.service.runnable;version=\"1.0\",org.\r\n" + " eclipse.osgi.service.urlconversion;version=\"1.0\",org.eclipse.osgi.sto\r\n" + " ragemanager;version=\"1.0\",org.eclipse.osgi.util;version=\"1.0\",org.osg\r\n" + " i.framework;version=\"1.3\",org.osgi.service.condpermadmin;version=\"1.0\r\n" + " \",org.osgi.service.packageadmin;version=\"1.2\",org.osgi.service.permis\r\n" + " sionadmin;version=\"1.2\",org.osgi.service.startlevel;version=\"1.0\",org\r\n" + " .osgi.service.url;version=\"1.0\",org.osgi.util.tracker;version=\"1.3.2\"\r\n" + " ,org.eclipse.core.runtime.adaptor;x-friends:=\"org.eclipse.core.runtim\r\n" + " e\",org.eclipse.core.runtime.internal.adaptor;x-internal:=true,org.ecl\r\n"
				+ " ipse.core.runtime.internal.stats;x-friends:=\"org.eclipse.core.runtime\r\n" + " \",org.eclipse.osgi.baseadaptor;x-internal:=true,org.eclipse.osgi.base\r\n" + " adaptor.bundlefile;x-internal:=true,org.eclipse.osgi.baseadaptor.hook\r\n" + " s;x-internal:=true,org.eclipse.osgi.baseadaptor.loader;x-internal:=tr\r\n" + " ue,org.eclipse.osgi.framework.adaptor;x-internal:=true,org.eclipse.os\r\n" + " gi.framework.debug;x-internal:=true,org.eclipse.osgi.framework.intern\r\n" + " al.core;x-internal:=true,org.eclipse.osgi.framework.internal.protocol\r\n" + " ;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.bundle\r\n" + " entry;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.b\r\n"
				+ " undleresource;x-internal:=true,org.eclipse.osgi.framework.internal.pr\r\n" + " otocol.reference;x-internal:=true,org.eclipse.osgi.framework.internal\r\n" + " .reliablefile;x-internal:=true,org.eclipse.osgi.framework.launcher;x-\r\n" + " internal:=true,org.eclipse.osgi.framework.util;x-internal:=true,org.e\r\n" + " clipse.osgi.internal.baseadaptor;x-internal:=true,org.eclipse.osgi.in\r\n" + " ternal.module;x-internal:=true,org.eclipse.osgi.internal.profile;x-in\r\n" + " ternal:=true,org.eclipse.osgi.internal.resolver;x-internal:=true,org.\r\n" + " eclipse.osgi.internal.verifier;x-internal:=true,org.eclipse.osgi.inte\r\n" + " rnal.provisional.verifier;x-friends:=\"org.eclipse.update.core,org.ecl\r\n" + " ipse.ui.workbench\"\r\n" + "Bundle-Version: 3.3.0.v20060925\r\n"
				+ "Eclipse-SystemBundle: true\r\n" + "Bundle-Copyright: %copyright\r\n" + "Bundle-Name: %systemBundle\r\n" + "Bundle-Description: %systemBundle\r\n" + "Bundle-DocUrl: http://www.eclipse.org\r\n" + "Bundle-ManifestVersion: 2\r\n" + "Export-Service: org.osgi.service.packageadmin.PackageAdmin,org.osgi.se\r\n" + " rvice.permissionadmin.PermissionAdmin,org.osgi.service.startlevel.Sta\r\n" + " rtLevel,org.eclipse.osgi.service.debug.DebugOptions\r\n" + "Bundle-Vendor: %eclipse.org\r\n" + "Main-Class: org.eclipse.core.runtime.adaptor.EclipseStarter\r\n" + "Bundle-SymbolicName: org.eclipse.osgi; singleton:=true\r\n" + "Bundle-Localization: systembundle\r\n" + "Eclipse-ExtensibleAPI: true\r\n" + "\r\n" + "";
		touchpointData.put("manifest", manifest);
		//touchpointData.put("install", "installBundle(bundle:${artifact});");
		//touchpointData.put("uninstall", "uninstallBundle(bundle:${artifact});");

		IResolvedInstallableUnit[] cus = new IResolvedInstallableUnit[1];
		InstallableUnitFragment tmp = new InstallableUnitFragment();
		tmp.setImmutableTouchpointData(new TouchpointData(touchpointData));
		cus[0] = tmp.getResolved();

		//IArtifactKey key = new ArtifactKey("eclipse", "plugin", "org.eclipse.osgi", new Version("3.3.1.R33x_v20070828"));
		//iu.setArtifacts(new IArtifactKey[] {key});

		ResolvedInstallableUnit result = (ResolvedInstallableUnit) iu.getResolved();
		result.setFragments(cus);

		return result;
	}

	private IResolvedInstallableUnit createBadIU() {
		InstallableUnit iu = new InstallableUnit();
		iu.setId("org.eclipse.osgi.bad");
		iu.setVersion(new Version("3.3.1.R33x_v20070828"));
		iu.setTouchpointType(new TouchpointType("eclipse", new Version("1.0.0")));
		Map touchpointData = new HashMap();
		String manifest = "Manifest-Version: 1.0\r\n" + "Bundle-Activator: org.eclipse.osgi.framework.internal.core.SystemBundl\r\n" + " eActivator\r\n" + "Bundle-RequiredExecutionEnvironment: J2SE-1.4,OSGi/Minimum-1.0\r\n" + "Export-Package: org.eclipse.osgi.event;version=\"1.0\",org.eclipse.osgi.\r\n" + " framework.console;version=\"1.0\",org.eclipse.osgi.framework.eventmgr;v\r\n" + " ersion=\"1.0\",org.eclipse.osgi.framework.log;version=\"1.0\",org.eclipse\r\n" + " .osgi.service.datalocation;version=\"1.0\",org.eclipse.osgi.service.deb\r\n" + " ug;version=\"1.0\",org.eclipse.osgi.service.environment;version=\"1.0\",o\r\n" + " rg.eclipse.osgi.service.localization;version=\"1.0\",org.eclipse.osgi.s\r\n" + " ervice.pluginconversion;version=\"1.0\",org.eclipse.osgi.service.resolv\r\n"
				+ " er;version=\"1.1\",org.eclipse.osgi.service.runnable;version=\"1.0\",org.\r\n" + " eclipse.osgi.service.urlconversion;version=\"1.0\",org.eclipse.osgi.sto\r\n" + " ragemanager;version=\"1.0\",org.eclipse.osgi.util;version=\"1.0\",org.osg\r\n" + " i.framework;version=\"1.3\",org.osgi.service.condpermadmin;version=\"1.0\r\n" + " \",org.osgi.service.packageadmin;version=\"1.2\",org.osgi.service.permis\r\n" + " sionadmin;version=\"1.2\",org.osgi.service.startlevel;version=\"1.0\",org\r\n" + " .osgi.service.url;version=\"1.0\",org.osgi.util.tracker;version=\"1.3.2\"\r\n" + " ,org.eclipse.core.runtime.adaptor;x-friends:=\"org.eclipse.core.runtim\r\n" + " e\",org.eclipse.core.runtime.internal.adaptor;x-internal:=true,org.ecl\r\n"
				+ " ipse.core.runtime.internal.stats;x-friends:=\"org.eclipse.core.runtime\r\n" + " \",org.eclipse.osgi.baseadaptor;x-internal:=true,org.eclipse.osgi.base\r\n" + " adaptor.bundlefile;x-internal:=true,org.eclipse.osgi.baseadaptor.hook\r\n" + " s;x-internal:=true,org.eclipse.osgi.baseadaptor.loader;x-internal:=tr\r\n" + " ue,org.eclipse.osgi.framework.adaptor;x-internal:=true,org.eclipse.os\r\n" + " gi.framework.debug;x-internal:=true,org.eclipse.osgi.framework.intern\r\n" + " al.core;x-internal:=true,org.eclipse.osgi.framework.internal.protocol\r\n" + " ;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.bundle\r\n" + " entry;x-internal:=true,org.eclipse.osgi.framework.internal.protocol.b\r\n"
				+ " undleresource;x-internal:=true,org.eclipse.osgi.framework.internal.pr\r\n" + " otocol.reference;x-internal:=true,org.eclipse.osgi.framework.internal\r\n" + " .reliablefile;x-internal:=true,org.eclipse.osgi.framework.launcher;x-\r\n" + " internal:=true,org.eclipse.osgi.framework.util;x-internal:=true,org.e\r\n" + " clipse.osgi.internal.baseadaptor;x-internal:=true,org.eclipse.osgi.in\r\n" + " ternal.module;x-internal:=true,org.eclipse.osgi.internal.profile;x-in\r\n" + " ternal:=true,org.eclipse.osgi.internal.resolver;x-internal:=true,org.\r\n" + " eclipse.osgi.internal.verifier;x-internal:=true,org.eclipse.osgi.inte\r\n" + " rnal.provisional.verifier;x-friends:=\"org.eclipse.update.core,org.ecl\r\n" + " ipse.ui.workbench\"\r\n" + "Bundle-Version: 3.3.0.v20060925\r\n"
				+ "Eclipse-SystemBundle: true\r\n" + "Bundle-Copyright: %copyright\r\n" + "Bundle-Name: %systemBundle\r\n" + "Bundle-Description: %systemBundle\r\n" + "Bundle-DocUrl: http://www.eclipse.org\r\n" + "Bundle-ManifestVersion: 2\r\n" + "Export-Service: org.osgi.service.packageadmin.PackageAdmin,org.osgi.se\r\n" + " rvice.permissionadmin.PermissionAdmin,org.osgi.service.startlevel.Sta\r\n" + " rtLevel,org.eclipse.osgi.service.debug.DebugOptions\r\n" + "Bundle-Vendor: %eclipse.org\r\n" + "Main-Class: org.eclipse.core.runtime.adaptor.EclipseStarter\r\n" + "Bundle-SymbolicName: org.eclipse.osgi; singleton:=true\r\n" + "Bundle-Localization: systembundle\r\n" + "Eclipse-ExtensibleAPI: true\r\n" + "\r\n" + "";
		touchpointData.put("manifest", manifest);
		touchpointData.put("install", "BAD");

		IResolvedInstallableUnit[] cus = new IResolvedInstallableUnit[1];
		InstallableUnitFragment tmp = new InstallableUnitFragment();
		tmp.setImmutableTouchpointData(new TouchpointData(touchpointData));
		cus[0] = tmp.getResolved();

		//IArtifactKey key = new ArtifactKey("eclipse", "plugin", "org.eclipse.osgi", new Version("3.3.1.R33x_v20070828"));
		//iu.setArtifacts(new IArtifactKey[] {key});

		ResolvedInstallableUnit result = (ResolvedInstallableUnit) iu.getResolved();
		result.setFragments(cus);

		return result;
	}

}
