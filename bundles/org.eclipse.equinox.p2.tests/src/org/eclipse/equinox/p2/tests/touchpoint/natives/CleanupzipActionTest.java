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
import org.eclipse.equinox.internal.p2.touchpoint.natives.IBackupStore;
import org.eclipse.equinox.internal.p2.touchpoint.natives.NativeTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class CleanupzipActionTest extends AbstractProvisioningTest {

	public CleanupzipActionTest(String name) {
		super(name);
	}

	public CleanupzipActionTest() {
		super("");
	}

	IBackupStore store;

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (store != null)
			store.discard();
	}

	public void testExecuteUndo() {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = createProfile("testExecuteUndo", profileProperties);

		File zipSource = getTestData("1.0", "/testData/nativeTouchpoint/a.zip");
		File zipTarget = new File(installFolder, "a.zip");
		copy("2.0", zipSource, zipTarget);

		InstallableUnitDescription iuDesc = new MetadataFactory.InstallableUnitDescription();
		iuDesc.setId("test");
		iuDesc.setVersion(DEFAULT_VERSION);
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testExecuteUndo", DEFAULT_VERSION);
		iuDesc.setArtifacts(new IArtifactKey[] {key});
		iuDesc.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDesc);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		parameters.put("iu", iu);
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		touchpoint.initializePhase(null, profile, "testExecuteUndo", parameters);
		store = (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP);

		parameters.put(ActionConstants.PARM_SOURCE, zipTarget.getAbsolutePath());
		parameters.put(ActionConstants.PARM_TARGET, installFolder.getAbsolutePath());
		parameters = Collections.unmodifiableMap(parameters);

		File aTxt = new File(installFolder, "a.txt");
		new UnzipAction().execute(parameters);
		assertTrue(aTxt.exists());
		assertEquals(1, profile.getInstallableUnitProperties(iu).size());

		CleanupzipAction action = new CleanupzipAction();
		action.execute(parameters);
		assertFalse(aTxt.exists());
		assertEquals(0, profile.getInstallableUnitProperties(iu).size());

		// does nothing so should not alter parameters
		action.undo(parameters);
		assertTrue(aTxt.exists());
		assertEquals(1, profile.getInstallableUnitProperties(iu).size());
	}

	public void testExecuteUndoWhereInstallFolderIsDifferent() {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = createProfile("testExecuteUndoWhereInstallFolderIsDifferent", profileProperties);

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
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);
		store = (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP);

		parameters.put(ActionConstants.PARM_SOURCE, zipTarget.getAbsolutePath());
		parameters.put(ActionConstants.PARM_TARGET, installFolder.getAbsolutePath());

		File aTxt = new File(installFolder, "a.txt");
		new UnzipAction().execute(parameters);
		assertTrue(aTxt.exists());
		assertEquals(1, profile.getInstallableUnitProperties(iu).size());

		File installFolder2 = getTempFolder();
		copy("", installFolder, installFolder2);
		parameters.put(ActionConstants.PARM_TARGET, installFolder2.getAbsolutePath());

		CleanupzipAction action = new CleanupzipAction();
		action.execute(parameters);
		File aTxt2 = new File(installFolder2, "a.txt");
		assertFalse(aTxt2.exists());
		assertEquals(0, profile.getInstallableUnitProperties(iu).size());

		// does nothing so should not alter parameters
		action.undo(parameters);
		assertTrue(aTxt2.exists());
		assertEquals(1, profile.getInstallableUnitProperties(iu).size());

	}

	/**
	 * Test that directories are removed when nested zip is unzipped.
	 */
	public void testDirectoryCleanup() {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = createProfile("testExecuteUndo", profileProperties);

		File zipSource = getTestData("1.0", "/testData/nativeTouchpoint/nestedFolder.zip");
		File zipTarget = new File(installFolder, "nestedFolder.zip");
		copy("2.0", zipSource, zipTarget);

		InstallableUnitDescription iuDesc = new MetadataFactory.InstallableUnitDescription();
		iuDesc.setId("test");
		iuDesc.setVersion(DEFAULT_VERSION);
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testDirectoryCleanup", DEFAULT_VERSION);
		iuDesc.setArtifacts(new IArtifactKey[] {key});
		iuDesc.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDesc);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		parameters.put("iu", iu);
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		touchpoint.initializePhase(null, profile, "testDirectoryCleanup", parameters);
		store = (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP);

		parameters.put(ActionConstants.PARM_SOURCE, zipTarget.getAbsolutePath());
		parameters.put(ActionConstants.PARM_TARGET, installFolder.getAbsolutePath());
		parameters = Collections.unmodifiableMap(parameters);

		new UnzipAction().execute(parameters);
		assertEquals(1, profile.getInstallableUnitProperties(iu).size());

		File aTxt = new File(installFolder, "nestedFolder/innerFolder/a.txt");
		File innerFolder = new File(installFolder, "nestedFolder/innerFolder");
		File nestedFolder = new File(installFolder, "nestedFolder");

		assertTrue("File " + aTxt.getAbsolutePath() + " should exist", aTxt.exists());
		assertTrue("Folder " + innerFolder.getAbsolutePath() + " should exist", innerFolder.exists());
		assertTrue("Folder " + nestedFolder.getAbsolutePath() + " should exist", nestedFolder.exists());

		CleanupzipAction action = new CleanupzipAction();
		action.execute(parameters);

		assertEquals(0, profile.getInstallableUnitProperties(iu).size());

		assertFalse("File " + aTxt.getAbsolutePath() + " should not exist", aTxt.exists());
		assertFalse("Folder " + innerFolder.getAbsolutePath() + " should not exist", innerFolder.exists());
		assertFalse("Folder " + nestedFolder.getAbsolutePath() + " should not exist", nestedFolder.exists());
	}
}