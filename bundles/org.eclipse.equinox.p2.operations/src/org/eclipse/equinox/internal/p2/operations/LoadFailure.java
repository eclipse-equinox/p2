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
import org.eclipse.equinox.p2.core.ProvisionException;

public class LoadFailure {

	private final URI location;
	private final ProvisionException provisionException;

	public LoadFailure(URI location, ProvisionException provisionException) {
		this.location = location;
		this.provisionException = provisionException;
	}

	public URI getLocation() {
		return location;
	}

	public ProvisionException getProvisionException() {
		return provisionException;
	}

	@Override
	public String toString() {
		return "LoadFailure [location=" + location + ", provisionException=" + provisionException + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public static boolean failureRepresentsBadRepositoryLocation(ProvisionException exception) {
		int code = exception.getStatus().getCode();
		return code == IStatusCodes.INVALID_REPOSITORY_LOCATION
				|| code == ProvisionException.REPOSITORY_INVALID_LOCATION
				|| code == ProvisionException.REPOSITORY_NOT_FOUND;
	}

}
