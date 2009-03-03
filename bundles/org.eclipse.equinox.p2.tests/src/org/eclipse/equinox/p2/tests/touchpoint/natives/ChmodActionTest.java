/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.natives;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.p2.touchpoint.natives.NativeTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.ChmodAction;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.InstallableUnitPhase;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ChmodActionTest extends AbstractProvisioningTest {

	public ChmodActionTest(String name) {
		super(name);
	}

	public ChmodActionTest() {
		super("");
	}

	public void testExecuteUndo() {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = createProfile("test", null, profileProperties);

		File zipSource = getTestData("1.0", "/testData/nativeTouchpoint/a.zip");
		File zipTarget = new File(installFolder, "a.zip");
		copy("2.0", zipSource, zipTarget);
		File subDir = new File(installFolder, "subfolder");
		subDir.mkdir();
		File zipTarget2 = new File(subDir, "a.zip");
		copy("3.0", zipSource, zipTarget2);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		parameters.put(InstallableUnitPhase.PARM_ARTIFACT_REQUESTS, new ArrayList());
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);

		parameters.put(ActionConstants.PARM_TARGET_DIR, installFolder.getAbsolutePath());
		parameters.put(ActionConstants.PARM_TARGET_FILE, "a.zip");
		parameters.put(ActionConstants.PARM_PERMISSIONS, "+x");
		Map xparameters = Collections.unmodifiableMap(parameters);

		// TODO: We need a way to verify
		// one idea is to run an executable here (or chmod with -w, -r and then test that files are unreadable
		// and unwriteable. But, that would make this test fail on non UN*X.
		// This is currently just going through the paces to check for any runtime exceptions
		ChmodAction action = new ChmodAction();
		action.execute(xparameters);
		// does nothing so should not alter parameters
		action.undo(xparameters);

		// make a recursive run as well...
		action = new ChmodAction();
		parameters.put(ChmodAction.PARM_OPTIONS, "-R"); // recursive
		parameters.put(ActionConstants.PARM_TARGET_FILE, "subfolder");
		xparameters = Collections.unmodifiableMap(parameters);

		action.execute(xparameters);
		action.undo(xparameters);

		// and one with two parameters
		action = new ChmodAction();
		parameters.put(ChmodAction.PARM_OPTIONS, "-R -H"); // recursive, modify symlinks (follow link).
		parameters.put(ActionConstants.PARM_TARGET_FILE, "subfolder");
		parameters.put(ActionConstants.PARM_PERMISSIONS, "700");

		xparameters = Collections.unmodifiableMap(parameters);
		action.execute(xparameters);
		action.undo(xparameters);

	}
}