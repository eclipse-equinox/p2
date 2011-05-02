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

		Map parameters = new HashMap();
		IProfile profile = createProfile("test");

		touchpoint.initializePhase(null, profile, "test", parameters);
		touchpoint.completePhase(null, profile, "test", parameters);

		parameters.clear();
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		profile = createProfile("test", profileProperties);

		touchpoint.initializePhase(null, profile, "test", parameters);
		touchpoint.completePhase(null, profile, "test", parameters);
	}

	public void testQualifyAction() {
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		assertEquals("org.eclipse.equinox.p2.touchpoint.natives.chmod", touchpoint.qualifyAction("chmod"));
	}
}
