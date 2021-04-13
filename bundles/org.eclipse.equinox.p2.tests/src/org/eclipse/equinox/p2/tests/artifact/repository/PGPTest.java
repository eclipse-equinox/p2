/*******************************************************************************
 *  Copyright (c) 2021 Red Hat Inc. and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.nio.file.Files;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.processors.pgp.PGPSignatureVerifier;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.junit.Before;
import org.junit.Test;

public class PGPTest extends AbstractProvisioningTest {
	IArtifactRepository targetRepo = null;
	IArtifactRepository sourceRepo = null;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		PGPSignatureVerifier.discardKnownKeys();
	}

	private void loadPGPTestRepo(String repoName) throws Exception {
		sourceRepo = getArtifactRepositoryManager().loadRepository(
				getTestData("Test repository for PGP", "testData/pgp/" + repoName).toURI(), new NullProgressMonitor());
		targetRepo = createArtifactRepository(Files.createTempDirectory(PGPTest.class.getSimpleName()).toUri(),
				NO_PROPERTIES);
	}

	@Test
	public void testAllGood() throws Exception {
		IStatus mirrorStatus = performMirrorFrom("repoPGPOK");
		assertOK(mirrorStatus);
	}

	private IStatus performMirrorFrom(String repoName) throws Exception {
		loadPGPTestRepo(repoName);
		ArtifactKey key = new ArtifactKey("osgi.bundle", "blah", Version.create("1.0.0.123456"));
		MirrorRequest mirrorRequest = new MirrorRequest(key, targetRepo, NO_PROPERTIES, NO_PROPERTIES, getTransport());
		mirrorRequest.perform(sourceRepo, getMonitor());
		return mirrorRequest.getResult();
	}

	@Test
	public void testMissingPublicKey() throws Exception {
		IStatus mirrorStatus = performMirrorFrom("repoMissingPublicKey");
		assertNotOK(mirrorStatus);
		assertTrue(mirrorStatus.toString().contains("Public key not found"));
	}

	@Override
	protected void tearDown() throws Exception {
		getArtifactRepositoryManager().removeRepository(sourceRepo.getLocation());
		getArtifactRepositoryManager().removeRepository(targetRepo.getLocation());
		super.tearDown();
	}
}
