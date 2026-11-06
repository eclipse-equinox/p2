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
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.osgi.namespace.FeatureNamespace;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * Validates {@link IInstallableUnit#toResource()} for p2 <b>feature</b> IUs,
 * that is IUs published from a {@code feature.xml} by
 * {@link FeaturesAction}. As there is no independent OSGi tool (like bnd) that
 * models p2 features, this asserts the shape of the resulting {@link Resource}
 * directly instead of cross validating against a second implementation.
 */
public class InstallableUnitFeatureToResourceTest extends AbstractProvisioningTest {

	public void testFeatureResource() throws Exception {
		IInstallableUnit featureIU = createFeatureIU("test.feature", "1.0.0", //
				"<import plugin=\"org.plug\" version=\"[1.0.0,2.0.0)\" match=\"versionRange\"/>\n" //
						+ "<import feature=\"org.foo\" version=\"[1.0.0,2.0.0)\" match=\"versionRange\"/>\n");

		Resource resource = featureIU.toResource();

		// osgi.identity must report the feature type
		Capability identity = getCapability(resource, IdentityNamespace.IDENTITY_NAMESPACE);
		assertEquals("test.feature", identity.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
		assertEquals(FeatureNamespace.TYPE_FEATURE,
				identity.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));

		// a feature does not resolve as a bundle, so there must not be a
		// osgi.wiring.bundle capability
		assertTrue(resource.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE).isEmpty());

		// osgi.wiring.feature (self capability)
		Capability featureCapability = getCapability(resource, FeatureNamespace.FEATURE_NAMESPACE);
		assertEquals("test.feature", featureCapability.getAttributes().get(FeatureNamespace.FEATURE_NAMESPACE));
		assertEquals(new Version("1.0.0"),
				featureCapability.getAttributes().get(FeatureNamespace.CAPABILITY_VERSION_ATTRIBUTE));

		// osgi.wiring.bundle requirement (plugin inclusion)
		Requirement bundleRequirement = getRequirement(resource, BundleNamespace.BUNDLE_NAMESPACE);
		assertFilterMatches(bundleRequirement, //
				Map.of(BundleNamespace.BUNDLE_NAMESPACE, "org.plug",
						BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, new Version("1.5.0")),
				true);
		assertFilterMatches(bundleRequirement, //
				Map.of(BundleNamespace.BUNDLE_NAMESPACE, "org.plug",
						BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, new Version("2.0.0")),
				false);

		// osgi.wiring.feature requirement (nested feature inclusion), note that the
		// p2 internal ".feature.group" id suffix must not leak into the filter
		Requirement featureRequirement = getRequirement(resource, FeatureNamespace.FEATURE_NAMESPACE);
		assertFilterMatches(featureRequirement, //
				Map.of(FeatureNamespace.FEATURE_NAMESPACE, "org.foo",
						FeatureNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version("1.5.0")),
				true);
		assertFilterMatches(featureRequirement, //
				Map.of(FeatureNamespace.FEATURE_NAMESPACE, "org.foo",
						FeatureNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version("2.0.0")),
				false);
	}

	// --- helpers ---------------------------------------------------------

	private IInstallableUnit createFeatureIU(String id, String version, String requires) throws Exception {
		File testFolder = getTempFolder();
		File featureXML = new File(testFolder, "feature.xml");
		String xml = "<feature id=\"" + id + "\" version=\"" + version + "\">\n" //
				+ "   <requires>\n" + requires + "   </requires>\n" //
				+ "</feature>\n";
		try (FileWriter writer = new FileWriter(featureXML)) {
			writer.write(xml);
		}

		PublisherInfo publisherInfo = new PublisherInfo();
		PublisherResult publisherResult = new PublisherResult();
		FeaturesAction action = new FeaturesAction(new File[] { testFolder });
		action.perform(publisherInfo, publisherResult, new NullProgressMonitor());

		String groupId = id + ".feature.group";
		IInstallableUnit iu = publisherResult.getIU(groupId, org.eclipse.equinox.p2.metadata.Version.parseVersion(version),
				IPublisherResult.ROOT);
		assertNotNull("no group IU found for " + groupId, iu);
		return iu;
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

	private void assertFilterMatches(Requirement requirement, Map<String, Object> candidateAttributes,
			boolean expectedMatch) throws Exception {
		Filter filter = FrameworkUtil.createFilter(requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
		assertEquals("filter " + filter, expectedMatch, filter.matches(candidateAttributes));
	}

}
