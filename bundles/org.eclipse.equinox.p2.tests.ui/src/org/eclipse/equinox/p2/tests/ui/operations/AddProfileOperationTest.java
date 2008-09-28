/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.operations;

import java.util.HashMap;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.AddProfileOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests for {@link AddProfileOperation}.
 */
public class AddProfileOperationTest extends AbstractProvisioningTest {
	public static final String TEST_PROP_KEY = "TEST_PROP_KEY";
	public static final String TEST_PROP_VALUE = "TEST_PROP_VALUE";

	/**
	 * Tests attempting to add a profile that already exists.
	 */
	public void testAddExistingProfile() {
		String profileId = "add-existing";
		IProfile profile = createProfile(profileId, null, new HashMap());
		profilesToRemove.add(profileId);
		AddProfileOperation op = new AddProfileOperation("label", profile);

		assertTrue("1.0", !op.canUndo());
		assertTrue("1.1", op.canExecute());

		try {
			op.execute(getMonitor(), null);
			fail("1.2");//should have failed
		} catch (ExecutionException e) {
			//failure expected
		}

		//it should not be possible to undo because it failed
		assertTrue("2.0", !op.canUndo());
		assertTrue("2.1", op.canExecute());
		assertTrue("2.2", op.canRedo());
	}

	/**
	 * Tests a simple profile addition that should succeed
	 */
	public void testAddSimple() {
		HashMap properties = new HashMap();
		properties.put(TEST_PROP_KEY, TEST_PROP_VALUE);
		String profileId = "add-simple";
		profilesToRemove.add(profileId);
		AddProfileOperation op = new AddProfileOperation("label", profileId, properties);

		assertTrue("1.0", !op.canUndo());
		assertTrue("1.1", op.canExecute());

		try {
			IStatus result = op.execute(getMonitor(), null);
			assertTrue("1.2", result.isOK());
		} catch (ExecutionException e) {
			fail("0.99", e);
		}

		//it should be possible to undo
		assertTrue("2.0", op.canUndo());
		assertTrue("2.1", !op.canExecute());
		assertTrue("2.2", !op.canRedo());

		IProfile profile = null;
		try {
			profile = ProvisioningUtil.getProfile(profileId);
		} catch (ProvisionException e) {
			fail("2.99", e);
			return;
		}
		assertNotNull("3.0", profile);
		assertEquals("3.1", TEST_PROP_VALUE, profile.getProperty(TEST_PROP_KEY));
	}

	/**
	 * Tests undoing a simple profile addition.
	 * Commented out due to bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=247532.
	 */
	public void _testUndoRedo() {
		String profileId = "simple-undo";
		profilesToRemove.add(profileId);
		HashMap properties = new HashMap();
		properties.put(TEST_PROP_KEY, TEST_PROP_VALUE);
		AddProfileOperation op = new AddProfileOperation("label", profileId, properties);

		try {
			op.execute(getMonitor(), null);
		} catch (ExecutionException e) {
			fail("0.99", e);
		}

		//now undo
		try {
			IStatus result = op.undo(getMonitor(), null);
			assertTrue("1.0", result.isOK());
		} catch (ExecutionException e1) {
			fail("1.99", e1);
		}

		assertTrue("1.1", !op.canUndo());
		assertTrue("1.2", op.canRedo());

		IProfile profile = null;
		try {
			profile = ProvisioningUtil.getProfile(profileId);
		} catch (ProvisionException e) {
			fail("2.99", e);
			return;
		}
		//profile should not exist
		assertNull("2.0", profile);

		//now redo the operation
		try {
			op.redo(getMonitor(), null);
		} catch (ExecutionException e) {
			fail("3.99", e);
		}

		//the profile should exist
		try {
			profile = ProvisioningUtil.getProfile(profileId);
		} catch (ProvisionException e) {
			fail("4.99", e);
			return;
		}
		assertNotNull("4.0", profile);
		assertEquals("4.1", TEST_PROP_VALUE, profile.getProperty(TEST_PROP_KEY));
	}
}
