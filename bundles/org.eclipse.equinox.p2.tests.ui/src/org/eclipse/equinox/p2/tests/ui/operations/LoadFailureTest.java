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

import junit.framework.TestCase;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.operations.IStatusCodes;
import org.eclipse.equinox.internal.p2.operations.LoadFailure;
import org.eclipse.equinox.p2.core.ProvisionException;

public class LoadFailureTest extends TestCase {

	public void testFailureRepresentsBadRepositoryLocationForCodeInvalidRepositoryLocation() throws Exception {
		int code = IStatusCodes.INVALID_REPOSITORY_LOCATION;
		ProvisionException provisionException = buildProvisionExceptionWithCode(code);
		boolean isBadLocation = LoadFailure.failureRepresentsBadRepositoryLocation(provisionException);
		assertTrue(isBadLocation);
	}

	public void testFailureRepresentsBadRepositoryLocationForCodeRepositoryInvalidLocation() throws Exception {
		int code = ProvisionException.REPOSITORY_INVALID_LOCATION;
		ProvisionException provisionException = buildProvisionExceptionWithCode(code);
		boolean isBadLocation = LoadFailure.failureRepresentsBadRepositoryLocation(provisionException);
		assertTrue(isBadLocation);
	}

	public void testFailureRepresentsBadRepositoryLocationForCodeRepositoryNotFound() throws Exception {
		int code = ProvisionException.REPOSITORY_INVALID_LOCATION;
		ProvisionException provisionException = buildProvisionExceptionWithCode(code);
		boolean isBadLocation = LoadFailure.failureRepresentsBadRepositoryLocation(provisionException);
		assertTrue(isBadLocation);
	}

	public void testFailureRepresentsBadRepositoryLocationForOtherCode() throws Exception {
		int code = ProvisionException.REPOSITORY_FAILED_READ;
		ProvisionException provisionException = buildProvisionExceptionWithCode(code);
		boolean isBadLocation = LoadFailure.failureRepresentsBadRepositoryLocation(provisionException);
		assertFalse(isBadLocation);
	}

	private ProvisionException buildProvisionExceptionWithCode(int code) {
		IStatus status = new Status(IStatus.ERROR, "pluginId", code, "message", new RuntimeException());
		return new ProvisionException(status);
	}

}
