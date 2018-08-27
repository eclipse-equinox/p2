/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *     SAP AG - Ongoing development
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

	private File testFolder;
	private File testFile;

	private Map<String, Object> parameters;
	private RemoveAction action;

	public RemoveActionTest(String name) {
		super(name);
	}

	public RemoveActionTest() {
		super("");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		if (testFolder.exists()) {
			delete(testFolder);
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		Map<String, String> profileProperties = new HashMap<>();
		File installFolder = getTempFolder();
		profileProperties.put(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = createProfile("testExecuteUndo", profileProperties);

		parameters = new HashMap<>();
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		touchpoint.initializePhase(null, profile, "testExecuteUndo", parameters);

		testFolder = new File(installFolder, "testExecuteUndo");
		testFile = new File(testFolder, "data.txt");

		parameters.put(ActionConstants.PARM_PATH, testFolder.getAbsolutePath());
		parameters = Collections.unmodifiableMap(parameters);
	}

	public void testExecuteUndo() {

		executeRemoveActionOnNonEmptyDir();

		action.undo(parameters);
		IBackupStore store = (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP);
		if (store != null)
			try {
				store.restore();
			} catch (IOException e) {
				fail("Restore of backup failed");
			}
		assertTrue("Test folder was not restored from backup", testFolder.exists());
		assertFileContent("Should contain AA", testFile, "AA");
		if (store != null)
			store.discard();
	}

	public void testExecuteMultipleRemovesOnTheSameDir() {

		executeRemoveActionOnNonEmptyDir();
		executeRemoveActionOnNonEmptyDir();

	}

	public void testExecuteMultipleRemovesOnTheSameEmptyDir() {

		executeRemoveActionOnEmptyDir("Test folder exists after executing RemoveAction for the first time");
		executeRemoveActionOnEmptyDir("Test folder exists after executing RemoveAction for the second time");
	}

	public void testExecuteMultipleRemovesOnTheSameEmptyDir2() {

		executeRemoveActionOnEmptyDir("Test folder exists after executing RemoveAction for the first time");
		executeUncheckedRemoveActionOnNonEmptyDir();
		executeRemoveActionOnEmptyDir("Test folder exists after executing RemoveAction for the third time");
	}

	private void writeTestFile() {
		testFolder.mkdir();
		assertTrue("Test folder was not created before removal", testFolder.exists());
		try {
			writeToFile(testFile, "AA\nTestfile with AA on first line.");
		} catch (IOException e1) {
			fail("Could not write test data to test file");
		}
		assertFileContent("Test file should contain AA", testFile, "AA");
	}

	private void executeRemoveActionOnNonEmptyDir() {
		writeTestFile();
		action = new RemoveAction();
		action.execute(parameters);
		assertFalse("Test file exists after executing RemoveAction", testFile.exists());
		assertFalse("Test folder exists after executing RemoveAction", testFolder.exists());
	}

	private void executeRemoveActionOnEmptyDir(String failureMessage) {
		executeUncheckedRemoveActionOnNonEmptyDir();
		assertFalse(failureMessage, testFolder.exists());
	}

	private void executeUncheckedRemoveActionOnNonEmptyDir() {
		testFolder.mkdir();
		assertTrue("Test folder was not created before removal", testFolder.exists());
		action = new RemoveAction();
		action.execute(parameters);
	}

	private static void writeToFile(File file, String content) throws IOException {
		file.getParentFile().mkdirs();
		file.createNewFile();
		try (FileWriter writer = new FileWriter(file); BufferedWriter out = new BufferedWriter(writer)) {
			out.write(content);
		}
	}
}