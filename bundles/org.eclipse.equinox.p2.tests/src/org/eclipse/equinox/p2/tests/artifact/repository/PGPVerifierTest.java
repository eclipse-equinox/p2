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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.repository.DefaultPGPPublicKeyService;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.junit.Before;
import org.junit.Test;

public class PGPVerifierTest extends AbstractProvisioningTest {
	IArtifactRepository targetRepo = null;
	IArtifactRepository sourceRepo = null;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	private void loadPGPTestRepo(String repoName) throws Exception {
		sourceRepo = getArtifactRepositoryManager().loadRepository(
				getTestData("Test repository for PGP", "testData/pgp/" + repoName).toURI(), new NullProgressMonitor());
		targetRepo = createArtifactRepository(Files.createTempDirectory(PGPVerifierTest.class.getSimpleName()).toUri(),
				NO_PROPERTIES);
	}

	@Test
	public void testAllGood() throws Exception {
		IStatus mirrorStatus = performMirrorFrom("repoPGPOK");
		assertOK(mirrorStatus);
	}

	@Test
	public void testAllGoodWithEncodedProperties() throws Exception {
		IStatus mirrorStatus = performMirrorFrom("repoPGPOK_encoded");
		assertOK(mirrorStatus);
	}

	private IStatus performMirrorFrom(String repoName) throws Exception {
		// Clear the remembered keys/cache of the agent.
		IAgentLocation agentLocation = getAgent().getService(IAgentLocation.class);
		Path repositoryCache = Paths
				.get(agentLocation.getDataArea(org.eclipse.equinox.internal.p2.repository.Activator.ID));
		if (Files.isDirectory(repositoryCache)) {
			try (Stream<Path> walk = Files.walk(repositoryCache)) {
				walk.sorted(Comparator.reverseOrder()).forEach(path -> {
					try {
						Files.delete(path);
					} catch (IOException e) {
						// Ignore
					}
				});
			}
		}
		DefaultPGPPublicKeyService keyService = new DefaultPGPPublicKeyService(getAgent());
		keyService.setGPG(false);
		keyService.setKeyServers(Set.of());

		getAgent().registerService(PGPPublicKeyService.SERVICE_NAME, keyService);
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
		assertTrue(mirrorStatus.toString().matches(".*public key.*not be found.*"));
	}

	@Override
	protected void tearDown() throws Exception {
		getArtifactRepositoryManager().removeRepository(sourceRepo.getLocation());
		getArtifactRepositoryManager().removeRepository(targetRepo.getLocation());
		super.tearDown();
	}
}
