/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Properties;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * @since 1.0
 */
public class UtilTest extends AbstractProvisioningTest {
	/*
	 * Constructor for the class.
	 */
	public UtilTest(String name) {
		super(name);
	}

	/*
	 * Run all the tests in this class.
	 */
	public static Test suite() {
		return new TestSuite(UtilTest.class);
	}

	public void testDefaultBundlePool() {
		IProfile profile = createProfile("test");
		IAgentLocation agentLocation = (IAgentLocation) getAgent().getService(IAgentLocation.SERVICE_NAME);
		assertEquals(agentLocation.getDataArea("org.eclipse.equinox.p2.touchpoint.eclipse"), Util.getBundlePoolLocation(getAgent(), profile));
	}

	public void testExplicitBundlePool() throws MalformedURLException {
		Properties props = new Properties();
		File cacheDir = new File(System.getProperty("java.io.tmpdir"), "cache");
		props.put(IProfile.PROP_CACHE, cacheDir.toString());
		IProfile profile = createProfile("test", props);
		assertEquals(cacheDir.toURL().toExternalForm(), Util.getBundlePoolLocation(getAgent(), profile).toString());
	}
}
