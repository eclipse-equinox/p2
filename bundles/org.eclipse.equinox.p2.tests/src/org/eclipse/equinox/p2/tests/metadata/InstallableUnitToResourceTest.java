/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.resource.ResourceBuilder;

/**
 * Cross validates {@link IInstallableUnit#toResource()} by publishing a real
 * bundle into an {@link IInstallableUnit} through
 * {@link BundlesAction#createBundleIU(File)}, converting the result back into
 * an OSGi {@link Resource} and comparing the outcome with the {@link Resource}
 * that the bnd {@link ResourceBuilder} computes directly from the very same
 * bundle.
 */
public class InstallableUnitToResourceTest extends AbstractProvisioningTest {

	public void testHostBundleResourceMatchesBnd() throws Exception {
		File bundle = createBundleJar("host", "1.2.3", //
				"Export-Package: com.example.api;version=\"1.2.3\"\n" //
						+ "Import-Package: com.example.spi;version=\"[1.0.0,2.0.0)\"\n" //
						+ "Require-Bundle: some.other.bundle;bundle-version=\"[1.0.0,2.0.0)\";resolution:=optional\n");

		Resource p2Resource = createBundleIU(bundle).toResource();
		Resource bndResource = createBndResource(bundle);

		// osgi.identity
		assertAttributesEqual(bndResource, p2Resource, IdentityNamespace.IDENTITY_NAMESPACE, //
				IdentityNamespace.IDENTITY_NAMESPACE, IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE,
				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);

		// osgi.wiring.bundle (self capability)
		assertAttributesEqual(bndResource, p2Resource, BundleNamespace.BUNDLE_NAMESPACE, //
				BundleNamespace.BUNDLE_NAMESPACE, BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);

		// osgi.wiring.package (Export-Package)
		assertAttributesEqual(bndResource, p2Resource, PackageNamespace.PACKAGE_NAMESPACE, //
				PackageNamespace.PACKAGE_NAMESPACE, PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE,
				PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE,
				PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);

		// osgi.wiring.package (Import-Package) - compare the *effective* filter
		Requirement bndPackageReq = getRequirement(bndResource, PackageNamespace.PACKAGE_NAMESPACE);
		Requirement p2PackageReq = getRequirement(p2Resource, PackageNamespace.PACKAGE_NAMESPACE);
		assertFilterBehaviorEqual(bndPackageReq, p2PackageReq, //
				Map.of(PackageNamespace.PACKAGE_NAMESPACE, "com.example.spi",
						PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version("1.5.0")),
				true);
		assertFilterBehaviorEqual(bndPackageReq, p2PackageReq, //
				Map.of(PackageNamespace.PACKAGE_NAMESPACE, "com.example.spi",
						PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version("2.0.0")),
				false);
		assertEquals(bndPackageReq.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE),
				p2PackageReq.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));

		// osgi.wiring.bundle (Require-Bundle)
		Requirement bndBundleReq = getRequirement(bndResource, BundleNamespace.BUNDLE_NAMESPACE);
		Requirement p2BundleReq = getRequirement(p2Resource, BundleNamespace.BUNDLE_NAMESPACE);
		assertFilterBehaviorEqual(bndBundleReq, p2BundleReq, //
				Map.of(BundleNamespace.BUNDLE_NAMESPACE, "some.other.bundle",
						BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, new Version("1.5.0")),
				true);
		assertFilterBehaviorEqual(bndBundleReq, p2BundleReq, //
				Map.of(BundleNamespace.BUNDLE_NAMESPACE, "some.other.bundle",
						BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, new Version("2.0.0")),
				false);
		assertEquals(Namespace.RESOLUTION_OPTIONAL, bndBundleReq.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));
		assertEquals(Namespace.RESOLUTION_OPTIONAL, p2BundleReq.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));
	}

	public void testFragmentResourceMatchesBnd() throws Exception {
		File bundle = createBundleJar("frag", "1.0.0", //
				"Fragment-Host: host;bundle-version=\"[1.0.0,2.0.0)\"\n");

		Resource p2Resource = createBundleIU(bundle).toResource();
		Resource bndResource = createBndResource(bundle);

		// osgi.identity must report the fragment type
		assertAttributesEqual(bndResource, p2Resource, IdentityNamespace.IDENTITY_NAMESPACE, //
				IdentityNamespace.IDENTITY_NAMESPACE, IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE,
				IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
		assertEquals(IdentityNamespace.TYPE_FRAGMENT, getCapability(p2Resource, IdentityNamespace.IDENTITY_NAMESPACE)
				.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));

		// a fragment does not resolve on its own, so there must not be a
		// osgi.wiring.bundle capability, matching what bnd computes
		assertTrue(bndResource.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE).isEmpty());
		assertTrue(p2Resource.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE).isEmpty());

		// osgi.wiring.host (Fragment-Host)
		Requirement bndHostReq = getRequirement(bndResource, HostNamespace.HOST_NAMESPACE);
		Requirement p2HostReq = getRequirement(p2Resource, HostNamespace.HOST_NAMESPACE);
		assertFilterBehaviorEqual(bndHostReq, p2HostReq, //
				Map.of(HostNamespace.HOST_NAMESPACE, "host",
						HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, new Version("1.5.0")),
				true);
		assertFilterBehaviorEqual(bndHostReq, p2HostReq, //
				Map.of(HostNamespace.HOST_NAMESPACE, "host",
						HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, new Version("2.0.0")),
				false);
	}

	// --- helpers ---------------------------------------------------------

	private File createBundleJar(String bsn, String version, String extraHeaders) throws Exception {
		File dir = Files.createTempDirectory("toResourceTest").toFile();
		dir.deleteOnExit();
		File jar = new File(dir, bsn + "_" + version + ".jar");
		Manifest manifest = new Manifest();
		Attributes attrs = manifest.getMainAttributes();
		attrs.putValue("Manifest-Version", "1.0");
		attrs.putValue("Bundle-ManifestVersion", "2");
		attrs.putValue("Bundle-SymbolicName", bsn);
		attrs.putValue("Bundle-Version", version);
		for (String line : extraHeaders.split("\n")) {
			int idx = line.indexOf(':');
			attrs.putValue(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
		}
		try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar), manifest)) {
			// header only bundle, no classes are required to derive capabilities
		}
		jar.deleteOnExit();
		return jar;
	}

	private IInstallableUnit createBundleIU(File bundle) {
		return BundlesAction.createBundleIU(bundle)
				.orElseThrow(() -> new AssertionError("could not create installable unit for " + bundle));
	}

	private Resource createBndResource(File bundle) throws Exception {
		ResourceBuilder builder = new ResourceBuilder();
		builder.addManifest(Domain.domain(bundle));
		return builder.build();
	}

	private Capability getCapability(Resource resource, String namespace) {
		List<Capability> capabilities = resource.getCapabilities(namespace);
		assertEquals("expected exactly one " + namespace + " capability in " + resource, 1, capabilities.size());
		return capabilities.get(0);
	}

	private Requirement getRequirement(Resource resource, String namespace) {
		List<Requirement> requirements = resource.getRequirements(namespace);
		assertEquals("expected exactly one " + namespace + " requirement in " + resource, 1, requirements.size());
		return requirements.get(0);
	}

	private void assertAttributesEqual(Resource expected, Resource actual, String namespace, String... attributeNames) {
		Map<String, Object> expectedAttributes = getCapability(expected, namespace).getAttributes();
		Map<String, Object> actualAttributes = getCapability(actual, namespace).getAttributes();
		for (String attribute : attributeNames) {
			assertEquals(namespace + '/' + attribute, expectedAttributes.get(attribute), actualAttributes.get(attribute));
		}
	}

	/**
	 * Instead of comparing the (library specific) textual representation of the
	 * generated LDAP filters directly, this validates that both filters behave
	 * identically for a representative capability attribute set. This is more
	 * robust than string comparison as different (but semantically equivalent)
	 * filter strings can be constructed by different implementations.
	 */
	private void assertFilterBehaviorEqual(Requirement expected, Requirement actual,
			Map<String, Object> candidateAttributes, boolean expectedMatch) throws Exception {
		Filter expectedFilter = FrameworkUtil
				.createFilter(expected.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
		Filter actualFilter = FrameworkUtil
				.createFilter(actual.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
		assertEquals("bnd filter " + expectedFilter, expectedMatch, expectedFilter.matches(candidateAttributes));
		assertEquals("p2 filter " + actualFilter, expectedMatch, actualFilter.matches(candidateAttributes));
	}

}
