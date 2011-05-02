/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.SetLauncherNameAction;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SetLauncherNameActionTest extends AbstractProvisioningTest {

	public SetLauncherNameActionTest(String name) {
		super(name);
	}

	public SetLauncherNameActionTest() {
		super("");
	}

	public void testExecuteUndo() {
		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		Properties profileProperties = new Properties();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, getTempFolder().toString());
		IProfile profile = createProfile("test", profileProperties);
		InstallableUnitOperand operand = new InstallableUnitOperand(null, createIU("test"));
		touchpoint.initializePhase(null, profile, "test", parameters);
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		parameters.put("iu", operand.second());
		touchpoint.initializeOperand(profile, parameters);
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);

		String launcherName = "test";
		assertNotSame(launcherName, profile.getProperty(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME));
		parameters.put(ActionConstants.PARM_LAUNCHERNAME, launcherName);
		parameters = Collections.unmodifiableMap(parameters);

		SetLauncherNameAction action = new SetLauncherNameAction();
		action.execute(parameters);
		assertEquals(launcherName, profile.getProperty(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME));
		action.undo(parameters);
		assertNotSame(launcherName, profile.getProperty(EclipseTouchpoint.PROFILE_PROP_LAUNCHER_NAME));
	}

	public void testEmptyName() {
		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();

		File tempFolder = getTempFolder();
		Properties profileProperties = new Properties();
		profileProperties.put(IProfile.PROP_INSTALL_FOLDER, tempFolder.toString());
		profileProperties.put(IProfile.PROP_ENVIRONMENTS, "osgi.ws=cocoa,osgi.os=macosx,osgi.arch=x86");
		IProfile profile = createProfile("launcherNameProfile", profileProperties);

		InstallableUnitOperand operand = new InstallableUnitOperand(null, createIU("test"));
		touchpoint.initializePhase(null, profile, "test", parameters);
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		parameters.put("iu", operand.second());
		touchpoint.initializeOperand(profile, parameters);

		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);

		parameters.put(ActionConstants.PARM_LAUNCHERNAME, "");
		parameters = Collections.unmodifiableMap(parameters);

		SetLauncherNameAction action = new SetLauncherNameAction();
		action.execute(parameters);
	}

	public void testChangeName() throws Exception {
		File tempFolder = getTempFolder();

		Properties profileProperties = new Properties();
		profileProperties.put(IProfile.PROP_INSTALL_FOLDER, tempFolder.toString());
		profileProperties.put(IProfile.PROP_ENVIRONMENTS, "osgi.ws=win32,osgi.os=win32,osgi.arch=x86");
		IProfile profile = createProfile("changeNameProfile", profileProperties);

		//profile will start using "eclipse" by default, give it some content and see if it 
		//survives a name change.
		File eclipseIni = new File(tempFolder, "eclipse.ini");
		StringBuffer ini = new StringBuffer();
		ini.append("-startup\n");
		ini.append("plugins/org.eclipse.equinox.launcher_1.2.4.v1234.jar\n");
		writeBuffer(eclipseIni, ini);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		InstallableUnitOperand operand = new InstallableUnitOperand(null, createIU("test"));
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		parameters.put("iu", operand.second());
		touchpoint.initializeOperand(profile, parameters);

		parameters.put(ActionConstants.PARM_LAUNCHERNAME, "foo");
		parameters = Collections.unmodifiableMap(parameters);

		SetLauncherNameAction action = new SetLauncherNameAction();
		action.execute(parameters);

		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		File bundle = new File(tempFolder, "plugins/aBundle_1.0.0.jar");
		bundle.getParentFile().mkdirs();
		copy("1.0", getTestData("1.1", "/testData/testRepos/simple.1/plugins/aBundle_1.0.0.jar"), bundle);
		manipulator.getConfigData().addBundle(new BundleInfo(bundle.toURI()));
		manipulator.save(false);

		assertLogContainsLines(new File(tempFolder, "foo.ini"), new String[] {"-startup", "plugins/org.eclipse.equinox.launcher_1.2.4.v1234.jar"});
		assertFalse(eclipseIni.exists());
	}
}