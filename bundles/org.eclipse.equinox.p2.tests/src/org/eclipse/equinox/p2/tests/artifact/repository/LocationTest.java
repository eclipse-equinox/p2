/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class LocationTest extends AbstractProvisioningTest {
	private static final String testDataLocation = "testData/artifactRepo/packedSiblingsWithUUID";
	private File targetLocation;
	private IArtifactRepository targetRepository, sourceRepository;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		targetLocation = File.createTempFile("target", ".repo");
		targetLocation.delete();
		targetLocation.mkdirs();
		targetRepository = new SimpleArtifactRepository(getAgent(), "TargetRepo", targetLocation.toURI(), null);

		IArtifactRepositoryManager mgr = getArtifactRepositoryManager();
		sourceRepository = mgr.loadRepository((getTestData("EmptyJar repo", testDataLocation).toURI()), null);

	}

	@Override
	protected void tearDown() throws Exception {
		getArtifactRepositoryManager().removeRepository(targetLocation.toURI());
		getArtifactRepositoryManager().removeRepository(sourceRepository.getLocation());
		AbstractProvisioningTest.delete(targetLocation);
		super.tearDown();
	}

	public void testLocation() throws Exception {
		IArtifactKey key = new ArtifactKey("osgi.bundle", "org.springframework.ide.eclipse", Version.parseVersion("2.3.2.201003220227-RELEASE"));
		assertTrue(sourceRepository.contains(key));
		MirrorRequest req = new MirrorRequest(key, targetRepository, null, null, getTransport());
		req.perform(sourceRepository, new NullProgressMonitor());
		IStatus status = req.getResult();
		assertTrue(status.getMessage(), status.isOK());
	}
}
