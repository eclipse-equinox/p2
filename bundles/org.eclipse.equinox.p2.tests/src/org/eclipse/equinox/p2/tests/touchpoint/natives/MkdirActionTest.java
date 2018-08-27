/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.natives;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.p2.touchpoint.natives.NativeTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.MkdirAction;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class MkdirActionTest extends AbstractProvisioningTest {

	public MkdirActionTest(String name) {
		super(name);
	}

	public MkdirActionTest() {
		super("");
	}

	public void testExecuteUndo() {
		Map<String, String> profileProperties = new HashMap<>();
		File installFolder = getTempFolder();
		profileProperties.put(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = createProfile("test", profileProperties);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);

		File testFolder = new File(installFolder, "test");

		parameters.put(ActionConstants.PARM_PATH, testFolder.getAbsolutePath());
		parameters = Collections.unmodifiableMap(parameters);

		assertFalse(testFolder.exists());

		MkdirAction action = new MkdirAction();
		action.execute(parameters);
		assertTrue(testFolder.exists());

		action.undo(parameters);
		assertFalse(testFolder.exists());
	}
}