/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Iterator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class MD5Tests extends AbstractProvisioningTest {
	File testRepo = null;
	IArtifactRepository repo = null;

	protected void setUp() throws Exception {
		super.setUp();
		testRepo = getTestData("Repository with MD5", "testData/artifactRepo/simpleWithMD5");
		repo = getArtifactRepositoryManager().loadRepository(testRepo.toURI(), new NullProgressMonitor());
		assertNotNull("1.0", repo);
	}

	public void DISABLE_testCheckMD5() {
		IQueryResult<IArtifactDescriptor> descriptors = repo.descriptorQueryable().query(ArtifactDescriptorQuery.ALL_DESCRIPTORS, null);
		for (Iterator<IArtifactDescriptor> iterator = descriptors.iterator(); iterator.hasNext();) {
			IArtifactDescriptor desc = iterator.next();
			IStatus status = repo.getArtifact(desc, new ByteArrayOutputStream(500), new NullProgressMonitor());
			//All artifacts that are expected to fail MD5 check are those whose id starts with bogus
			if (desc.getArtifactKey().getId().startsWith("bogus")) {
				assertNotOK(status);
				continue;
			}
			assertOK("2.1 " + desc, status);
		}
	}

	public void testBug249035_ArtifactIdentity() {
		//MD5 sum should not affect the identity of the artifact

		ArtifactDescriptor descriptor = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "aaPlugin", Version.create("1.0.0")));
		descriptor.setProperty(IArtifactDescriptor.DOWNLOAD_MD5, "42");

		try {
			repo.getOutputStream(descriptor);
			fail("3.1 - Expected Artifact exists exception did not occur.");
		} catch (ProvisionException e) {
			assertTrue("3.2", e.getMessage().contains("The artifact is already available in the repository"));
		}
	}

	protected void tearDown() throws Exception {
		getArtifactRepositoryManager().removeRepository(testRepo.toURI());
		super.tearDown();
	}
}
