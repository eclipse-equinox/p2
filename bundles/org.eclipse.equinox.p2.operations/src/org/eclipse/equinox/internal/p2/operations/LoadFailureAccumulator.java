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
package org.eclipse.equinox.internal.p2.operations;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.operations.RepositoryTracker;

/**
 * Accumulates information about load failures caused by bad locations.
 * <p>
 * This class is designed to support the UI in enabling the user to deal with
 * bad locations, e.g. by disabling or removing them. Therefore, it does not
 * save all informations about the failures.
 */
public final class LoadFailureAccumulator {

	private final RepositoryTracker repositoryTracker;
	private int loadFailuresNotCausedByBadRepoLocation;
	private final List<LoadFailure> loadFailuresCausedByBadRepoLocation;

	public LoadFailureAccumulator(RepositoryTracker repositoryTracker) {
		this.repositoryTracker = repositoryTracker;
		this.loadFailuresCausedByBadRepoLocation = new ArrayList<>();
	}

	public void recordLoadFailure(ProvisionException e, URI location) {
		if (LoadFailure.failureRepresentsBadRepositoryLocation(e)) {
			loadFailuresCausedByBadRepoLocation.add(new LoadFailure(location, e));
			repositoryTracker.addNotFound(location);
		} else {
			loadFailuresNotCausedByBadRepoLocation++;
		}
	}

	public boolean hasSingleFailureCausedByBadLocation() {
		return loadFailuresCausedByBadRepoLocation.size() == 1 && loadFailuresNotCausedByBadRepoLocation == 0;
	}

	public boolean allFailuresCausedByBadLocation() {
		return loadFailuresCausedByBadRepoLocation.size() >= 1 && loadFailuresNotCausedByBadRepoLocation == 0;
	}

	public List<LoadFailure> getLoadFailuresCausedByBadRepoLocation() {
		return loadFailuresCausedByBadRepoLocation;
	}

}
