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
package org.eclipse.equinox.p2.tests.engine;

import org.eclipse.equinox.internal.p2.engine.TouchpointManager;
import org.eclipse.equinox.internal.provisional.p2.metadata.TouchpointType;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Simple test of the engine API.
 */
public class TouchpointManagerTest extends AbstractProvisioningTest {

	public TouchpointManagerTest(String name) {
		super(name);
	}

	public TouchpointManagerTest() {
		super("");
	}

	protected void setUp() throws Exception {
	}

	protected void tearDown() throws Exception {
	}

	public void testGetTouchpointByType() {
		TouchpointManager manager = TouchpointManager.getInstance();
		assertNotNull(manager.getTouchpoint(TouchpointType.NONE));
	}

	public void testGetTouchpointByIdWithVersion() {
		TouchpointManager manager = TouchpointManager.getInstance();
		assertNotNull(manager.getTouchpoint("phaseTest", "1.0.0"));
	}

	public void testGetTouchpointByIdWithNullVersion() {
		TouchpointManager manager = TouchpointManager.getInstance();
		assertNotNull(manager.getTouchpoint("phaseTest", null));
	}
}
