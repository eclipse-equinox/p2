/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.natives;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.p2.touchpoint.natives.NativeTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.LinkAction;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class LinkActionTest extends AbstractProvisioningTest {

	public LinkActionTest(String name) {
		super(name);
	}

	public LinkActionTest() {
		super("");
	}

	public void testExecuteUndo() {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = createProfile("test", profileProperties);

		File zipSource = getTestData("1.0", "/testData/nativeTouchpoint/a.zip");
		File zipTarget = new File(installFolder, "a.zip");
		copy("2.0", zipSource, zipTarget);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);

		parameters.put(ActionConstants.PARM_TARGET_DIR, installFolder.getAbsolutePath());
		parameters.put(ActionConstants.PARM_LINK_NAME, "b.zip");
		parameters.put(ActionConstants.PARM_LINK_TARGET, zipTarget.getAbsolutePath());
		parameters = Collections.unmodifiableMap(parameters);

		// TODO: We need a way to verify success
		// one idea is to run an executable here
		// This is currently just going through the paces to check for any runtime exceptions
		LinkAction action = new LinkAction();
		assertOK("3.0", action.execute(parameters));
		// does nothing so should not alter parameters
		assertOK("3.1", action.undo(parameters));
		File linkFile = new File((String) parameters.get(ActionConstants.PARM_TARGET_DIR));
		linkFile = new File(linkFile, (String) parameters.get(ActionConstants.PARM_LINK_NAME));
		assertFalse("3.2", linkFile.exists());
	}
}