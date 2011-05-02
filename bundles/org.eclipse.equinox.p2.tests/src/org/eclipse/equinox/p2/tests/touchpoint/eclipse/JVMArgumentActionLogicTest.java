/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class JVMArgumentActionLogicTest extends AbstractProvisioningTest {

	private static File tempDir;
	private Map parameters;
	private LauncherData launcherData;

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
		AbstractProvisioningTest.delete(tempDir);
		super.tearDown();
	}

	public void testStandardUse() {
		AddJVMArgumentAction addAction = new AddJVMArgumentAction();
		RemoveJVMArgumentAction rmAction = new RemoveJVMArgumentAction();

		String maxJvmArg = "-Xmx512M";
		String minJvmArg = "-Xmx256M";
		String diffJvmArg = "-Xms50M";

		// Add a value then undo
		parameters.put(ActionConstants.PARM_JVM_ARG, maxJvmArg);
		addAction.execute(parameters);
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(maxJvmArg));
		addAction.undo(parameters);
		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(maxJvmArg));

		// Add value
		parameters.put(ActionConstants.PARM_JVM_ARG, minJvmArg);
		addAction = new AddJVMArgumentAction();
		addAction.execute(parameters);
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(minJvmArg));

		// Add a different type of argument
		parameters.put(ActionConstants.PARM_JVM_ARG, diffJvmArg);
		addAction.execute(parameters);
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(minJvmArg));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(diffJvmArg));
		rmAction.execute(parameters);

		// Add a larger value
		parameters.put(ActionConstants.PARM_JVM_ARG, maxJvmArg);
		addAction.execute(parameters);
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(maxJvmArg));
		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(minJvmArg));

		// Remove large value
		rmAction.execute(parameters);
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(minJvmArg));
		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(maxJvmArg));

		// Remove first value
		parameters.put(ActionConstants.PARM_JVM_ARG, minJvmArg);
		rmAction.execute(parameters);
		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(minJvmArg));
	}

	public void testPrefixEvaluation() {
		String gigabyteArg = "-XX:MaxPermSize=1G";
		String megabyteArg = "-XX:MaxPermSize=1M";
		String kilobyteArg = "-XX:MaxPermSize=1K";
		String byteArg = "-XX:MaxPermSize=1";
		AddJVMArgumentAction addAction = new AddJVMArgumentAction();
		RemoveJVMArgumentAction rmAction = new RemoveJVMArgumentAction();

		// Standard prefix evaluation
		parameters.put(ActionConstants.PARM_JVM_ARG, byteArg);
		addAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(byteArg));

		parameters.put(ActionConstants.PARM_JVM_ARG, kilobyteArg);
		addAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(kilobyteArg));

		parameters.put(ActionConstants.PARM_JVM_ARG, megabyteArg);
		addAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(megabyteArg));

		parameters.put(ActionConstants.PARM_JVM_ARG, gigabyteArg);
		addAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(gigabyteArg));

		// Remove values
		rmAction.execute(Collections.unmodifiableMap(parameters));
		parameters.put(ActionConstants.PARM_JVM_ARG, megabyteArg);
		rmAction.execute(Collections.unmodifiableMap(parameters));
		parameters.put(ActionConstants.PARM_JVM_ARG, kilobyteArg);
		rmAction.execute(Collections.unmodifiableMap(parameters));
		parameters.put(ActionConstants.PARM_JVM_ARG, byteArg);
		rmAction.execute(Collections.unmodifiableMap(parameters));

		// Non-standard prefix evaluation
		gigabyteArg = "-Xmx1G";
		megabyteArg = "-Xmx2048M";

		parameters.put(ActionConstants.PARM_JVM_ARG, gigabyteArg);
		addAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(gigabyteArg));

		parameters.put(ActionConstants.PARM_JVM_ARG, megabyteArg);
		addAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(megabyteArg));

		// Clear state
		parameters.put(ActionConstants.PARM_JVM_ARG, megabyteArg);
		rmAction.execute(Collections.unmodifiableMap(parameters));
		parameters.put(ActionConstants.PARM_JVM_ARG, gigabyteArg);
		rmAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).size() == 0);
	}

	public void testInvalidValues() {
		AddJVMArgumentAction action = new AddJVMArgumentAction();

		String invalid = "-Xms25F";
		String valid = "-Xms256M";

		parameters.put(ActionConstants.PARM_JVM_ARG, invalid);

		IStatus result = action.execute(Collections.unmodifiableMap(parameters));
		if (!result.matches(IStatus.ERROR) && !(result.getException() instanceof IllegalArgumentException))
			fail("Invalid Action value not caught!");

		// User has injected an invalid value
		launcherData.addJvmArg(invalid);
		parameters.put(ActionConstants.PARM_JVM_ARG, valid);

		result = action.execute(Collections.unmodifiableMap(parameters));
		if (!result.matches(IStatus.ERROR) && !(result.getException() instanceof IllegalArgumentException))
			fail("Invalid injected value not caught!");

		launcherData.removeJvmArg(invalid);
	}

	public void testUserInjectsInitialValue() {
		String userValue = "-Xmx400M";
		String largeValue = "-Xmx512M";
		AddJVMArgumentAction addAction = new AddJVMArgumentAction();
		RemoveJVMArgumentAction rmAction = new RemoveJVMArgumentAction();

		// Simulate a user injected value
		launcherData.addJvmArg(userValue);

		// Add a larger value 
		parameters.put(ActionConstants.PARM_JVM_ARG, largeValue);
		addAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(largeValue));
		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(userValue));

		// Remove added value
		rmAction.execute(Collections.unmodifiableMap(parameters));
		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(largeValue));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(userValue));

		// Clear state
		launcherData.removeJvmArg(userValue);
		parameters.put(ActionConstants.PARM_JVM_ARG, "-Xmx300M");
		addAction.execute(Collections.unmodifiableMap(parameters));
		rmAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).size() == 0);
	}

	public void testUserInjectsLargerValue() {
		AddJVMArgumentAction addAction = new AddJVMArgumentAction();
		RemoveJVMArgumentAction rmAction = new RemoveJVMArgumentAction();
		String userValue = "-Xmx400M";
		String initialValue = "-Xmx256M";
		String smallValue = "-Xmx100M";

		// Initial value
		parameters.put(ActionConstants.PARM_JVM_ARG, initialValue);
		addAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(initialValue));

		// Inject value
		launcherData.removeJvmArg(initialValue);
		launcherData.addJvmArg(userValue);

		// Smaller value added
		parameters.put(ActionConstants.PARM_JVM_ARG, smallValue);
		addAction.execute(Collections.unmodifiableMap(parameters));
		rmAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(userValue));
		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(initialValue));
		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(smallValue));

		// Value equal to User's added & removed
		parameters.put(ActionConstants.PARM_JVM_ARG, userValue);
		addAction.execute(Collections.unmodifiableMap(parameters));
		rmAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(userValue));
		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(initialValue));
		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(smallValue));

		// Clear state
		launcherData.removeJvmArg(userValue);
		parameters.put(ActionConstants.PARM_JVM_ARG, initialValue);
		rmAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).size() == 0);
	}

	public void testUserInjectsSmallerValue() {
		AddJVMArgumentAction addAction = new AddJVMArgumentAction();
		RemoveJVMArgumentAction rmAction = new RemoveJVMArgumentAction();
		String userValue = "-Xmx100M";
		String initialValue = "-Xmx256M";
		String largeValue = "-Xmx512M";

		// Initial value
		parameters.put(ActionConstants.PARM_JVM_ARG, initialValue);
		addAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(initialValue));

		// Inject value
		launcherData.removeJvmArg(initialValue);
		launcherData.addJvmArg(userValue);

		// Add new value
		parameters.put(ActionConstants.PARM_JVM_ARG, largeValue);
		addAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(largeValue));
		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(initialValue));
		assertFalse(Arrays.asList(launcherData.getJvmArgs()).contains(userValue));

		// Remove values
		rmAction.execute(Collections.unmodifiableMap(parameters));
		parameters.put(ActionConstants.PARM_JVM_ARG, initialValue);
		rmAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).contains(userValue));

		// Clear state
		launcherData.removeJvmArg(userValue);
		parameters.put(ActionConstants.PARM_JVM_ARG, initialValue);
		rmAction.execute(Collections.unmodifiableMap(parameters));
		assertTrue(Arrays.asList(launcherData.getJvmArgs()).size() == 0);
	}
}
