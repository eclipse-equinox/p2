/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.equinox.internal.p2.updatesite.metadata.UpdateSiteMetadataRepositoryFactory;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests for API of {@link IMetadataRepositoryManager}.
 */
public class MetadataRepositoryManagerExceptionsTest extends AbstractProvisioningTest {
	protected IMetadataRepositoryManager manager;
	/**
	 * Contains temp File handles that should be deleted at the end of the test.
	 */
	private final List<File> toDelete = new ArrayList<>();

	public static Test suite() {
		return new TestSuite(MetadataRepositoryManagerExceptionsTest.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		manager = getAgent().getService(IMetadataRepositoryManager.class);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		for (Iterator<File> it = toDelete.iterator(); it.hasNext();)
			delete(it.next());
		toDelete.clear();
	}

	/**
	 * Adds a repository for a non existing site, should
	 * return REPOSITORY_NOT_FOUND, since any other status code gets logged.
	 *
	 * @throws URISyntaxException
	 */
	public void testFailedConnection() throws URISyntaxException {
		//		URI location = new URI("invalid://example");
		URI location = new URI("http://bogus.nowhere");
		MetadataRepositoryFactory factory;

		factory = new SimpleMetadataRepositoryFactory();
		factory.setAgent(getAgent());
		try {
			factory.load(location, 0, new NullProgressMonitor());
		} catch (ProvisionException e) {
			assertEquals(ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		}
		factory = new UpdateSiteMetadataRepositoryFactory();
		try {
			factory.load(location, 0, new NullProgressMonitor());
		} catch (ProvisionException e) {
			assertEquals(ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		}
	}

}
