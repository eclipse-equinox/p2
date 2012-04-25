/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
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

	/**
	 * Test that when a path is used only files from that path down are unzipped to target as well as undo works.
	 */
	public void testPath() {
		String a = "a.txt";
		String b = "foo/b.txt";
		String c = "foo/bar/car/c.txt";
		String b1 = "b.txt";
		String c1 = "bar/car/c.txt";
		String c2 = "car/c.txt";

		{
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(ActionConstants.PARM_PATH, "foo");
			testUnzip(parameters, getTempFolder(), new String[] {b1, c1}, new String[] {a, b, c});
		}
		{
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(ActionConstants.PARM_PATH, "foo/");
			testUnzip(parameters, getTempFolder(), new String[] {b1, c1}, new String[] {a, b, c});
		}
		{
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(ActionConstants.PARM_PATH, "**/bar");
			testUnzip(parameters, getTempFolder(), new String[] {c2}, new String[] {a, b, c, b1});
		}
	}

	/**
	 * Tests that only the files specified by inclusion path are unzipped as well as undo works.
	 */
	public void testInclusion() {
		String a = "a.txt";
		String b = "foo/b.txt";
		String c = "foo/bar/car/c.txt";

		// full path
		{
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(ActionConstants.PARM_INCLUDE, "foo/b.txt");
			testUnzip(parameters, getTempFolder(), new String[] {b}, new String[] {a, c});
		}
		// wildcarded path
		{
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(ActionConstants.PARM_INCLUDE, "*/b.txt");
			testUnzip(parameters, getTempFolder(), new String[] {b}, new String[] {a, c});
		}
		// subdir wildcarded path
		{
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(ActionConstants.PARM_INCLUDE, "**/c.txt");
			testUnzip(parameters, getTempFolder(), new String[] {c}, new String[] {a, b});
		}
	}

	/**
	 * Tests that only the files specified by exclusion path are not unzipped as well as undo works.
	 */
	public void testExclusion() {
		String a = "a.txt";
		String b = "foo/b.txt";
		String c = "foo/bar/car/c.txt";

		// full path
		{
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(ActionConstants.PARM_EXCLUDE, "foo/b.txt");
			testUnzip(parameters, getTempFolder(), new String[] {a, c}, new String[] {b});
		}
		// wildcarded path
		{
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(ActionConstants.PARM_EXCLUDE, "*/b.txt");
			testUnzip(parameters, getTempFolder(), new String[] {a, c}, new String[] {b});
		}
		// subdir wildcarded path
		{
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(ActionConstants.PARM_EXCLUDE, "**/c.txt");
			testUnzip(parameters, getTempFolder(), new String[] {a, b}, new String[] {c});
		}
	}

	/**
	 * Tests that only the files specified by inclusion path and not in exclusion path are  unzipped as well as undo works.
	 */
	public void testInclusionAndExclusion() {
		String a = "a.txt";
		String b = "foo/b.txt";
		String c = "foo/bar/car/c.txt";

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(ActionConstants.PARM_INCLUDE, "*.txt");
		parameters.put(ActionConstants.PARM_EXCLUDE, "**/c.txt");
		testUnzip(parameters, getTempFolder(), new String[] {a, b}, new String[] {c});
	}

	private void testUnzip(Map<String, String> params, File installFolder, String[] shoudlExistNames, String[] shoudlNotExistNames) {

		ArrayList<File> shoudlExist = new ArrayList<File>();
		ArrayList<File> shoudlNotExist = new ArrayList<File>();

		// first check that are no files in install folder
		for (String fileName : shoudlExistNames) {
			File file = new File(installFolder, fileName);
			shoudlExist.add(file);
			assertFalse("File " + file.getPath() + " should not exist", file.exists());
		}
		for (String fileName : shoudlNotExistNames) {
			File file = new File(installFolder, fileName);
			shoudlNotExist.add(file);
			assertFalse("File " + file.getPath() + " should not exist", file.exists());
		}

		Properties profileProperties = new Properties();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = createProfile("test", profileProperties);

		File zipSource = getTestData("1.0", "/testData/nativeTouchpoint/a.dir.zip");
		File zipTarget = new File(installFolder, "a.dir.zip");
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
		parameters.putAll(params);
		parameters = Collections.unmodifiableMap(parameters);

		UnzipAction action = new UnzipAction();
		action.execute(parameters);
		for (File file : shoudlExist) {
			assertTrue("File " + file.getPath() + " should exist", file.exists());
		}
		for (File file : shoudlNotExist) {
			assertFalse("File " + file.getPath() + " should not exist", file.exists());
		}

		// does nothing so should not alter parameters
		action.undo(parameters);
		// check that undo removed all files
		for (File file : shoudlExist) {
			assertFalse("File " + file.getPath() + " should not exist", file.exists());
		}
		for (File file : shoudlNotExist) {
			assertFalse("File " + file.getPath() + " should not exist", file.exists());
		}
	}
}