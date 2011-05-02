/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
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
	private final List toDelete = new ArrayList();

	public static Test suite() {
		return new TestSuite(MetadataRepositoryManagerExceptionsTest.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		manager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		for (Iterator it = toDelete.iterator(); it.hasNext();)
			delete((File) it.next());
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
