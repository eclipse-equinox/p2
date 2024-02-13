/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.io.File;
import java.net.MalformedURLException;
import org.eclipse.equinox.internal.p2.engine.ActionManager;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Simple test of the engine API.
 */
public class ActionManagerTest extends AbstractProvisioningTest {

	public ActionManagerTest(String name) {
		super(name);
	}

	public ActionManagerTest() {
		super("");
	}

	public void testGetTouchpointQualifiedActionId() {
		ActionManager manager = new ActionManager();
		assertNotNull(manager.getTouchpointQualifiedActionId("test", MetadataFactory.createTouchpointType("phaseTest", Version.create("1"))));
	}

	public void testGetActionWithVersion() {
		ActionManager manager = new ActionManager();
		assertNotNull(manager.getAction("test1.test", new VersionRange("1.0.0")));
	}

	public void testGetActionWithNullVersion() {
		ActionManager manager = new ActionManager();
		assertNotNull(manager.getAction("test1.test", null));
	}

	@SuppressWarnings("deprecation") // java.io.File.toURL()
	// temporarily disabling this test until API is done
	public void DISABLED_testDynamicAction() throws MalformedURLException, BundleException, InterruptedException {
		ActionManager manager = new ActionManager();
		assertNull(manager.getAction("dummy.touchpointAndAction.dummy", new VersionRange("1.0.0")));
		File dummy = getTestData("0.1", "/testData/engineTest/dummy.touchpointAndAction_1.0.0.jar");
		Bundle bundle = TestActivator.getContext().installBundle(dummy.toURL().toString());
		bundle.start(); //force resolve

		int maxTries = 20;
		int current = 0;
		while (true) {
			if (null != manager.getAction("dummy.touchpointAndAction.dummy", new VersionRange("1.0.0")))
				break;
			if (++current == maxTries)
				fail("dummy action not added");
			Thread.sleep(100);
		}
		bundle.uninstall();
		current = 0;
		while (true) {
			if (null == manager.getAction("dummy.touchpointAndAction.dummy", new VersionRange("1.0.0")))
				break;
			if (++current == maxTries)
				fail("dummy action not removed");
			Thread.sleep(100);
		}
	}
}
