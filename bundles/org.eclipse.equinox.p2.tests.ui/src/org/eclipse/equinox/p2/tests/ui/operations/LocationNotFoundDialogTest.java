/*******************************************************************************
 *  Copyright (c) 2023 Erik Brangs.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Erik Brangs - initial implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.operations;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import java.net.URI;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class LocationNotFoundDialogTest extends AbstractProvisioningTest {

	public void testCorrectLocation() {
		// Set up a composite repo. This was copied from
		// ColocatedRepositoryTrackerTest.
		final String compositeRepo = "testData/bug338495/good.local";
		final URI compositeRepoURI = getTestData("composite repo", compositeRepo).toURI();
		final String childRepo = "testData/bug338495/good.local/one";
		final URI childRepoOneURI = getTestData("composite repo", childRepo).toURI();

		ProvisioningUI provUI = mock(ProvisioningUI.class);
		ProvisioningSession provisioningSession = mock(ProvisioningSession.class);
		when(provUI.getSession()).thenReturn(provisioningSession);
		ColocatedRepositoryTracker tracker = mock(ColocatedRepositoryTracker.class);
		URI location = compositeRepoURI;
		LocationNotFoundDialog dialog = new LocationNotFoundDialog(tracker, provUI, location);
		URI correctedLocation = childRepoOneURI;
		String repositoryName = "repositoryName";
		InOrder inOrder = Mockito.inOrder(tracker, provUI);
		dialog.correctLocation(correctedLocation, repositoryName);
		inOrder.verify(provUI, times(1)).signalRepositoryOperationStart();
		inOrder.verify(tracker, times(1)).removeRepositories(new URI[] { location }, provisioningSession);
		inOrder.verify(tracker, times(1)).addRepository(correctedLocation, repositoryName, provisioningSession);
		inOrder.verify(provUI, times(1)).signalRepositoryOperationComplete(null, true);
	}

	public void testRemoveRepository() {
		// Set up a composite repo. This was copied from
		// ColocatedRepositoryTrackerTest.
		final String compositeRepo = "testData/bug338495/good.local";
		final URI compositeRepoURI = getTestData("composite repo", compositeRepo).toURI();

		ProvisioningUI provUI = mock(ProvisioningUI.class);
		ProvisioningSession provisioningSession = mock(ProvisioningSession.class);
		when(provUI.getSession()).thenReturn(provisioningSession);
		ColocatedRepositoryTracker tracker = mock(ColocatedRepositoryTracker.class);
		URI location = compositeRepoURI;
		LocationNotFoundDialog dialog = new LocationNotFoundDialog(tracker, provUI, location);
		dialog.removeRepository();
		verify(tracker, times(1)).removeRepositories(new URI[] { location }, provisioningSession);
	}

	public void testDisableRepository() {
		// Set up a composite repo. This was adapted from
		// ColocatedRepositoryTrackerTest.
		final String compositeRepo = "testData/bug338495/good.local";
		final URI compositeRepoURI = getTestData("composite repo", compositeRepo).toURI();
		final String childRepo = "testData/bug338495/good.local/one";
		final URI childRepoOneURI = getTestData("composite repo", childRepo).toURI();

		ProvisioningUI provUI = ProvisioningUI.getDefaultUI();
		ProvisioningSession provSession = provUI.getSession();

		ColocatedRepositoryTracker tracker = new ColocatedRepositoryTracker(provUI);
		tracker.addRepository(compositeRepoURI, "main", provSession);
		tracker.addRepository(childRepoOneURI, "child", provSession);

		URI location = compositeRepoURI;
		LocationNotFoundDialog dialog = new LocationNotFoundDialog(tracker, provUI, location);
		dialog.disableRepository();
		assertThat(ProvUI.getMetadataRepositoryManager(provSession).isEnabled(location), is(false));
		assertThat(ProvUI.getArtifactRepositoryManager(provSession).isEnabled(location), is(false));
	}

}
