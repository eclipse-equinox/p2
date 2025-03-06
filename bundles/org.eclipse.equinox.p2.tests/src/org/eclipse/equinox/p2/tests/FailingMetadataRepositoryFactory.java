/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory;

/**
 * A repository factory that always throws exceptions. The "fail" flag must be set to
 * cause this factory to start throwing exceptions.
 */
public class FailingMetadataRepositoryFactory extends MetadataRepositoryFactory {
	public static boolean FAIL = false;

	@Override
	public IMetadataRepository create(URI location, String name, String type, Map<String, String> properties) throws ProvisionException {
		if (FAIL) {
			throw new RuntimeException("Exception thrown deliberately as part of test");
		}
		throw new ProvisionException(new Status(IStatus.ERROR, TestActivator.PI_PROV_TESTS, ProvisionException.REPOSITORY_NOT_FOUND, "", null));
	}

	@Override
	public IMetadataRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
		if (FAIL) {
			throw new RuntimeException("Exception thrown deliberately as part of test");
		}
		throw new ProvisionException(new Status(IStatus.ERROR, TestActivator.PI_PROV_TESTS, ProvisionException.REPOSITORY_NOT_FOUND, "", null));
	}

}
