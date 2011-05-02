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
package org.eclipse.equinox.p2.tests.ui.actions;

import org.eclipse.equinox.internal.p2.ui.actions.RemoveColocatedRepositoryAction;

public class RemoveColocatedRepositoryActionTest extends ColocatedRepositoryActionTest {
	public void testEmptySelection() {
		RemoveColocatedRepositoryAction action = new RemoveColocatedRepositoryAction(getProvisioningUI(), getSelectionProvider(getEmptySelection()));
		assertFalse("Should not be enabled with empty selection", action.isEnabled());
	}

	public void testInvalidSelection() {
		RemoveColocatedRepositoryAction action = new RemoveColocatedRepositoryAction(getProvisioningUI(), getSelectionProvider(getInvalidSelection()));
		assertFalse("Should not be enabled with invalid selection", action.isEnabled());
	}

	public void testRemoveRepo() {
		assertTrue(managerContains(metaManager, testRepoLocation));
		assertTrue(managerContains(artifactManager, testRepoLocation));

		RemoveColocatedRepositoryAction action = new RemoveColocatedRepositoryAction(getProvisioningUI(), getSelectionProvider(getValidRepoSelection()));
		assertTrue("Should be enabled", action.isEnabled());
		action.run();

		// Action runs asynchronously so these tests don't apply.
		// Right now this test is only testing enablement and the ability to run
		// assertFalse(managerContains(metaManager, testRepoLocation));
		// assertFalse(managerContains(artifactManager, testRepoLocation));
	}
}
