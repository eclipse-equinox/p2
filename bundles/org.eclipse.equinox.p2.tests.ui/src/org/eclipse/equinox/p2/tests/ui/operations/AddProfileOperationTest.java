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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.AddProfileOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

/**
 * Tests for {@link AddProfileOperation}.
 */
public class AddProfileOperationTest extends AbstractProvisioningUITest {
	public static final String TEST_PROP_KEY = "TEST_PROP_KEY";
	public static final String TEST_PROP_VALUE = "TEST_PROP_VALUE";

	/**
	 * Tests a simple profile addition that should succeed
	 */
	public void testAddSimple() {
		HashMap properties = new HashMap();
		properties.put(TEST_PROP_KEY, TEST_PROP_VALUE);
		String profileId = "add-simple";
		profilesToRemove.add(profileId);
		AddProfileOperation op = new AddProfileOperation("label", profileId, properties);

		try {
			IStatus result = op.execute(getMonitor());
			assertTrue("1.2", result.isOK());
		} catch (ProvisionException e) {
			fail("0.99", e);
		}

		IProfile p = null;
		try {
			p = ProvisioningUtil.getProfile(profileId);
		} catch (ProvisionException e) {
			fail("2.99", e);
			return;
		}
		assertNotNull("3.0", p);
		assertEquals("3.1", TEST_PROP_VALUE, p.getProperty(TEST_PROP_KEY));
	}
}
