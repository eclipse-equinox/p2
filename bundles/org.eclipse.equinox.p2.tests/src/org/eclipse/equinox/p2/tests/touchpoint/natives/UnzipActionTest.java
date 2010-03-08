/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.natives;

import java.io.*;
import java.util.*;
import org.eclipse.equinox.internal.p2.touchpoint.natives.IBackupStore;
import org.eclipse.equinox.internal.p2.touchpoint.natives.NativeTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.UnzipAction;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class UnzipActionTest extends AbstractProvisioningTest {

	private static void writeToFile(File file, String content) throws IOException {
		file.getParentFile().mkdirs();
		file.createNewFile();
		FileWriter writer = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(writer);
		out.write(content);
		out.close();
	}

	public UnzipActionTest() {
		super("");
	}

	public UnzipActionTest(String name) {
		super(name);
	}

	public void testExecuteUndo() {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = createProfile("test", profileProperties);

		File zipSource = getTestData("1.0", "/testData/nativeTouchpoint/a.zip");
		File zipTarget = new File(installFolder, "a.zip");
		copy("2.0", zipSource, zipTarget);

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

		parameters.put(ActionConstants.PARM_SOURCE, zipTarget.getAbsolutePath());
		parameters.put(ActionConstants.PARM_TARGET, installFolder.getAbsolutePath());
		parameters = Collections.unmodifiableMap(parameters);

		File aTxt = new File(installFolder, "a.txt");
		assertFalse(aTxt.exists());

		UnzipAction action = new UnzipAction();
		action.execute(parameters);
		assertTrue(aTxt.exists());
		// does nothing so should not alter parameters
		action.undo(parameters);
		assertFalse(aTxt.exists());
	}

	public void testExecuteUndoBackup() {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = createProfile("testExecuteUndoBackup", profileProperties);

		File zipSource = getTestData("1.0", "/testData/nativeTouchpoint/a.zip");
		File zipTarget = new File(installFolder, "a.zip");
		copy("2.0", zipSource, zipTarget);

		InstallableUnitDescription iuDesc = new MetadataFactory.InstallableUnitDescription();
		iuDesc.setId("testExecuteUndoBackup");
		iuDesc.setVersion(DEFAULT_VERSION);
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testExecuteUndoBackup", DEFAULT_VERSION);
		iuDesc.setArtifacts(new IArtifactKey[] {key});
		iuDesc.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDesc);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		parameters.put("iu", iu);
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		touchpoint.initializePhase(null, profile, "testExecuteUndoBackup", parameters);

		parameters.put(ActionConstants.PARM_SOURCE, zipTarget.getAbsolutePath());
		parameters.put(ActionConstants.PARM_TARGET, installFolder.getAbsolutePath());
		parameters = Collections.unmodifiableMap(parameters);

		File aTxt = new File(installFolder, "a.txt");
		try {
			writeToFile(aTxt, "ORIGINAL-A");
		} catch (IOException e) {
			fail("Can not write to aTxt");
		}
		assertTrue(aTxt.exists());

		UnzipAction action = new UnzipAction();
		action.execute(parameters);
		assertTrue(aTxt.exists());
		assertFileContent("Should contain text 'nothing'", aTxt, "nothing");
		// does nothing so should not alter parameters
		action.undo(parameters);
		IBackupStore backup = (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP);
		try {
			backup.restore();
		} catch (IOException e) {
			fail("Restore of backup failed", e);
		}
		assertFileContent("Should contain text 'ORIGINAL-A'", aTxt, "ORIGINAL-A");

		backup.discard();
	}

	/**
	 * Tests executing and undoing an unzip action when the profile
	 * id contains characters that are not valid in file names. See bug 274182.
	 */
	public void testUndoBackUpWithSymbolsInProfileId() {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		final String profileId = "Test:With\\Sym/bols";
		IProfile profile = createProfile(profileId, profileProperties);

		File zipSource = getTestData("1.0", "/testData/nativeTouchpoint/a.zip");
		File zipTarget = new File(installFolder, "a.zip");
		copy("2.0", zipSource, zipTarget);

		InstallableUnitDescription iuDesc = new MetadataFactory.InstallableUnitDescription();
		iuDesc.setId(profileId);
		iuDesc.setVersion(DEFAULT_VERSION);
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey(profileId, DEFAULT_VERSION);
		iuDesc.setArtifacts(new IArtifactKey[] {key});
		iuDesc.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDesc);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		parameters.put("iu", iu);
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		touchpoint.initializePhase(null, profile, profileId, parameters);

		parameters.put(ActionConstants.PARM_SOURCE, zipTarget.getAbsolutePath());
		parameters.put(ActionConstants.PARM_TARGET, installFolder.getAbsolutePath());
		parameters = Collections.unmodifiableMap(parameters);

		File aTxt = new File(installFolder, "a.txt");
		try {
			writeToFile(aTxt, "ORIGINAL-A");
		} catch (IOException e) {
			fail("Can not write to aTxt");
		}
		assertTrue(aTxt.exists());

		UnzipAction action = new UnzipAction();
		action.execute(parameters);
		// does nothing so should not alter parameters
		action.undo(parameters);
		IBackupStore backup = (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP);
		try {
			backup.restore();
		} catch (IOException e) {
			fail("Restore of backup failed", e);
		}
		assertFileContent("Should contain text 'ORIGINAL-A'", aTxt, "ORIGINAL-A");

		backup.discard();
	}
}