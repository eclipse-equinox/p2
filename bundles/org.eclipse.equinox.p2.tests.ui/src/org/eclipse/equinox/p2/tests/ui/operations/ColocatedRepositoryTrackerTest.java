/*******************************************************************************
 *  Copyright (c) 2011 Sonatype, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *    Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.operations;

import java.net.URI;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.internal.p2.ui.ColocatedRepositoryTracker;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

public class ColocatedRepositoryTrackerTest extends AbstractProvisioningTest {
	public void testAdditionOfChildren() throws ProvisionException, OperationCanceledException {
		final String compositeRepo = "testData/bug338495/good.local";
		final URI compositeRepoURI = getTestData("composite repo", compositeRepo).toURI();
		final String childRepo = "testData/bug338495/good.local/one";
		final URI childRepoOneURI = getTestData("composite repo", childRepo).toURI();

		ProvisioningUI provUI = ProvisioningUI.getDefaultUI();
		ProvisioningSession provSession = provUI.getSession();

		ColocatedRepositoryTracker tracker = new ColocatedRepositoryTracker(provUI);
		tracker.addRepository(compositeRepoURI, "main", provSession);
		getMetadataRepositoryManager().loadRepository(compositeRepoURI, new NullProgressMonitor()); //Force the loading the composite repo to show the problem
		assertOK(tracker.validateRepositoryLocation(ProvisioningUI.getDefaultUI().getSession(), childRepoOneURI, false, new NullProgressMonitor()));
		tracker.addRepository(childRepoOneURI, "child", provSession);

		assertTrue(getMetadataRepositoryManager().isEnabled(childRepoOneURI));
		assertEquals(Boolean.FALSE.toString(), getMetadataRepositoryManager().getRepositoryProperty(childRepoOneURI, IRepository.PROP_SYSTEM));
	}
}
