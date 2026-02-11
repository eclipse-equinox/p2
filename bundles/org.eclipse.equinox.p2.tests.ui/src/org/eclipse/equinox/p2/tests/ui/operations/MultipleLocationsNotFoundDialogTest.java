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

import java.io.IOException;
import java.net.URI;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

public class MultipleLocationsNotFoundDialogTest extends AbstractProvisioningTest {

	public void testRemoveRepositories() throws IOException {
		// Set up a composite repo. This was copied from
		// ColocatedRepositoryTrackerTest.
		final String compositeRepo = "testData/bug338495/good.local";
		final URI compositeRepoURI = getTestData("composite repo", compositeRepo).toURI();

		ProvisioningUI provUI = mock(ProvisioningUI.class);
		ProvisioningSession provisioningSession = mock(ProvisioningSession.class);
		when(provUI.getSession()).thenReturn(provisioningSession);
		ColocatedRepositoryTracker tracker = mock(ColocatedRepositoryTracker.class);
		URI firstLocation = compositeRepoURI;
		URI secondLocation = compositeRepoURI.resolve("/test2");
		URI[] locations = new URI[] { firstLocation, secondLocation };
		MultipleLocationsNotFoundDialog dialog = new MultipleLocationsNotFoundDialog(tracker, provUI, locations);
		dialog.removeRepositories();
		verify(tracker, times(1)).removeRepositories(locations, provisioningSession);
	}

	public void testDisableRepositories() throws IOException {
		// Set up a composite repo. This was adapted from
		// ColocatedRepositoryTrackerTest.
		final String compositeRepo = "testData/bug338495/good.local";
		final URI compositeRepoURI = getTestData("composite repo", compositeRepo).toURI();
		final String childRepo = "testData/bug338495/good.local/one";
		final URI childRepoOneURI = getTestData("composite repo", childRepo).toURI();
		final String childRepoTwo = "testData/bug338495/good.local/two";
		final URI childRepoTwoURI = getTestData("composite repo", childRepoTwo).toURI();

		ProvisioningUI provUI = ProvisioningUI.getDefaultUI();
		ProvisioningSession provSession = provUI.getSession();

		ColocatedRepositoryTracker tracker = new ColocatedRepositoryTracker(provUI);
		tracker.addRepository(compositeRepoURI, "main", provSession);
		tracker.addRepository(childRepoOneURI, "child", provSession);
		tracker.addRepository(childRepoTwoURI, "child2", provSession);

		URI[] locations = new URI[] { childRepoOneURI, childRepoTwoURI };
		MultipleLocationsNotFoundDialog dialog = new MultipleLocationsNotFoundDialog(tracker, provUI, locations);
		dialog.disableRepositories();
		assertThat(ProvUI.getMetadataRepositoryManager(provSession).isEnabled(childRepoOneURI), is(false));
		assertThat(ProvUI.getArtifactRepositoryManager(provSession).isEnabled(childRepoOneURI), is(false));
		assertThat(ProvUI.getMetadataRepositoryManager(provSession).isEnabled(childRepoTwoURI), is(false));
		assertThat(ProvUI.getArtifactRepositoryManager(provSession).isEnabled(childRepoTwoURI), is(false));
	}

}
