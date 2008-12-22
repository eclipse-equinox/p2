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
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.AddColocatedRepositoryOperation;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

/**
 * Tests for {@link AddColocatedRepositoryOperation}.
 */
public class AddColocatedRepositoryOperationTest extends AbstractProvisioningUITest {
	public void testAddSingleRepository() {
		URI repoLocation = null;
		try {
			repoLocation = TestData.getFile("artifactRepo", "").toURI();
		} catch (Exception e) {
			fail("0.99", e);
		}
		AddColocatedRepositoryOperation op = new AddColocatedRepositoryOperation("label", repoLocation);
		assertTrue("1.0", op.runInBackground());

		try {
			IStatus result = op.execute(getMonitor());
			assertTrue("1.1", result.isOK());
		} catch (ProvisionException e) {
			fail("1.99", e);
		}

		URI[] repos = metaManager.getKnownRepositories(0);
		assertTrue("2.0", Arrays.asList(repos).contains(repoLocation));
		assertTrue("2.1", metaManager.isEnabled(repoLocation));

		repos = artifactManager.getKnownRepositories(0);
		assertTrue("3.0", Arrays.asList(repos).contains(repoLocation));
		assertTrue("3.1", artifactManager.isEnabled(repoLocation));
	}
}
