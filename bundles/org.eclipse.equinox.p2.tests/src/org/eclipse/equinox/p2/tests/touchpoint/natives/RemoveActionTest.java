/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.tests.touchpoint.natives;

import java.io.*;
import java.util.*;
import org.eclipse.equinox.internal.p2.touchpoint.natives.IBackupStore;
import org.eclipse.equinox.internal.p2.touchpoint.natives.NativeTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.RemoveAction;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class RemoveActionTest extends AbstractProvisioningTest {

	public RemoveActionTest(String name) {
		super(name);
	}

	public RemoveActionTest() {
		super("");
	}

	public void testExecuteUndo() {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = createProfile("testExecuteUndo", profileProperties);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		touchpoint.initializePhase(null, profile, "testExecuteUndo", parameters);

		File testFolder = new File(installFolder, "testExecuteUndo");
		File testFile = new File(testFolder, "data.txt");

		parameters.put(ActionConstants.PARM_PATH, testFolder.getAbsolutePath());
		parameters = Collections.unmodifiableMap(parameters);

		testFolder.mkdir();
		assertTrue(testFolder.exists());
		try {
			writeToFile(testFile, "AA\nTestfile with AA on first line.");
		} catch (IOException e1) {
			fail("Could not write test data to test file");
		}
		assertFileContent("Should contain AA", testFile, "AA");

		RemoveAction action = new RemoveAction();
		action.execute(parameters);
		assertFalse(testFolder.exists());
		assertFalse(testFile.exists());

		action.undo(parameters);
		IBackupStore store = (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP);
		if (store != null)
			try {
				store.restore();
			} catch (IOException e) {
				fail("Restore of backup failed");
			}
		assertTrue(testFolder.exists());
		assertFileContent("Should contain AA", testFile, "AA");
		if (store != null)
			store.discard();
	}

	private static void writeToFile(File file, String content) throws IOException {
		file.getParentFile().mkdirs();
		file.createNewFile();
		FileWriter writer = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(writer);
		out.write(content);
		out.close();
	}

}