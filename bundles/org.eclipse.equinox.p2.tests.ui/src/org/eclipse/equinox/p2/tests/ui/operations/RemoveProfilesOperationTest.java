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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.RemoveProfilesOperation;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests for {@link RemoveProfilesOperation}
 */
public class RemoveProfilesOperationTest extends AbstractProvisioningTest {
	public static final String TEST_PROP_KEY = "TEST_PROP_KEY";
	public static final String TEST_PROP_VALUE = "TEST_PROP_VALUE";

	public void testRemoveExisting() {
		String profileId = "testRemoveNonExisting";
		IProfile profile = createProfile(profileId);
		RemoveProfilesOperation op = new RemoveProfilesOperation("label", new IProfile[] {profile});

		assertTrue("1.0", op.canExecute());
		assertTrue("1.1", !op.canUndo());

		try {
			op.execute(getMonitor(), null);
		} catch (ExecutionException e) {
			fail("0.99", e);
		}

		try {
			profile = ProvisioningUtil.getProfile(profileId);
		} catch (ProvisionException e) {
			fail("1.99", e);
		}
		assertNull("2.0", profile);
	}

	/**
	 * Commented out due to bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=247540.
	 */
	public void _testRemoveNonExisting() {
		String profileId = "testRemoveNonExisting";
		IProfile profile = createProfile(profileId);
		try {
			ProvisioningUtil.removeProfile(profileId, getMonitor());
		} catch (ProvisionException e) {
			fail("0.99", e);
		}
		RemoveProfilesOperation op = new RemoveProfilesOperation("label", new IProfile[] {profile});

		//currently running doesn't fail and behavior is not specific either way
		try {
			op.execute(getMonitor(), null);
		} catch (ExecutionException e) {
			//expected
		}

		//redo shouldn't recreate the profile since we never removed it
		try {
			op.undo(getMonitor(), null);
		} catch (ExecutionException e) {
			fail("1.99", e);
		}

		try {
			profile = ProvisioningUtil.getProfile(profileId);
		} catch (ProvisionException e) {
			fail("2.99", e);
		}
		assertNull("1.0", profile);
	}

	public void testUndoRedo() {
		String profileId = "testUndoRedo";
		IProfile profile = createProfile(profileId);
		RemoveProfilesOperation op = new RemoveProfilesOperation("label", new IProfile[] {profile});

		try {
			op.execute(getMonitor(), null);
		} catch (ExecutionException e) {
			fail("0.99", e);
		}

		//now undo
		try {
			op.undo(getMonitor(), null);
		} catch (ExecutionException e1) {
			fail("1.99", e1);
		}

		try {
			profile = ProvisioningUtil.getProfile(profileId);
		} catch (ProvisionException e) {
			fail("2.99", e);
		}
		assertNotNull("2.0", profile);
		assertEquals("2.1", profileId, profile.getProfileId());

		try {
			op.redo(getMonitor(), null);
		} catch (ExecutionException e) {
			fail("3.99", e);
		}
		try {
			profile = ProvisioningUtil.getProfile(profileId);
		} catch (ProvisionException e) {
			fail("2.99", e);
		}

		assertNull("4.0", profile);
	}
}
