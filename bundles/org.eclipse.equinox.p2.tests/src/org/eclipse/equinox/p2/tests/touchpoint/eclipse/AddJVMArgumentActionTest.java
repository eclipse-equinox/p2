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
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.AddJVMArgumentAction;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class AddJVMArgumentActionTest extends AbstractProvisioningTest {

	private static File tempDir;
	private LauncherData launcherData;
	private Map parameters;

	public AddJVMArgumentActionTest(String name) {
		super(name);
	}

	public AddJVMArgumentActionTest() {
		super("");
	}

	public void setUp() throws Exception {
		super.setUp();
		tempDir = new File(System.getProperty("java.io.tmpdir"), "JVMArgs");
		tempDir.mkdirs();

		parameters = new HashMap();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		Properties profileProperties = new Properties();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, getTempFolder().toString());
		IProfile profile = createProfile("test", profileProperties);
		InstallableUnitOperand operand = new InstallableUnitOperand(null, createIU("test"));
		touchpoint.initializePhase(null, profile, "test", parameters);
		parameters.put("iu", operand.second());
		touchpoint.initializeOperand(profile, parameters);
		parameters.put(ActionConstants.PARM_PROFILE_DATA_DIRECTORY, tempDir);

		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);
		launcherData = manipulator.getLauncherData();
	}

	public void tearDown() throws Exception {
		delete(tempDir);
		super.tearDown();
	}

	public void testExecuteUndo() {
		String jvmArg = "-Dtest=true";

		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(jvmArg));
		parameters.put(ActionConstants.PARM_JVM_ARG, jvmArg);

		AddJVMArgumentAction action = new AddJVMArgumentAction();
		action.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(jvmArg));
		action.undo(Collections.unmodifiableMap(parameters));
		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(jvmArg));

		// Test using byte argument
		String byteArg = "-Xmx256M";
		parameters.put(ActionConstants.PARM_JVM_ARG, byteArg);
		action.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(byteArg));
		action.undo(Collections.unmodifiableMap(parameters));
		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(byteArg));
	}
}
