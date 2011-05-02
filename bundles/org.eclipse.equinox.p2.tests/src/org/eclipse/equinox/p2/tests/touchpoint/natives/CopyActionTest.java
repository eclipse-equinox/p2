/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.natives;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.p2.touchpoint.natives.NativeTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.CopyAction;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class CopyActionTest extends AbstractProvisioningTest {

	public CopyActionTest(String name) {
		super(name);
	}

	public CopyActionTest() {
		super("");
	}

	public void testExecuteUndo() {
		Map parameters = createParameters("/testData/nativeTouchpoint/aFolder/a.txt", "a.txt", false);
		Map safeParameters = Collections.unmodifiableMap(parameters);

		CopyAction action = new CopyAction();
		action.execute(safeParameters);

		// Verify that the right file was copied
		File target = new File((String) parameters.get(ActionConstants.PARM_COPY_TARGET));
		assertFileContent("copied content", target, "A");

		// does nothing so should not alter parameters
		action.undo(safeParameters);
		assertFalse("Target should be removed after undo", target.exists());
	}

	public void testCopyDirectory() {
		Map parameters = createParameters("/testData/nativeTouchpoint/aFolder/", "aFolder", false);
		Map safeParameters = Collections.unmodifiableMap(parameters);

		CopyAction action = new CopyAction();
		action.execute(safeParameters);

		// Verify that the right files was copied
		File target = new File((String) parameters.get(ActionConstants.PARM_COPY_TARGET));
		assertFileContent("copied content A", new File(target, "a.txt"), "A");
		assertFileContent("copied content B", new File(target, "b.txt"), "B");

		// does nothing so should not alter parameters
		action.undo(safeParameters);
		assertFalse("Target should be removed after undo", target.exists());
	}

	public void testMergeDirectory() {
		Map parameters1 = createParameters("/testData/nativeTouchpoint/xFolder/", "aFolder", true);
		Map safeParameters1 = Collections.unmodifiableMap(parameters1);

		CopyAction action1 = new CopyAction();
		action1.execute(safeParameters1);

		// Verify that the right files was copied
		File target = new File((String) parameters1.get(ActionConstants.PARM_COPY_TARGET));
		assertFileContent("copied content X", new File(target, "x.txt"), "X");
		assertFileContent("copied content Y", new File(target, "y.txt"), "Y");

		Map parameters2 = new HashMap();
		parameters2.putAll(parameters1);
		parameters2.put(ActionConstants.PARM_COPY_SOURCE, getTestData("get folder A", "/testData/nativeTouchpoint/aFolder/").getAbsolutePath());
		Map safeParameters2 = Collections.unmodifiableMap(parameters2);

		CopyAction action2 = new CopyAction();
		action2.execute(safeParameters2);
		assertFileContent("copied content A", new File(target, "a.txt"), "A");
		assertFileContent("copied content B", new File(target, "b.txt"), "B");

		// undo copy of x and y
		action1.undo(safeParameters1);
		assertTrue("Target should exist after undo", target.exists());
		File tmp = new File(target, "x.txt");
		assertFalse("File x should not exist", tmp.exists());
		tmp = new File(target, "y.txt");
		assertFalse("File y should not exist", tmp.exists());
		assertFileContent("copied content A", new File(target, "a.txt"), "A");
		assertFileContent("copied content B", new File(target, "b.txt"), "B");

		// undo copy of a and b
		action2.undo(safeParameters2);
		assertFalse("Target should not exist after undo", target.exists());
	}

	public void testMergeOverwrite() {
		Map parameters1 = createParameters("/testData/nativeTouchpoint/bcFolder/", "aFolder", true);
		Map safeParameters1 = Collections.unmodifiableMap(parameters1);

		CopyAction action1 = new CopyAction();
		action1.execute(safeParameters1);

		// Verify that the right file was copied (a b.txt with a C in it [sic])
		File target = new File((String) parameters1.get(ActionConstants.PARM_COPY_TARGET));
		assertFileContent("copied content C", new File(target, "b.txt"), "C"); // [sic]

		Map parameters2 = new HashMap();
		parameters2.putAll(parameters1);
		parameters2.put(ActionConstants.PARM_COPY_SOURCE, getTestData("get folder A", "/testData/nativeTouchpoint/aFolder/").getAbsolutePath());
		Map safeParameters2 = Collections.unmodifiableMap(parameters2);

		CopyAction action2 = new CopyAction();
		action2.execute(safeParameters2);
		assertFileContent("copied content A", new File(target, "a.txt"), "A");
		assertFileContent("copied content B", new File(target, "b.txt"), "B");

		// undo copy of a and b
		action2.undo(safeParameters2);
		assertFalse("Target should not exist after undo", target.exists());
	}

	public void testBlockedMergeOverwrite() {
		Map parameters1 = createParameters("/testData/nativeTouchpoint/bcFolder/", "aFolder", false);
		Map safeParameters1 = Collections.unmodifiableMap(parameters1);

		CopyAction action1 = new CopyAction();
		action1.execute(safeParameters1);

		// Verify that the right file was copied (a b.txt with a C in it [sic])
		File target = new File((String) parameters1.get(ActionConstants.PARM_COPY_TARGET));
		assertFileContent("copied content B", new File(target, "b.txt"), "C"); // [sic]

		Map parameters2 = new HashMap();
		parameters2.putAll(parameters1);
		parameters2.put(ActionConstants.PARM_COPY_SOURCE, getTestData("get folder A", "/testData/nativeTouchpoint/aFolder/").getAbsolutePath());
		Map safeParameters2 = Collections.unmodifiableMap(parameters2);

		CopyAction action2 = new CopyAction();
		assertFalse("Overwrite of b.txt should not succeed", action2.execute(safeParameters2).isOK());
		assertFileContent("copied content B", new File(target, "b.txt"), "C"); // [sic]

	}

	public void testOverwrite() {
		Map parameters = createParameters("/testData/nativeTouchpoint/aFolder/a.txt", "a.txt", true);
		Map safeParameters = Collections.unmodifiableMap(parameters);

		File source = new File((String) parameters.get(ActionConstants.PARM_COPY_SOURCE));
		File target = new File((String) parameters.get(ActionConstants.PARM_COPY_TARGET));

		// test an overwrite - by first copying the b file
		copy("2.0", getTestData("1.0", "/testData/nativeTouchpoint/aFolder/b.txt"), target);

		CopyAction action = new CopyAction();
		action.execute(safeParameters);
		// Verify that the right file was copied
		assertFileContent("copied content", target, "A");
		// and that we did nothing bad to the source
		assertFileContent("source content", source, "A");

		assertTrue("copy action status", action.undo(safeParameters).isOK());
		assertFalse("Target should be removed after undo", target.exists());
	}

	public void testBlockedOverwrite() {
		Map parameters = createParameters("/testData/nativeTouchpoint/aFolder/a.txt", "a.txt", false);
		Map safeParameters = Collections.unmodifiableMap(parameters);

		File source = new File((String) parameters.get(ActionConstants.PARM_COPY_SOURCE));
		File target = new File((String) parameters.get(ActionConstants.PARM_COPY_TARGET));

		// test an overwrite - by first copying the b file
		copy("2.0", getTestData("1.0", "/testData/nativeTouchpoint/aFolder/b.txt"), target);

		CopyAction action = new CopyAction();
		assertFalse("copy action status", action.execute(safeParameters).isOK());

		// Verify that nothing was copied
		assertFileContent("original content", target, "B");
		// and that we did nothing bad to the source
		assertFileContent("source content", source, "A");

		// there is nothing to undo - the B file should still be there
		action.undo(safeParameters);
		assertTrue("Target should remain after undo", target.exists());
		assertFileContent("original content", target, "B");
	}

	/*
	 * TODO: testing of the following
	 * - copy of directory - check that it merges
	 * - copy of directory with overwrite false/true
	 */
	private Map createParameters(String sourceName, String targetName, boolean overwrite) {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = createProfile("test", profileProperties);

		File source = getTestData("1.0", sourceName);
		File target = new File(installFolder, targetName);

		InstallableUnitDescription iuDesc = new MetadataFactory.InstallableUnitDescription();
		iuDesc.setId("test");
		iuDesc.setVersion(DEFAULT_VERSION);
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("test", DEFAULT_VERSION);
		iuDesc.setArtifacts(new IArtifactKey[] {key});
		iuDesc.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDesc);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		parameters.put("iu", iu);
		parameters.put(ActionConstants.PARM_PROFILE, profile);

		NativeTouchpoint touchpoint = new NativeTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);

		parameters.put(ActionConstants.PARM_COPY_SOURCE, source.getAbsolutePath());
		parameters.put(ActionConstants.PARM_COPY_TARGET, target.getAbsolutePath());
		parameters.put(ActionConstants.PARM_COPY_OVERWRITE, Boolean.toString(overwrite));
		return parameters;
	}

}
