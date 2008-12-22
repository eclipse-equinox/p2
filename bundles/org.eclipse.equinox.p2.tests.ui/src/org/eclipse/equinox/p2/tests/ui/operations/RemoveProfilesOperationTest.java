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

import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.RemoveProfilesOperation;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

/**
 * Tests for {@link RemoveProfilesOperation}
 */
public class RemoveProfilesOperationTest extends AbstractProvisioningUITest {
	public void testRemoveExisting() {
		String profileId = "testRemoveNonExisting";
		IProfile p = createProfile(profileId);
		RemoveProfilesOperation op = new RemoveProfilesOperation("label", new String[] {profileId});

		try {
			op.execute(getMonitor());
		} catch (ProvisionException e) {
			fail("0.99", e);
		}

		try {
			p = ProvisioningUtil.getProfile(profileId);
		} catch (ProvisionException e) {
			fail("1.99", e);
		}
		assertNull("2.0", p);
	}

	public void testRemoveNonExisting() {
		String profileId = "testRemoveNonExisting";
		IProfile p = createProfile(profileId);
		try {
			ProvisioningUtil.removeProfile(profileId, getMonitor());
		} catch (ProvisionException e) {
			fail("0.99", e);
		}
		RemoveProfilesOperation op = new RemoveProfilesOperation("label", new String[] {profileId});

		//Currently the profile registry does not mind if we try to delete a profile that doesn't exist, so
		//the UI classes don't test for it.
		try {
			op.execute(getMonitor());
		} catch (ProvisionException e) {
			//expected
		}

		try {
			p = ProvisioningUtil.getProfile(profileId);
		} catch (ProvisionException e) {
			fail("2.99", e);
		}
		assertNull("1.0", p);
	}
}
