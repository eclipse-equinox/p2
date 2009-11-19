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

import java.net.URI;
import java.util.Arrays;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.AddColocatedRepositoryJob;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

/**
 * Tests for {@link AddColocatedRepositoryJob}.
 */
public class RemoveColocatedRepositoryOperationTest extends AbstractProvisioningUITest {
	public void testRemoveSingleRepository() {
		URI repoLocation = null;
		try {
			repoLocation = TestData.getFile("artifactRepo", "").toURI();
		} catch (Exception e) {
			fail("0.99", e);
		}
		AddColocatedRepositoryJob op = new AddColocatedRepositoryJob("label", getSession(), repoLocation);

		IStatus status = op.runModal(getMonitor());
		assertTrue("1.99", status.isOK());

		URI[] repos = metaManager.getKnownRepositories(0);
		assertTrue("2.0", Arrays.asList(repos).contains(repoLocation));
		assertTrue("2.1", metaManager.isEnabled(repoLocation));

		repos = artifactManager.getKnownRepositories(0);
		assertTrue("3.0", Arrays.asList(repos).contains(repoLocation));
		assertTrue("3.1", artifactManager.isEnabled(repoLocation));
	}
}
