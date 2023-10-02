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
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.*;

import java.net.URI;
import junit.framework.TestCase;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.operations.LoadFailureAccumulator;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class LoadFailureAccumulatorTest extends TestCase {

	public void testRecordFailureForSingleBadLocation() throws Exception {
		RepositoryTracker repositoryTracker = mock(RepositoryTracker.class);
		LoadFailureAccumulator loadFailureAccumulator = new LoadFailureAccumulator(repositoryTracker);
		ProvisionException exception = buildProvisionExceptionWithCode(ProvisionException.REPOSITORY_INVALID_LOCATION);
		URI invalidLocation = new URI("https://example.com/invalid");
		loadFailureAccumulator.recordLoadFailure(exception, invalidLocation);
		assertThat(loadFailureAccumulator.allFailuresCausedByBadLocation(), is(true));
		assertThat(loadFailureAccumulator.hasSingleFailureCausedByBadLocation(), is(true));
		assertThat(loadFailureAccumulator.getLoadFailuresCausedByBadRepoLocation(), hasSize(1));
		verify(repositoryTracker, times(1)).addNotFound(invalidLocation);
	}

	public void testRecordFailureForMultipleBadLocations() throws Exception {
		RepositoryTracker repositoryTracker = mock(RepositoryTracker.class);
		LoadFailureAccumulator loadFailureAccumulator = new LoadFailureAccumulator(repositoryTracker);
		ProvisionException firstException = buildProvisionExceptionWithCode(
				ProvisionException.REPOSITORY_INVALID_LOCATION);
		URI firstInvalidLocation = new URI("https://example.com/invalid");
		ProvisionException secondException = buildProvisionExceptionWithCode(
				ProvisionException.REPOSITORY_INVALID_LOCATION);
		URI secondInvalidLocation = new URI("https://example.com/invalidTwo");
		loadFailureAccumulator.recordLoadFailure(firstException, firstInvalidLocation);
		loadFailureAccumulator.recordLoadFailure(secondException, secondInvalidLocation);
		assertThat(loadFailureAccumulator.allFailuresCausedByBadLocation(), is(true));
		assertThat(loadFailureAccumulator.hasSingleFailureCausedByBadLocation(), is(false));
		assertThat(loadFailureAccumulator.getLoadFailuresCausedByBadRepoLocation(), hasSize(2));
		InOrder inOrder = Mockito.inOrder(repositoryTracker);
		inOrder.verify(repositoryTracker, times(1)).addNotFound(firstInvalidLocation);
		inOrder.verify(repositoryTracker, times(1)).addNotFound(secondInvalidLocation);
	}

	public void testRecordFailureForMultipleBadLocationsAndOneFailureCausedBySomethingElse() throws Exception {
		RepositoryTracker repositoryTracker = mock(RepositoryTracker.class);
		LoadFailureAccumulator loadFailureAccumulator = new LoadFailureAccumulator(repositoryTracker);
		ProvisionException firstException = buildProvisionExceptionWithCode(
				ProvisionException.REPOSITORY_INVALID_LOCATION);
		URI firstInvalidLocation = new URI("https://example.com/invalid");
		ProvisionException secondException = buildProvisionExceptionWithCode(
				ProvisionException.REPOSITORY_INVALID_LOCATION);
		URI secondInvalidLocation = new URI("https://example.com/invalidTwo");
		ProvisionException thirdException = buildProvisionExceptionWithCode(
				ProvisionException.REPOSITORY_FAILED_AUTHENTICATION);
		URI thirdLocation = new URI("https://example.com/requiresAuthentication");
		loadFailureAccumulator.recordLoadFailure(firstException, firstInvalidLocation);
		loadFailureAccumulator.recordLoadFailure(secondException, secondInvalidLocation);
		loadFailureAccumulator.recordLoadFailure(thirdException, thirdLocation);
		assertThat(loadFailureAccumulator.allFailuresCausedByBadLocation(), is(false));
		assertThat(loadFailureAccumulator.hasSingleFailureCausedByBadLocation(), is(false));
		assertThat(loadFailureAccumulator.getLoadFailuresCausedByBadRepoLocation(), hasSize(2));
		InOrder inOrder = Mockito.inOrder(repositoryTracker);
		inOrder.verify(repositoryTracker, times(1)).addNotFound(firstInvalidLocation);
		inOrder.verify(repositoryTracker, times(1)).addNotFound(secondInvalidLocation);
		verify(repositoryTracker, never()).addNotFound(thirdLocation);
	}

	private ProvisionException buildProvisionExceptionWithCode(int code) {
		IStatus status = new Status(IStatus.ERROR, "pluginId", code, "message", new RuntimeException());
		return new ProvisionException(status);
	}

}
