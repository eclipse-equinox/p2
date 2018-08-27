/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.natives;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.internal.p2.touchpoint.natives.NativeTouchpoint;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class NativeTouchpointTest extends AbstractProvisioningTest {

	public NativeTouchpointTest(String name) {
		super(name);
	}

	public NativeTouchpointTest() {
		super("");
	}

	public void testInitializeCompletePhase() {
		NativeTouchpoint touchpoint = new NativeTouchpoint();

		Map<String, Object> parameters = new HashMap<>();
		IProfile profile = createProfile("test");

		touchpoint.initializePhase(null, profile, "test", parameters);
		touchpoint.completePhase(null, profile, "test", parameters);

		parameters.clear();
		Map<String, String> profileProperties = new HashMap<>();
		File installFolder = getTempFolder();
		profileProperties.put(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		profile = createProfile("test", profileProperties);

		touchpoint.initializePhase(null, profile, "test", parameters);
		touchpoint.completePhase(null, profile, "test", parameters);
	}

	public void testQualifyAction() {
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		assertEquals("org.eclipse.equinox.p2.touchpoint.natives.chmod", touchpoint.qualifyAction("chmod"));
	}
}
