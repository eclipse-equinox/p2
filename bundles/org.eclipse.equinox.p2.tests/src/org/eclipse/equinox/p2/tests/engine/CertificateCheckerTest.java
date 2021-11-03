/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.engine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.cert.Certificate;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.processors.pgp.PGPSignatureVerifier;
import org.eclipse.equinox.internal.p2.core.AgentLocation;
import org.eclipse.equinox.internal.p2.core.ProvisioningAgent;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.p2.engine.phases.CertificateChecker;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.UIServices;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.TestData;

/**
 * Tests for {@link CertificateChecker}.
 */
public class CertificateCheckerTest extends AbstractProvisioningTest {
	private static final String PGP_PUBLIC_KEY = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" + "\n"
			+ "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\n" + "=JYpC\n"
			+ "-----END PGP PUBLIC KEY BLOCK-----\n";
	private static final String PGP_SIGNATURE = "-----BEGIN PGP SIGNATURE-----\n" + "\n"
			+ "iQHRBAABCAA7FiEE6ZbGcKp/ZUCb9dtWE5442Q3tEfAFAmB4Bf8dHHNpZ25lcjFA\n"
			+ "ZmFrZXVzZXIuZWNsaXBzZS5vcmcACgkQE5442Q3tEfBPuAwAhE4zA7BswKFhEtzm\n"
			+ "DS3EbyRr/U13sV01YxqGtxYDCfrOt8TGVPXJSvo0AVP4vLFc5b+0jtVFoarFJNBu\n"
			+ "xhbVuyC72YdqudNbncSlW6KG5SgeWIM//ThKfl95pOWRWvJEoKJhmDwbDqhZYtL5\n"
			+ "SECegnWGtGx/klFtZihzHYJE/nfSnhySDaz5lCvXFFYRhIbNmNm2Yq7ztCOsN4Ys\n"
			+ "3uN+GEdoXGvv1DFg/xZPvwCOhZGsSQfkl1jmUwVltgKrw9OCFbdfYz7H6dbGWhRu\n"
			+ "2XpuKxPXGavKfpSvssVQIZ6aWi5W6wp5lZAQQddZvYAv3Gi5CZZcUT7ayFJYdD23\n"
			+ "p9jz76G7MXm0f0uNT9B57T72QryokUIEIJYsCb6lNjWUQB4cd0+JesM7sHwweOQ3\n"
			+ "7iTFc+WgVJkP0e695mm1tcvtQHUPbIItYJUsndyLgGInzglxN8+F4U4k8uapydI9\n"
			+ "RmV2NVAifYp8z95Am5AnlG8lqjwrWk5bMbJH82QsQESrNT/h\n" + "=8Vrn\n"
			+ "-----END PGP SIGNATURE-----\n";

	class CertificateTestService extends UIServices {
		public boolean unsignedReturnValue = true;
		public boolean wasPrompted = false;

		@Override
		public AuthenticationInfo getUsernamePassword(String location) {
			return null;
		}

		@Override
		public AuthenticationInfo getUsernamePassword(String location, AuthenticationInfo previousInfo) {
			return null;
		}

		@Override
		public TrustInfo getTrustInfo(Certificate[][] untrustedChain, String[] unsignedDetail) {
			wasPrompted = true;
			return new TrustInfo(null, false, unsignedReturnValue);
		}

	}

	CertificateChecker checker;
	CertificateTestService serviceUI;
	File unsigned;
	private ProvisioningAgent testAgent;

	@Override
	protected void setUp() throws Exception {
		serviceUI = new CertificateTestService();
		testAgent = new ProvisioningAgent();
		testAgent.registerService(UIServices.SERVICE_NAME, serviceUI);
		testAgent.setBundleContext(TestActivator.getContext());
		checker = new CertificateChecker(testAgent);
		try {
			unsigned = TestData.getFile("CertificateChecker", "unsigned.jar");
		} catch (IOException e) {
			fail("0.99", e);
		}
		assertNotNull("1.0", unsigned);
		assertTrue("1.0", unsigned.exists());
	}

	/**
	 * Tests that installing unsigned content is not allowed when the policy says it must fail.
	 */
	public void testPolicyAllow() {
		try {
			//if the service is consulted it will say no
			serviceUI.unsignedReturnValue = false;
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_ALLOW);
			checker.add(Map.of(new ArtifactDescriptor(new ArtifactKey("what", "ever", Version.create("1"))), unsigned));
			IStatus result = checker.start();
			assertEquals("1.0", IStatus.OK, result.getSeverity());
		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}

	/**
	 * Tests that installing unsigned content is not allowed when the policy says it must fail.
	 */
	public void testPolicyFail() {
		try {
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_FAIL);
			checker.add(Map.of(new ArtifactDescriptor(new ArtifactKey("what", "ever", Version.create("1"))), unsigned));
			IStatus result = checker.start();
			assertEquals("1.0", IStatus.ERROR, result.getSeverity());

		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}

	/**
	 * Tests that installing unsigned content with the "prompt" policy and the prompt succeeds.
	 */
	public void testPolicyPromptSuccess() {
		try {
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_PROMPT);
			serviceUI.unsignedReturnValue = true;
			checker.add(Map.of(new ArtifactDescriptor(new ArtifactKey("what", "ever", Version.create("1"))), unsigned));
			IStatus result = checker.start();
			assertEquals("1.0", IStatus.OK, result.getSeverity());
			assertTrue("1.1", serviceUI.wasPrompted);
		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}

	/**
	 * Tests that the default policy for unsigned content is to prompt.
	 */
	public void testPolicyDefault() {
		System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		serviceUI.unsignedReturnValue = true;
		checker.add(Map.of(new ArtifactDescriptor(new ArtifactKey("what", "ever", Version.create("1"))), unsigned));
		IStatus result = checker.start();
		assertEquals("1.0", IStatus.OK, result.getSeverity());
		assertTrue("1.1", serviceUI.wasPrompted);
	}

	/**
	 * Tests that installing unsigned content with the "prompt" policy and the prompt says no.
	 */
	public void testPolicyPromptCancel() {
		try {
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_PROMPT);
			serviceUI.unsignedReturnValue = false;
			checker.add(Map.of(new ArtifactDescriptor(new ArtifactKey("what", "ever", Version.create("1"))), unsigned));
			IStatus result = checker.start();
			assertEquals("1.0", IStatus.CANCEL, result.getSeverity());
			assertTrue("1.1", serviceUI.wasPrompted);
		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}

	/**
	 * Tests that trust checks that occur in a headless environment are properly treated
	 * as permissive, but not persistent, the same way as it would be if the service registration
	 * were not there.
	 */
	public void testBug291049() {
		try {
			// Intentionally replace our service with a null service
			testAgent.registerService(UIServices.SERVICE_NAME, null);
			checker.add(Map.of(new ArtifactDescriptor(new ArtifactKey("what", "ever", Version.create("1"))), unsigned));
			// TODO need to add some untrusted files here, too.  To prove that we treated them as trusted temporarily
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_PROMPT);
			IStatus result = checker.start();
			assertTrue("1.0", result.isOK());
		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}

	public void testPGPSignedArtifactUntrustedKey() throws IOException, ProvisionException {
		try {
			// create a test profile
			testAgent.registerService("FORCED_SELF", IProfileRegistry.SELF);
			testAgent.registerService(IAgentLocation.SERVICE_NAME,
					new AgentLocation(Files.createTempDirectory(
							CertificateCheckerTest.class.getName() + "testPGPSignedArtifactUntrustedKey-profile")
							.toUri()));
			testAgent.getService(IProfileRegistry.class).addProfile(IProfileRegistry.SELF);

			unsigned = TestData.getFile("pgp/repoPGPOK/plugins", "blah_1.0.0.123456.jar");
			ArtifactDescriptor artifactDescriptor = new ArtifactDescriptor(
					new ArtifactKey("what", "ever", Version.create("1")));
			artifactDescriptor.addProperties(
					Map.of(PGPSignatureVerifier.PGP_SIGNATURES_PROPERTY_NAME, PGP_SIGNATURE, //
							PGPSignatureVerifier.PGP_SIGNER_KEYS_PROPERTY_NAME,
							PGP_PUBLIC_KEY));
			checker.add(Map.of(artifactDescriptor, unsigned));
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_PROMPT);
			IStatus result = checker.start();
			assertFalse(result.isOK());
			assertTrue(serviceUI.wasPrompted);
		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}

	public void testPGPSignedArtifactTrustedKeyInProfile() throws ProvisionException, IOException {
		try {
			// create a test profile
			testAgent.registerService("FORCED_SELF", IProfileRegistry.SELF);
			testAgent.registerService(IAgentLocation.SERVICE_NAME, new AgentLocation(
					Files.createTempDirectory(
							CertificateCheckerTest.class.getName() + "testPGPSignedArtifactTrustedKey-profile")
							.toUri()));
			testAgent.getService(IProfileRegistry.class).addProfile(IProfileRegistry.SELF,
					Map.of(CertificateChecker.TRUSTED_KEY_STORE_PROPERTY, PGP_PUBLIC_KEY));

			unsigned = TestData.getFile("pgp/repoPGPOK/plugins", "blah_1.0.0.123456.jar");
			ArtifactDescriptor artifactDescriptor = new ArtifactDescriptor(
					new ArtifactKey("what", "ever", Version.create("1")));
			artifactDescriptor.addProperties(
					Map.of(PGPSignatureVerifier.PGP_SIGNATURES_PROPERTY_NAME, PGP_SIGNATURE));
			checker.add(Map.of(artifactDescriptor, unsigned));
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_PROMPT);
			IStatus result = checker.start();
			assertTrue(result.isOK());
			assertFalse(serviceUI.wasPrompted);
		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}

	//// SECURITY ISSUE: next lines become an attack vector as we have no guarantee
	//// the metadata of those IUs is safe/were signed.
	//// https://bugs.eclipse.org/bugs/show_bug.cgi?id=576705#c4
//	public void testPGPSignedArtifactTrustedKeyInInstalledIU() throws ProvisionException, IOException {
//		try {
//			// create a test profile
//			testAgent.registerService("FORCED_SELF", IProfileRegistry.SELF);
//			testAgent
//					.registerService(IAgentLocation.SERVICE_NAME,
//							new AgentLocation(Files.createTempDirectory(
//									CertificateCheckerTest.class.getName() + "testPGPSignedArtifactTrustedKey-profile")
//									.toUri()));
//			IProfile testProfile = testAgent.getService(IProfileRegistry.class).addProfile(IProfileRegistry.SELF);
//			// install an IU that declares trusted keys
//			InstallableUnitDescription desc = new InstallableUnitDescription();
//			desc.setProperty(CertificateChecker.TRUSTED_KEY_STORE_PROPERTY, PGP_PUBLIC_KEY);
//			desc.setId("unitWithTrustedKeys");
//			desc.setVersion(Version.create("1.0.0"));
//			IEngine engine = testAgent.getService(IEngine.class);
//			IProvisioningPlan plan = engine.createPlan(testProfile, new ProvisioningContext(testAgent));
//			plan.addInstallableUnit(MetadataFactory.createInstallableUnit(desc));
//			assertTrue(engine.perform(plan, getMonitor()).isOK());
//
//			unsigned = TestData.getFile("pgp/repoPGPOK/plugins", "blah_1.0.0.123456.jar");
//			ArtifactDescriptor artifactDescriptor = new ArtifactDescriptor(
//					new ArtifactKey("what", "ever", Version.create("1")));
//			artifactDescriptor.addProperties(Map.of(PGPSignatureVerifier.PGP_SIGNATURES_PROPERTY_NAME, PGP_SIGNATURE));
//			checker.add(Map.of(artifactDescriptor, unsigned));
//			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_PROMPT);
//			IStatus result = checker.start();
//			assertTrue(result.isOK());
//			assertFalse(serviceUI.wasPrompted);
//		} finally {
//			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
//		}
//	}
}
