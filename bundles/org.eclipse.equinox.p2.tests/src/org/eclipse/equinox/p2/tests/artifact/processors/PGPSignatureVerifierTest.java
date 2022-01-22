/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Set;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.processors.pgp.PGPSignatureVerifier;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.repository.DefaultPGPPublicKeyService;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;
import org.eclipse.equinox.p2.tests.TestAgentProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PGPSignatureVerifierTest {

	@Rule
	public TestAgentProvider agentProvider = new TestAgentProvider();

	@Before
	public void initialize() {
		try {
			PGPPublicKeyService keyService = agentProvider.getService(PGPPublicKeyService.class);
			if (keyService instanceof DefaultPGPPublicKeyService) {
				DefaultPGPPublicKeyService defaultPGPPublicKeyService = (DefaultPGPPublicKeyService) keyService;
				defaultPGPPublicKeyService.setKeyServers(Set.of());
				defaultPGPPublicKeyService.setGPG(false);
			}
		} catch (ProvisionException e) {
			//$FALL-THROUGH$
		}
	}

	// @formatter:off
	/*
	 * About test keys: * Install the public&private keys locally * then generate
	 * signatures with eg `gpg -u signer2@fakeuser.eclipse.org -a --output
	 * signed_by_signer_2 --detach-sig testArtifact`
	 */
	// @formatter:on

	private IArtifactDescriptor createArtifact(String signaturesResourcePath, String publicKeyResourcePath)
			throws IOException, URISyntaxException {
		ArtifactDescriptor res = new ArtifactDescriptor(
				new ArtifactKey("whatever", "whatever", Version.parseVersion("1.0.0")));
		res.setProperty(PGPSignatureVerifier.PGP_SIGNATURES_PROPERTY_NAME, read(signaturesResourcePath));
		res.setProperty(PGPSignatureVerifier.PGP_SIGNER_KEYS_PROPERTY_NAME, read(publicKeyResourcePath));
		return res;
	}

	private String read(String resource) throws IOException, URISyntaxException {
		return Files.readString(new File(FileLocator.toFileURL(getClass().getResource(resource)).toURI()).toPath());
	}

	@Test
	public void testOK() throws Exception {
		IProcessingStepDescriptor processingStepDescriptor = new ProcessingStepDescriptor(null, null, false);
		IArtifactDescriptor artifact = createArtifact("signed_by_signer_1", "public_signer1.pgp");
		@SuppressWarnings("resource")
		PGPSignatureVerifier verifier = new PGPSignatureVerifier();
		verifier.initialize(agentProvider.getAgent(), processingStepDescriptor, artifact);
		Assert.assertTrue(verifier.getStatus().toString(), verifier.getStatus().isOK());
		try (InputStream bytes = getClass().getResourceAsStream("testArtifact")) {
			bytes.transferTo(verifier);
		}
		Assert.assertTrue(verifier.getStatus().isOK());
		verifier.close();
		Assert.assertTrue(verifier.getStatus().isOK());
	}

	@Test
	public void testNoPublicKeyFound() throws Exception {
		IProcessingStepDescriptor processingStepDescriptor = new ProcessingStepDescriptor(null, null, false);
		IArtifactDescriptor artifact = createArtifact("signed_by_signer_1", "public_signer2.pgp");
		try (PGPSignatureVerifier verifier = new PGPSignatureVerifier()) {
			verifier.initialize(agentProvider.getAgent(), processingStepDescriptor, artifact);
			IStatus status = verifier.getStatus();
			assertEquals(IStatus.ERROR, status.getSeverity());
			assertTrue(status.getMessage().matches("A public key.*could not be found.*"));
		}
	}

	@Test
	public void testTamperedSignature() throws Exception {
		IProcessingStepDescriptor processingStepDescriptor = new ProcessingStepDescriptor(null, null, false);
		IArtifactDescriptor artifact = createArtifact("signed_by_signer_1_tampered", "public_signer1.pgp");
		try (PGPSignatureVerifier verifier = new PGPSignatureVerifier()) {
			verifier.initialize(agentProvider.getAgent(), processingStepDescriptor, artifact);
			// signature has random modification, making it invalid by itself
			Assert.assertFalse(verifier.getStatus().isOK());
		}
	}

	@Test
	public void testSignatureForAnotherArtifact() throws Exception {
		IProcessingStepDescriptor processingStepDescriptor = new ProcessingStepDescriptor(null, null, false);
		IArtifactDescriptor artifact = createArtifact("signed_by_signer_1_otherArtifact", "public_signer1.pgp");
		@SuppressWarnings("resource")
		PGPSignatureVerifier verifier = new PGPSignatureVerifier();
		verifier.initialize(agentProvider.getAgent(), processingStepDescriptor, artifact);
		Assert.assertTrue(verifier.getStatus().isOK());
		try (InputStream bytes = getClass().getResourceAsStream("testArtifact")) {
			bytes.transferTo(verifier);
		}
		Assert.assertTrue(verifier.getStatus().isOK());
		verifier.close();
		IStatus status = verifier.getStatus();
		assertEquals(IStatus.ERROR, status.getSeverity());
		assertTrue(status.getMessage().matches(".*signature.*invalid.*"));
	}
}
